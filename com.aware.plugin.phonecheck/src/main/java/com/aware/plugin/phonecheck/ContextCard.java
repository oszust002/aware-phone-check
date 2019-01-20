package com.aware.plugin.phonecheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aware.Aware;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ContextCard implements IContextCard {

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    private BarChart chart = null;

    @Override
    public View getContextCard(Context context) {
        //Load card layout
        View card = LayoutInflater.from(context).inflate(R.layout.card, null);
        chart = card.findViewById(R.id.check_bar_chart);

        //Register the broadcast receiver that will update the UI from the background service (Plugin)
        IntentFilter filter = new IntentFilter(Plugin.ACTION_AWARE_PLUGIN_PHONE_CHECK);
        context.registerReceiver(phonecheckObserver, filter);

        drawGraph(context);

        //Return the card to AWARE/apps
        return card;
    }

    private void drawGraph(Context context) {
        if (chart == null) {
            return;
        }
        String summaryTimeGranularity = Aware.getSetting(context, Settings.SUMMARY_TIME_GRANULARITY, "com.aware.plugin.phonecheck");
        boolean is24HoursBars = Objects.equals(String.valueOf(context.getResources().getInteger(R.integer.phonecheck_last_24_hours)), summaryTimeGranularity);

        BarPreferences barPreferences = is24HoursBars ? new DayBarPreferences() : new WeekBarPreferences();
        barPreferences.init(Calendar.getInstance());

        //Method to get all days/hours on chart
        Map<String, BarEntry> barEntries = barPreferences.getInitialBarEntries();

        final List<String> xLabels = barPreferences.getXLabels();
        String[] columns = barPreferences.getDbColumnsQuery();

        Cursor picks = context.getContentResolver().query(Provider.PhoneCheck_Data.CONTENT_URI, columns, barPreferences.getDbSelection(), null, barPreferences.getOrder());
        if (picks != null && picks.moveToFirst()) {
            do {
                Integer stringDateIndex = xLabels.indexOf(picks.getString(1));
                barEntries.put(picks.getString(1), new BarEntry(stringDateIndex, picks.getInt(0)));
            } while (picks.moveToNext());
        }
        if (picks != null && !picks.isClosed()) picks.close();

        BarDataSet dataSet = new BarDataSet(new ArrayList<>(barEntries.values()), "Amount of times phone was checked");
        dataSet.setColor(Color.parseColor("#33B5E5"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        chart.getDescription().setEnabled(false);

        ViewGroup.LayoutParams params = chart.getLayoutParams();
        params.height = 700;
        chart.setLayoutParams(params);

        chart.setContentDescription("");
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setScaleEnabled(false);

        YAxis left = chart.getAxisLeft();
        left.setDrawLabels(true);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(true);
        left.setGranularity(1);
        left.setGranularityEnabled(true);
        left.setAxisMinimum(0);

        chart.getAxisRight().setEnabled(false);

        XAxis bottom = chart.getXAxis();
        bottom.setLabelRotationAngle(90f);
        bottom.setLabelCount(xLabels.size());
        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottom.setDrawGridLines(false);
        bottom.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xLabels.get((int) value);
            }
        });

        chart.setData(data);
        chart.invalidate();
        chart.animateX(1000);

    }

    //This broadcast receiver is auto-unregistered because it's not static.
    private PhonecheckObserver phonecheckObserver = new PhonecheckObserver();

    public class PhonecheckObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Plugin.ACTION_AWARE_PLUGIN_PHONE_CHECK)) {
                drawGraph(context);
            }
        }
    }
}
