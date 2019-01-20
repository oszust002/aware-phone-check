package com.aware.plugin.phonecheck;

import android.util.Log;

import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayBarPreferences implements BarPreferences {

    private static final String[] COLUMNS = new String[]{
            "count(*) as frequencies",
            "strftime('%Y-%m-%d %H:00', " + Provider.PhoneCheck_Data.TIMESTAMP + "/1000, 'unixepoch', 'localtime') as time_of_pick"
    };
    private static final String ORDER = "time_of_pick ASC";
    private static final String SELECTION = "strftime('%Y-%m-%d %H:00', " + Provider.PhoneCheck_Data.TIMESTAMP
            + "/1000, 'unixepoch', 'localtime') BETWEEN strftime('%Y-%m-%d %H:00', 'now', '-23 hours', 'localtime')" +
            " AND strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')) group by (time_of_pick";

    private boolean initialised = false;
    private HashMap<String, BarEntry> barEntries;
    private List<String> xLabels;


    @Override
    public void init(Calendar calendar) {
        barEntries = new HashMap<>();

        xLabels = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:00");
        calendar.add(Calendar.HOUR, -23);

        for (int i = 0; i < 24; i++) {
            String format = simpleDateFormat.format(calendar.getTime());
            Log.d("VIEW", format);
            xLabels.add(format);
            Integer stringDateIndex = xLabels.indexOf(format);
            barEntries.put(format, new BarEntry(stringDateIndex, 0));
            calendar.add(Calendar.HOUR, 1);
        }

        initialised = true;
    }

    @Override
    public Map<String, BarEntry> getInitialBarEntries() {
        if (!initialised) {
            throw new RuntimeException("Cannot get bar entries without initialization");
        }
        return new HashMap<>(barEntries);
    }

    @Override
    public List<String> getXLabels() {
        if (!initialised) {
            throw new RuntimeException("Cannot get bar entries without initialization");
        }

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
