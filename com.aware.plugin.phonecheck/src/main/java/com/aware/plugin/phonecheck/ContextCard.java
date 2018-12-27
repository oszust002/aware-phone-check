package com.aware.plugin.phonecheck;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

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
        IntentFilter filter = new IntentFilter("ACCELEROMETER_DATA");
        context.registerReceiver(accelerometerObserver, filter);

        drawGraph(context);

        //Return the card to AWARE/apps
        return card;
    }

    private void drawGraph(Context context) {
        if (chart == null) {
            return;
        }

        List<BarEntry> barEntries = new ArrayList<>();
        //TODO KO: Add entries


        BarDataSet dataSet = new BarDataSet(barEntries, "Amount of times phone was checked");
        dataSet.setColor(Color.parseColor("#33B5E5"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        chart.getDescription().setEnabled(false);

        ViewGroup.LayoutParams params = chart.getLayoutParams();
        params.height = 300;
        chart.setLayoutParams(params);

        chart.setContentDescription("");
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);

        YAxis left = chart.getAxisLeft();
        left.setDrawLabels(true);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(true);
        left.setGranularity(1);
        left.setGranularityEnabled(true);
        left.setAxisMinimum(0);

        chart.getAxisRight().setEnabled(false);

        XAxis bottom = chart.getXAxis();
        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottom.setDrawGridLines(false);
        bottom.setGranularity(1);
        bottom.setGranularityEnabled(true);

        chart.setData(data);
        chart.invalidate();
        chart.animateX(1000);

    }

    //This broadcast receiver is auto-unregistered because it's not static.
    private AccelerometerObserver accelerometerObserver = new AccelerometerObserver();
    public class AccelerometerObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("ACCELEROMETER_DATA")) {
                ContentValues data = intent.getParcelableExtra("data");
            }
        }
    }
}
