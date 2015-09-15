package com.example.avi.myfirstmap;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;


import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
    private ImageView mImgMyLocationTracking;
    private DriveId mDriveFolderId = null;
    private static final boolean YES_ZOOM = true;
    private static final boolean NO_ZOOM = false;
    private Location mLastLocation = null;
    private DriveFile mDriveFile = null;

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

        mImgMyLocationTracking = (ImageView) findViewById(R.id.imgMyLocationTracking);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mImgMyLocationTracking.getLayoutParams();
        int pad = getStatusBarHeight();
        lp.setMargins(lp.leftMargin, pad * 2, pad, lp.bottomMargin);
        mImgMyLocationTracking.setLayoutParams(lp);

        mImgMyLocationTracking.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onMyLocationButtonClick();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        //mGoogleApiClient.disconnect();
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
        Location new_loc =  LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (new_loc == null) {
            return mLastLocation;
        } else {
            return new_loc;
        }
    }

    private void panToLocation(Location location, boolean zoom_in) {
        // Location lat-lng
        LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());

        float zoomLevel = mMap.getCameraPosition().zoom;

        if (zoom_in) {
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
            zoomLevel = (float) Math.max(0, ((Math.log(equator / (256 * requiredMpp))) / Math.log(2)) - 2);
        }

        // Center to user's position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, zoomLevel));
        Log.i("Panned To", loc.toString() + " _ " + zoomLevel);
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mTrackPosition) {
           panToLocation(location, NO_ZOOM);
        }

        Log.i("Location Listener", location.toString());
     }

    /**
     * Callback called when connected to GCore. Implementation of {@link GoogleApiClient.ConnectionCallbacks}.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, REQUEST, this);  // LocationListener
        mImgMyLocationTracking.setVisibility(View.VISIBLE);

        try {
            SharedPreferences settings = getPreferences(MODE_PRIVATE);
            mDriveFolderId = DriveId.decodeFromString(settings.getString("DriveFolderId", ""));
            DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, mDriveFolderId);
            folder.getMetadata(mGoogleApiClient).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                   @Override
                   public void onResult(DriveResource.MetadataResult result) {
                       if (!result.getStatus().isSuccess()) {
                           Log.i("METADATA", "Problem while trying to fetch metadata.");
                           getNewFolder();
                           return;
                       }

                       Metadata metadata = result.getMetadata();
                       if(metadata.isTrashed()){
                           Log.i("METADATA", "Folder is trashed");
                           getNewFolder();
                       }else{
                           Log.i("METADATA", "Folder is not trashed");
                       }
                   }
               });
        } catch (IllegalArgumentException e) {
            Log.i("CAUGHT", "ID IS NULL");
            // mDriveFolderId is null or otherwise invalid
            getNewFolder();
        }
    }

    private void getNewFolder() {
        mDriveFolderId = null;
        IntentSender intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
                .setActivityTitle("Store data where?")
                .setMimeType(new String[]{DriveFolder.MIME_TYPE})
                .build(mGoogleApiClient);

        try {
            startIntentSenderForResult(intentSender, REQUEST_CODE.FOLDER_OPENER.ordinal(), null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w("INTENT ERROR", "Unable to send intent FOLDER_OPENER", e);
        }
    }


    /**
     * Callback called when disconnected from GCore. Implementation of {@link GoogleApiClient.ConnectionCallbacks}.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        // Do nothing
    }

    private enum REQUEST_CODE {
        RESOLVE_CONNECTION,
        FOLDER_OPENER
    }

    private static final REQUEST_CODE[] RC = REQUEST_CODE.values();

    /**
     * Implementation of {@link GoogleApiClient.OnConnectionFailedListener}.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                Log.i("CONNECTION RETRY?", "RETRY");
                connectionResult.startResolutionForResult(this, REQUEST_CODE.RESOLVE_CONNECTION.ordinal());
            } catch (IntentSender.SendIntentException e) {
                Log.e("CONNECTION EXCEPTION", "Exception while starting resolution activity", e);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (RC[requestCode]) {
            case RESOLVE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
            case FOLDER_OPENER:
                if (resultCode == RESULT_OK) {
                    mDriveFolderId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    SharedPreferences settings = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor settings_editor = settings.edit();
                    settings_editor.putString("DriveFolderId", mDriveFolderId.encodeToString());
                    settings_editor.commit();

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(getString(R.string.track_file_name))
                            .setMimeType(getString(R.string.track_file_mime)).build();

                    DriveFolder mDriveFolder = Drive.DriveApi.getFolder(mGoogleApiClient, mDriveFolderId);
                    mDriveFolder.createFile(mGoogleApiClient, changeSet, null).setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                             @Override
                             public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                                 if (!driveFileResult.getStatus().isSuccess()) {
                                     Log.i("DRIVE FILE", "Problem trying to get file");
                                     return;
                                 } else {
                                     mDriveFile = driveFileResult.getDriveFile();
                                     mDriveFile.open(mGoogleApiClient, DriveFile.MODE_READ_WRITE, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                                         @Override
                                         public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                                             if (!driveContentsResult.getStatus().isSuccess()) {
                                                 Log.i("FILE CONTENTS", "Error while trying to create new file contents");
                                                 return;
                                             }
                                             DriveContents driveContents = driveContentsResult.getDriveContents();

                                             try {
                                                 ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
                                                 FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                                                 // Read to the end of the file.
                                                 fileInputStream.read(new byte[fileInputStream.available()]);

                                                 // Append to the file.
                                                 FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                                                 Writer writer = new OutputStreamWriter(fileOutputStream);
                                                 writer.write("hello world");
                                             } catch (IOException e) {
                                                 e.printStackTrace();
                                             }

                                             driveContents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<Status>() {
                                                 @Override
                                                 public void onResult(Status result) {
                                                     // Handle the response status
                                                 }
                                             });

                                         }
                                     });
                                 }
                             }
                         });

                    Log.i("OPENER", "Selected folder's ID: " + mDriveFolderId);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private boolean onMyLocationButtonClick() {

        Location my_loc = getMyLocation();

        if (my_loc != null) {
            mTrackPosition = !mTrackPosition;

            //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
            Log.i("MyLocation button", "clicked. Follow?" + mTrackPosition);

            if (mTrackPosition) {
                mImgMyLocationTracking.setImageResource(R.mipmap.tracking_on);
                mImgMyLocationTracking.setContentDescription(getString(R.string.disable_panning));
                panToLocation(my_loc, YES_ZOOM);
            } else {
                mImgMyLocationTracking.setImageResource(R.mipmap.tracking_off);
                mImgMyLocationTracking.setContentDescription(getString(R.string.enable_panning));
            }
        }

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return true;
    }

}