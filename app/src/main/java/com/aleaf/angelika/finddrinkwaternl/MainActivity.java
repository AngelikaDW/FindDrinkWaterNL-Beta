package com.aleaf.angelika.finddrinkwaternl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final int PERMISSION_REQUEST_CODE_LOCATION = 1; //NEW
    private static final int MY_PERMISSIONS_REQUEST = 1;
    private final String LOG_TAG = AppCompatActivity.class.getSimpleName();
    GoogleMap m_map;
    boolean mapReady = false;
    ArrayList<String[]> coordinates = new ArrayList<>();
    Marker marker;
    private TextView txtOutput;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //shows the map
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        configureLocationUpdates();

        try {
            LoadLocations();

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),
                    "Problems: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        Button info_button = (Button) findViewById(R.id.button_info);
//        info_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent infoIntent = new Intent(MainActivity.this, InfoActivity.class);
//                startActivity(infoIntent);
//            }
//        });
        info_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent infoIntent = new Intent(MainActivity.this, Main2Activity.class);
                startActivity(infoIntent);
            }
        });


    }

    private void requestLocationUpdate() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //This check necessary for API 23 or higher (Android 6.0)
        int permissionCheck = ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST);
            }

        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    requestLocationUpdate();
                } else {
                    txtOutput.setText("No permission to get the location");
                }
                return;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("CURRENT LOCATION", location.toString());
        LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude());

        if (marker != null) {
            marker.remove();
        }

        marker = m_map.addMarker(new MarkerOptions()
                .position(currentLoc)
                .title("Here are you!")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        //Reposition the map to display current location
        CameraPosition CURRENT = CameraPosition.builder().target(currentLoc).zoom(14).build();
        flyTo(CURRENT);

        //Adds Markers to the map from where_water1.txt
        addMarkersToMap();

    }

    private void flyTo(CameraPosition target) {
        m_map.animateCamera(CameraUpdateFactory.newCameraPosition(target), 2000, null);
    }

    public void LoadLocations() throws IOException {
        String str="";
        InputStream is = this.getResources().openRawResource(R.raw.where_water1);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        if (is!=null) {
            while ((str = reader.readLine()) != null) {
                String[] coord = str.split(",");
                coordinates.add(coord);
            }
        }
        is.close();
    }


    @Override
    public void onMapReady(GoogleMap map) {
        mapReady = true;
        m_map = map;
        LatLng amsterdamNL = new LatLng(52.3702, 4.8952);
        CameraPosition target = CameraPosition.builder().target(amsterdamNL).zoom(14).build();
        m_map.moveCamera(CameraUpdateFactory.newCameraPosition(target));

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Connect the Client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        //Disconnect the Client
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void configureLocationUpdates() {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(3 * 60000); //Update location every 3 minutes
    }

    public void addMarkersToMap() {
        for (int j = 1; j<coordinates.size(); j ++){
            String fountainaddress = coordinates.get(j)[2] +", "+coordinates.get(j)[3];
            double fountainlat = Double.parseDouble(coordinates.get(j)[0].trim());
            double fountainlng = Double.parseDouble(coordinates.get(j)[1].trim());
            LatLng fountainCoordinates = new LatLng(fountainlat, fountainlng);

            m_map.addMarker(new MarkerOptions()
                    .position(fountainCoordinates)
                    .title(fountainaddress)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }

}
