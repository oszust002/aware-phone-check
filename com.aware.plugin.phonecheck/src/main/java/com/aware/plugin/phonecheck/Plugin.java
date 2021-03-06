package com.aware.plugin.phonecheck;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
import com.aware.providers.Accelerometer_Provider;
import com.aware.utils.Aware_Plugin;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_PHONE_CHECK = "ACTION_AWARE_PLUGIN_PHONE_CHECK";

    //TODO: Threshold values
    private static final long SCREEN_ON_THRESHHOLD = 1500;
    private static final long ACCELEROMETER_SCREEN_DIFF_THRESHOLD = 2000;
    public static final int MINIMAL_ACCELERATION_VALUE = 4;

    private long lastScreenOn = 0;
    private long lastAccelerationRead;

    private AccelerometerFilter accelerometerFilter = new AccelerometerFilter(0.5f);
    private boolean wasPicked;

    @Override
    public void onCreate() {
        super.onCreate();

        //This allows plugin data to be synced on demand from broadcast Aware#ACTION_AWARE_SYNC_DATA
        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    private void triggerPhoneCheck() {
        Log.d(TAG, "Phone was picked up");

        ContentValues context_data = new ContentValues();
        context_data.put(Provider.PhoneCheck_Data.TIMESTAMP, System.currentTimeMillis());
        context_data.put(Provider.PhoneCheck_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));

        if (DEBUG) Log.d(TAG, context_data.toString());

        if (awareSensor != null) {
            awareSensor.onPhoneCheck(context_data);
        }

        getContentResolver().insert(Provider.PhoneCheck_Data.CONTENT_URI, context_data);

        Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_PHONE_CHECK);
        sendBroadcast(sharedContext);
    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;

    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onPhoneCheck(ContentValues data);
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            /**
             * Example of how to enable accelerometer sensing and how to access the data in real-time for your app.
             * In this particular case, we are sending a broadcast that the ContextCard listens to and updates the UI in real-time.
             */
//            Aware.setSetting(this, Aware_Preferences.FREQUENCY_ACCELEROMETER_ENFORCE, true);
            Aware.setSetting(this, Aware_Preferences.FREQUENCY_ACCELEROMETER, 60000);
            Aware.startAccelerometer(this);
            Accelerometer.setSensorObserver(new Accelerometer.AWARESensorObserver() {
                @Override
                public void onAccelerometerChanged(ContentValues contentValues) {
                    long currentTime = System.currentTimeMillis();

                    if (Math.abs(currentTime - lastAccelerationRead) < 1500) {
                        return;
                    }

                    float[] rawAcceleration = extractRawAcceleration(contentValues);
                    float[] calculate = accelerometerFilter.calculate(rawAcceleration);
                    float value = calculateAccelerationMagnitude(calculate);

                    if (value < MINIMAL_ACCELERATION_VALUE) {
                        return;
                    }

                    lastAccelerationRead = currentTime;
                    long screenAccelerometerTimeDiff = Math.abs(currentTime - lastScreenOn);
                    if (screenAccelerometerTimeDiff > ACCELEROMETER_SCREEN_DIFF_THRESHOLD || lastScreenOn == 0) {
                        return;
                    }
                    recordPhonePick();
                }
            });

            Aware.startScreen(this);
            Screen.setSensorObserver(new Screen.AWARESensorObserver() {
                @Override
                public void onScreenOn() {
                    //TODO: Filter fast screen on/off during picking
                    long currentScreenOn = System.currentTimeMillis();
                    if (currentScreenOn - lastScreenOn < SCREEN_ON_THRESHHOLD) {
                        return;
                    }
                    lastScreenOn = currentScreenOn;
                    long screenAccelerometerTimeDiff = Math.abs(currentScreenOn - lastAccelerationRead);
                    if (screenAccelerometerTimeDiff > ACCELEROMETER_SCREEN_DIFF_THRESHOLD || lastAccelerationRead == 0) {
                        return;
                    }
                    recordPhonePick();
                }

                @Override
                public void onScreenOff() {

                }

                @Override
                public void onScreenLocked() {

                }

                @Override
                public void onScreenUnlocked() {

                }
            });

            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
            }

            //Initialise AWARE instance in plugin
            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    private void recordPhonePick() {
        if (wasPicked) {
            return;
        }

        triggerPhoneCheck();
        wasPicked = true;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                wasPicked = false;
            }
        }, ACCELEROMETER_SCREEN_DIFF_THRESHOLD);
    }

    private float calculateAccelerationMagnitude(float[] calculate) {
        return (float) Math.sqrt(calculate[0] * calculate[0] + calculate[1] * calculate[1] + calculate[2] * calculate[2]);
    }

    private float[] extractRawAcceleration(ContentValues contentValues) {
        Float x = (Float) contentValues.get(Accelerometer_Provider.Accelerometer_Data.VALUES_0);
        Float y = (Float) contentValues.get(Accelerometer_Provider.Accelerometer_Data.VALUES_1);
        Float z = (Float) contentValues.get(Accelerometer_Provider.Accelerometer_Data.VALUES_2);
        return new float[]{x, y, z};
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Turn off the sync-adapter if part of a study
        if (Aware.isStudy(this) && (getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone))) {
            ContentResolver.removePeriodicSync(
                    Aware.getAWAREAccount(this),
                    Provider.getAuthority(this),
                    Bundle.EMPTY
            );
        }

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
