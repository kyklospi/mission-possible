package org.dieschnittstelle.mobile.android.skeleton.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.SerializedName;

import org.dieschnittstelle.mobile.android.skeleton.R;
import org.dieschnittstelle.mobile.android.skeleton.util.LocationConverter;
import org.dieschnittstelle.mobile.android.skeleton.util.StringListConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Task implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String description;
    private long expiry;
    @SerializedName("done")
    private boolean completed;
    @SerializedName("favourite")
    private boolean favorite;
    @RemoteTaskDatabaseOperation.Exclude
    private Priority priority = Priority.NONE;
    @TypeConverters(StringListConverter.class)
    private List<String> contacts = new ArrayList<>();
    @TypeConverters(LocationConverter.class)
    private Location location;

    public Task() {
    }

    public Task(String name, String description, long expiry, boolean completed, boolean favorite, Priority priority, List<String> contacts) {
        this.name = name;
        this.description = description;
        this.expiry = expiry;
        this.completed = completed;
        this.favorite = favorite;
        this.priority = priority;
        this.contacts = contacts;
        this.location = new Location();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public enum Priority {
        CRITICAL(R.color.colorTaskPrioCritical), HIGH(R.color.colorTaskPrioHigh), NORMAL(R.color.colorTaskPrioNormal), LOW(R.color.colorTaskPrioLow), NONE(R.color.colorTaskPrioNone);

        public final int resourceId;

        Priority(int resourceId) {
            this.resourceId = resourceId;
        }
    }

    public static class Location implements Serializable {
        private String name;
        private LatLng latlng;

        public Location() {
        }

        public Location(String name, LatLng latlng) {
            this.name = name;
            this.latlng = latlng;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LatLng getLatlng() {
            return latlng;
        }

        public void setLatlng(LatLng latlng) {
            this.latlng = latlng;
        }
    }

    public static class LatLng implements Serializable {

        private double lat;
        private double lng;

        public LatLng() {
        }

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }
}
