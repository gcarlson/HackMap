package com.example.gcarl.hackmap;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
//import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;

import java.text.DateFormat;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    final String[] signals = {"", "|", "||", "|||", "||||", "|||||"};

    TextView clock, headingText, latText, lonText, pressureText, trackingText, signalText;
    Button markButton;
    Button trackButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button upButton, downButton, leftButton, rightButton;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    LatLng latLng;
    LatLng last = null;
    GoogleMap mGoogleMap;
    Marker currLocationMarker;
    boolean breadcrumbs = true;

    SensorManager manager;
    Sensor sensor;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and gt notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        clock = (TextView) findViewById(R.id.clock);
        headingText = (TextView) findViewById(R.id.heading);
        latText = (TextView) findViewById(R.id.lat);
        lonText = (TextView) findViewById(R.id.lon);
        pressureText = (TextView) findViewById(R.id.pressure);
        trackingText = (TextView) findViewById(R.id.tracking);
        signalText = (TextView) findViewById(R.id.signal);

        markButton = (Button) findViewById(R.id.markbutton);
        markButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //place marker at current position
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Danger");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                mGoogleMap.addMarker(markerOptions);
            }
        });

        trackButton = (Button) findViewById(R.id.trackbutton);
        trackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // toggle breadcrumb tracking
                breadcrumbs = !breadcrumbs;
                last = null;
                trackingText.setText(breadcrumbs ? "Tracking" : "");
            }
        });

        zoomInButton = (Button) findViewById(R.id.zoominbutton);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // zoom map in
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomBy(1));
            }
        });

        zoomOutButton = (Button) findViewById(R.id.zoomoutbutton);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // zoom map out
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomBy(-1));
            }
        });

        upButton = (Button) findViewById(R.id.upbutton);
        upButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.scrollBy(0, -100));
            }
        });

        downButton = (Button) findViewById(R.id.downbutton);
        downButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.scrollBy(0, 100));
            }
        });

        rightButton = (Button) findViewById(R.id.rightbutton);
        rightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.scrollBy(100, 0));
            }
        });

        leftButton = (Button) findViewById(R.id.leftbutton);
        leftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mGoogleMap.animateCamera(CameraUpdateFactory.scrollBy(-100, 0));
            }
        });

        // TODO: use dive computer in place of native sensors
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensor = manager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) throws SecurityException
    {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);

        // Remove google Mp tiling, leaving only custom map.
        // Note: if you wish to navigate with Google Maps data, remove below line:
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    public synchronized void buildGoogleApiClient()
    {
        Toast.makeText(this, "buildGoogleApiClient", Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void onConnected(Bundle bundle) throws SecurityException
    {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            //place marker at current position
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            currLocationMarker = mGoogleMap.addMarker(markerOptions);
        }

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 18));

        GroundOverlay over = mGoogleMap.addGroundOverlay(new GroundOverlayOptions()
        .image(BitmapDescriptorFactory.fromResource(R.drawable.divemap))
        .anchor(0.5f, 0.5f)
        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 2000f, 2000f));

        over.setTransparency(0.1f);

        // TODO: modify code to respond to positioning code instead of native GPS.
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); //5 seconds
        mLocationRequest.setFastestInterval(3000); //3 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // May fail if permission denied
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }

    // Run whenever a new location is received
    @Override
    public void onLocationChanged(Location location)
    {
        // Place marker at current position
        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }

        // Mark current location on map
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        currLocationMarker = mGoogleMap.addMarker(markerOptions);

        // If tracking is on, mark it
        if (breadcrumbs)
        {
            markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Breadcrumb");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mGoogleMap.addMarker(markerOptions);

            // add lines between marked points
            if (last != null)
            {
                PolylineOptions polyOptions = new PolylineOptions();
                polyOptions.add(last);
                polyOptions.add(latLng);
                polyOptions.color(Color.RED);
                mGoogleMap.addPolyline(polyOptions);
            }
            last = latLng;

            // TODO: add tracked points to a database
        }

        // update coordinates
        latText.setText(location.getLatitude() + " N");
        lonText.setText(0 - location.getLongitude() + " W");

        // TODO: get actual signal strength
        signalText.setText(signals[((int) (Math.random() * 1000)) % 6]);
    }

    public void onSensorChanged(SensorEvent event)
    {
        // TODO: get dive computer data
        pressureText.setText("Pressure: " + Math.round(event.values[0]) / 1000.0 + " bar");
        clock.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
