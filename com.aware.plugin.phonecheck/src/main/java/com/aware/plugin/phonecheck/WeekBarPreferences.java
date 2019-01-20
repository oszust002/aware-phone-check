package com.aware.plugin.phonecheck;

import android.util.Log;

import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeekBarPreferences implements BarPreferences {

    private static final String[] COLUMNS = new String[]{
            "count(*) as frequencies",
            "strftime('%Y-%m-%d', " + Provider.PhoneCheck_Data.TIMESTAMP + "/1000, 'unixepoch', 'localtime') as time_of_pick"
    };
    private static final String ORDER = "time_of_pick ASC";
    private static final String SELECTION = "strftime('%Y-%m-%d', " + Provider.PhoneCheck_Data.TIMESTAMP
            + "/1000, 'unixepoch', 'localtime') BETWEEN strftime('%Y-%m-%d', 'now', '-6 days', 'localtime')" +
            " AND strftime('%Y-%m-%d', 'now', 'localtime')) group by (time_of_pick";

    private boolean initialised = false;
    private HashMap<String, BarEntry> barEntries;
    private List<String> xLabels;
    @Override
    public void init(Calendar calendar) {
        barEntries = new HashMap<>();

        xLabels = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        calendar.add(Calendar.DATE, -6);

        for (int i = 0; i < 7; i++) {
            String format = simpleDateFormat.format(calendar.getTime());
            Log.d("VIEW", format);
            xLabels.add(format);
            Integer stringDateIndex = xLabels.indexOf(format);
            barEntries.put(format, new BarEntry(stringDateIndex, 0));
            calendar.add(Calendar.DATE, 1);
        }

        initialised = true;
    }

    @Override
    public Map<String, BarEntry> getInitialBarEntries() {
        return new HashMap<>(barEntries);
    }

    @Override
    public List<String> getXLabels() {
        return new ArrayList<>(xLabels);
    }

    @Override
    public String[] getDbColumnsQuery() {
        return COLUMNS;
    }

    @Override
    public String getDbSelection() {
        return SELECTION;
    }

    @Override
    public String getOrder() {
        return ORDER;
    }
}
