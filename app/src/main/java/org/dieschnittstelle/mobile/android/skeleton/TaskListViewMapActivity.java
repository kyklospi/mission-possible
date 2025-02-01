package org.dieschnittstelle.mobile.android.skeleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.viewmodel.TaskListViewModel;

import java.util.List;

public class TaskListViewMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private static List<Task> tasks;
    private GoogleMap map;
    private TaskListViewModel viewModel;

    protected static final String TASK_LIST_VIEW_MAP_KEY = "taskListMapViewObject";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list_view_map);
        viewModel = new ViewModelProvider(this).get(TaskListViewModel.class);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.task_list_map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Using setOnMarkerClickListener may override behaviors set by the Maps SDK for Android Utility Library.
    // Safely suppress warning because clustering, GeoJson, or KML are not used
    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (tasks == null) {
            tasks = viewModel.getTaskList();
        }
        map = googleMap;
        map.setOnMarkerClickListener(this);
        showTasksOnMap();
    }

    private void showTasksOnMap() {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int markerCounter = 0;
        for (Task t : tasks) {
            if(t.getLocation() != null && t.getLocation().getLatlng() != null) {
                markerCounter++;
                Task.LatLng location = t.getLocation().getLatlng();
                LatLng coordinate = new LatLng(location.getLat(), location.getLng());
                boundsBuilder.include(coordinate);
                addMarker(coordinate, t.getName());
            }
        }
        // Move camera to show all markers and locations
        if (markerCounter > 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        }
    }

    private void addMarker(LatLng coordinates, String title) {
        map.addMarker(new MarkerOptions()
                .position(coordinates)
                .title(title));
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        LatLng markerPosition = marker.getPosition();
        for (Task t : tasks) {
            if (t.getLocation() != null && t.getLocation().getLatlng() != null) {
                Task.LatLng taskLocation = t.getLocation().getLatlng();
                LatLng taskLocationLatLng = new LatLng(taskLocation.getLat(), taskLocation.getLng());
                if (markerPosition.equals(taskLocationLatLng)) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(TASK_LIST_VIEW_MAP_KEY, t);
                    this.setResult(TaskListViewMapActivity.RESULT_OK, returnIntent);
                    this.finish();
                }
            }
        }
        return true;
    }
}
