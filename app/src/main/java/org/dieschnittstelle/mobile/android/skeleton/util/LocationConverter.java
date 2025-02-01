package org.dieschnittstelle.mobile.android.skeleton.util;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

import org.dieschnittstelle.mobile.android.skeleton.model.Task;


public class LocationConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static Task.Location toLocation(String locationString) {
        return gson.fromJson(locationString, Task.Location.class);
    }

    @TypeConverter
    public static String fromLocation(Task.Location location) {
        return gson.toJson(location);
    }
}
