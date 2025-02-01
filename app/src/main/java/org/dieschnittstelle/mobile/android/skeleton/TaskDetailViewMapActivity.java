package org.dieschnittstelle.mobile.android.skeleton;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.viewmodel.TaskDetailViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TaskDetailViewMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    protected static final String LOCATION_VIEW_KEY = "locationViewObject";
    private static final int ACCESS_FINE_LOCATION_CODE = 100;
    private TaskDetailViewModel viewModel;

    private Task.Location taskLocation;

    private Marker pinMarker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail_view_map);
        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);
        taskLocation = (Task.Location) getIntent().getSerializableExtra(LOCATION_VIEW_KEY);
        if (taskLocation == null || taskLocation.getLatlng() == null) {
            taskLocation = new Task.Location();
        }

        getUserDefaultLocation();
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.task_detail_map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (taskLocation == null || taskLocation.getLatlng() == null) {
            LatLng defaultLatLng = new LatLng(viewModel.getDefaultLatLng().getLat(), viewModel.getDefaultLatLng().getLng());
            pinMarker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(defaultLatLng)
                            .title("Your location")
            );
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(defaultLatLng));
        } else {
            LatLng selectedLatLng = new LatLng(
                    taskLocation.getLatlng().getLat(),
                    taskLocation.getLatlng().getLng()
            );
            pinMarker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(selectedLatLng)
                            .title(taskLocation.getName())
            );
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(selectedLatLng));
        }

        googleMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = new ArrayList<>();

        try {
            // Get the address from the latitude and longitude
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            Log.e("TaskLocationViewActivity", Objects.requireNonNull(e.getMessage()));
        }

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            String city = address.getLocality() != null ? address.getLocality() : address.getAdminArea();
            pinMarker.setPosition(latLng);
            pinMarker.setTitle(city);
            Task.LatLng pinnedLatLng = new Task.LatLng(latLng.latitude, latLng.longitude);
            Task.Location pinnedLocation = new Task.Location(city, pinnedLatLng);
            selectLocationAlertDialog(pinnedLocation);
        }
    }

    private void getUserDefaultLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
        } else {
            String provider = locationManager.getAllProviders().get(0);
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                viewModel.setupDefaultLatLng(location.getLatitude(), location.getLongitude());
            }
        }
    }

    private void selectLocationAlertDialog(Task.Location pinnedLocation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(android.R.layout.select_dialog_item);
        builder.setMessage("Save \"" + pinnedLocation.getName() + "\" as Mission Location?");
        builder.setPositiveButton("Save", (dialog, id) -> saveLocationIntent(pinnedLocation));
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveLocationIntent(Task.Location pinnedLocation) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(LOCATION_VIEW_KEY, pinnedLocation);
        this.setResult(TaskDetailViewMapActivity.RESULT_OK, returnIntent);
        this.finish();
    }
}
