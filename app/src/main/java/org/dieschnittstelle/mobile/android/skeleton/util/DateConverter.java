package org.dieschnittstelle.mobile.android.skeleton.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateConverter {
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

    public static long fromDateString(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return 0;
        }
        try {
            Date date = DATE_FORMATTER.parse(dateTime);
            return date == null ? 0 : date.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String toDateString(long dateTime) {
        if (dateTime == 0) {
            return "";
        }
        return DATE_FORMATTER.format(dateTime);
    }

    public static long fromDate(Date dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return dateTime.getTime();
    }
}
