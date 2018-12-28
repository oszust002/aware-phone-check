package com.aware.plugin.phonecheck;

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

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_PHONE_CHECK = "ACTION_AWARE_PLUGIN_PHONE_CHECK";

    //TODO: Threshold values
    private static final long SCREEN_ON_THRESHHOLD = 0;
    private static final long ACCELEROMETER_SCREEN_DIFF_THRESHOLD = 0;
    public static final int MINIMAL_ACCELERATION_VALUE = 4;

    private long lastScreenOn = 0;

    private AccelerometerFilter accelerometerFilter = new AccelerometerFilter(0.5f);

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
                ContentValues context_data = new ContentValues();
                context_data.put(Provider.PhoneCheck_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.PhoneCheck_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));

                if (DEBUG) Log.d(TAG, context_data.toString());

                getContentResolver().insert(Provider.PhoneCheck_Data.CONTENT_URI, context_data);

                Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_PHONE_CHECK);
                sendBroadcast(sharedContext);
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
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

                    float[] rawAcceleration = extractRawAcceleration(contentValues);
                    //TODO KO: Acceleration, orientation, low/high pass filter (continuous kalman?) Use standard accelerometer instead of Aware?
                    float[] calculate = accelerometerFilter.calculate(rawAcceleration);
                    float value = calculateAccelerationMagnitude(calculate);
                    if (value < MINIMAL_ACCELERATION_VALUE) {
                        return;
                    }

                    long screenAccelerometerTimeDiff = Math.abs(System.currentTimeMillis() - lastScreenOn);

                    if (screenAccelerometerTimeDiff > ACCELEROMETER_SCREEN_DIFF_THRESHOLD) {
                        return;
                    }
                    CONTEXT_PRODUCER.onContext();
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
