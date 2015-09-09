package com.example.avi.myfirstmap;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    private GoogleApiClient mGoogleApiClient;
    private TextView mMessageView;
    private GoogleMap mMap;
    private boolean mTrackPosition = false;
    private ImageView imgMyLocationTrackingOff;
    //private FrameLayout imgMyLocationTrackingOffContainer;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        imgMyLocationTrackingOff = (ImageView) findViewById(R.id.imgMyLocationTrackingOff);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) imgMyLocationTrackingOff.getLayoutParams();
        int pad = getStatusBarHeight();
        lp.setMargins(lp.leftMargin, pad*2, pad, lp.bottomMargin);
        imgMyLocationTrackingOff.setLayoutParams(lp);

        imgMyLocationTrackingOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onMyLocationButtonClick();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setPadding(0, getStatusBarHeight(), 0, 0);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.getUiSettings().setMyLocationButtonEnabled(false); // disable default location button so I can make my own
        //mMap.setOnMyLocationButtonClickListener(this);
    }

    // A method to find height of the status bar
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private Location getMyLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //TODO: LOG! Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void panToLocation(Location location) {
        // Location lat-lng
        LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());

        // Location accuracy diameter (in meters)
        float accuracy = location.getAccuracy() * 2;

        // Screen measurements
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Use min(width, height) (to properly fit the screen
        int screenSize = Math.min(metrics.widthPixels, metrics.heightPixels);

        // Equators length
        long equator = 40075004;

        // The meters per pixel required to show the whole area the user might be located in
        double requiredMpp = accuracy / screenSize;

        // Calculate the zoom level
        double zoomLevel = ((Math.log(equator / (256 * requiredMpp))) / Math.log(2)) -1;

        // Center to user's position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, (float) zoomLevel));
        Log.i("Panned To", loc.toString() + " _ " + zoomLevel);

    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (mTrackPosition == true) {
           panToLocation(location);
        }

        Log.i("Location Listener", location.toString());
        // mMessageView.setText("Location = " + location);
    }

    /**
     * Callback called when connected to GCore. Implementation of {@link ConnectionCallbacks}.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                REQUEST,
                this);  // LocationListener
    }

    /**
     * Callback called when disconnected from GCore. Implementation of {@link ConnectionCallbacks}.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        // Do nothing
    }

    /**
     * Implementation of {@link OnConnectionFailedListener}.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Do nothing
    }

    public boolean onMyLocationButtonClick() {

        mTrackPosition = !mTrackPosition;

        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        Log.i("MyLocation button", "clicked. Follow?"+mTrackPosition);

        if (mTrackPosition) {
            imgMyLocationTrackingOff.setImageResource(R.mipmap.tracking_on);
            panToLocation(getMyLocation());
        } else {
            imgMyLocationTrackingOff.setImageResource(R.mipmap.tracking_off);
        }

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return true;
    }

}