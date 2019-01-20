package com.aware.plugin.phonecheck;

import com.github.mikephil.charting.data.BarEntry;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public interface BarPreferences {

    void init(Calendar calendar);

    Map<String, BarEntry> getInitialBarEntries();

    List<String> getXLabels();

    String[] getDbColumnsQuery();

    String getDbSelection();

    String getOrder();
}
