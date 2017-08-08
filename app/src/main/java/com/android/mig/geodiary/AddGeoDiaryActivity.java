package com.android.mig.geodiary;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.mig.geodiary.models.GeoDiary;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.List;

public class AddGeoDiaryActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST_USE_CAMERA = 10;        // code should be bigger than 0
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 20;
    private static final String STATE_LATITUDE = "latitude";
    private static final String STATE_LONGITUDE = "longitude";
    private static final String STATE_PHOTO_PATH = "photoPath";
    private static final String STATE_TEMP_PHOTO_PATH = "tempPhotoPath";
    private static final String STATE_STORAGE_REFERENCE = "storageReference";

    CoordinatorLayout mRootLayout;
    EditText mTitleEditText, mBodyEditText;
    ImageView mThumbnailImageView;
    FloatingActionButton mFabPlus, mFabInsertQuote, mFabTakePhoto;
    Animation mScaleUpAnimation, mScaleDownAnimation,
            mFabClockwiseAnimation, mFabCounterClockwiseAnimation;
    Uri mPhotoPath, mTempContainerPath;
    boolean isOpen = false;
    double mLatitude, mLongitude;

    private GoogleApiClient mGoogleApiClient;
    FirebaseStorage mFirebaseStorage;
    StorageReference mStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geodiary);

        mRootLayout = (CoordinatorLayout) findViewById(R.id.add_geodiary_coordinator_layout);
        mTitleEditText = (EditText) findViewById(R.id.title_edit_text);
        mBodyEditText = (EditText) findViewById(R.id.body_edit_text);
        mThumbnailImageView = (ImageView) findViewById(R.id.thumbnail_image_view);
        mFabPlus = (FloatingActionButton) findViewById(R.id.fab_plus);
        mFabInsertQuote = (FloatingActionButton) findViewById(R.id.fab_insert_quote);
        mFabTakePhoto = (FloatingActionButton) findViewById(R.id.fab_take_photo);

        mScaleUpAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_up);
        mScaleDownAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_down);
        mFabClockwiseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_clockwise);
        mFabCounterClockwiseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_counterclockwise);

        mTitleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                // if edit text has focus and small fab buttons are open
                if (hasFocus && isOpen){
                    disableButtons();
                }
            }
        });

        mBodyEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                // if edit text has focus and small fab buttons are open
                if (hasFocus && isOpen){
                    disableButtons();
                }
            }
        });

        mFabPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // fab buttons are visible
                if (isOpen) {
                    disableButtons();
                    // fab buttons are not visible
                } else {
                    enableButtons();
                }
            }
        });

        // creates an instance of Google Service API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLatitude = savedInstanceState.getDouble(STATE_LATITUDE);
        mLongitude = savedInstanceState.getDouble(STATE_LONGITUDE);
        mTempContainerPath = Uri.parse(savedInstanceState.getString(STATE_TEMP_PHOTO_PATH));
        String statePhotoPath = savedInstanceState.getString(STATE_PHOTO_PATH);
        if (statePhotoPath != null){
            mPhotoPath = Uri.parse(statePhotoPath);
            Glide.with(mThumbnailImageView.getContext()).load(mPhotoPath).into(mThumbnailImageView);
        }

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString(STATE_STORAGE_REFERENCE);
        if (stringRef == null) {
            return;
        }
        restoreStorageTask(stringRef);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putDouble(STATE_LATITUDE, mLatitude);
        outState.putDouble(STATE_LONGITUDE, mLongitude);
        outState.putString(STATE_TEMP_PHOTO_PATH, String.valueOf(mTempContainerPath));
        if (mPhotoPath != null){
            outState.putString(STATE_PHOTO_PATH, String.valueOf(mPhotoPath));
        }
        // If there's an upload in progress, save the reference so you can query it later
        if (mStorageReference != null) {
            outState.putString(STATE_STORAGE_REFERENCE, mStorageReference.toString());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receives result code from camera intent launched previously
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mPhotoPath = mTempContainerPath;
            Glide.with(mThumbnailImageView.getContext()).load(mPhotoPath).into(mThumbnailImageView);
            mTempContainerPath = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_geodiary_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            String title = mTitleEditText.getText().toString();
            String content = mBodyEditText.getText().toString();
            if (title.matches("") || content.matches("")){
                showSnackbar(R.string.need_title_content_message);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                Toast.makeText(this, R.string.sending_message, Toast.LENGTH_SHORT).show();
                if (isOpen){
                    // if small FABs are being displayed then do scaling-down animation
                    disableButtons();
                }
                preparePhotoAndSave();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_USE_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // if permission is granted at run time, then launch camera
                    dispatchCameraIntent();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // if permission is granted at run time, then re-connect to the service
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // checks if user granted permission for using location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            // if permission is denied then request permission
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                mGoogleApiClient.disconnect();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
            return;
        }

        // once permission is granted, request location
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mLongitude = location.getLongitude();
        mLatitude = location.getLatitude();
    }

    /**
     * Shows fab buttons with animation and set their OnClick methods
     */
    private void enableButtons() {
        mFabTakePhoto.startAnimation(mScaleUpAnimation);
        mFabTakePhoto.setVisibility(View.VISIBLE);
        mFabTakePhoto.setClickable(true);
        mFabTakePhoto.setFocusable(true);

        mFabInsertQuote.startAnimation(mScaleUpAnimation);
        mFabInsertQuote.setVisibility(View.VISIBLE);
        mFabInsertQuote.setClickable(true);
        mFabInsertQuote.setFocusable(true);

        mFabPlus.startAnimation(mFabClockwiseAnimation);
        isOpen = true;
        mFabTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryUsingCamera();
            }
        });

        mFabInsertQuote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddGeoDiaryActivity.this, QuotesActivity.class);
                startActivity(intent);
            }
        });

        mFabPlus.setNextFocusForwardId(R.id.fab_insert_quote);
        mFabPlus.setNextFocusRightId(R.id.action_save);
        mFabPlus.setNextFocusUpId(R.id.fab_insert_quote);
        mFabInsertQuote.setNextFocusDownId(R.id.fab_plus);
        mFabInsertQuote.setNextFocusUpId(R.id.fab_take_photo);
        mFabTakePhoto.setNextFocusDownId(R.id.fab_insert_quote);
    }

    /**
     * Hides fab buttons with animation
     */
    private void disableButtons() {
        mFabInsertQuote.startAnimation(mScaleDownAnimation);
        mFabTakePhoto.startAnimation(mScaleDownAnimation);
        mFabPlus.startAnimation(mFabCounterClockwiseAnimation);
        // setting these views to invisible, prevents the d-pad to do actions on them
        mFabInsertQuote.setVisibility(View.INVISIBLE);
        mFabTakePhoto.setVisibility(View.INVISIBLE);
        isOpen = false;
    }

    /**
     * checks for permission to use camera. Result is passed to onRequestPermissionsResult
     */
    private void tryUsingCamera() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_USE_CAMERA);
        } else if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            dispatchCameraIntent();
        }
    }

    /**
     * Launches camera intent with a temporary path
     */
    private void dispatchCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "");
            mTempContainerPath = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTempContainerPath);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Uploads the photo (if taken) to the storage and passes its link or null to saveGeoDiary
     */
    private void preparePhotoAndSave(){
        // if user took a photo, then a photo path exists
        if (mPhotoPath != null){
            mFirebaseStorage = FirebaseStorage.getInstance();
            // names the child as the last path segment
            mStorageReference = mFirebaseStorage.getReference().child("geodiary_photos").child(mPhotoPath.getLastPathSegment());

            // uploads file to Firebase Storage
            mStorageReference.putFile(mPhotoPath).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // gets url of the photo from storage
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    if (downloadUri != null){
                        saveGeoDiary(downloadUri.toString());
                    } else {
                        // something unexpected occurred, do nothing
                        showSnackbar(R.string.download_uri_error);
                    }
                }
            });
        } else {
            // if user didn't take a photo, save a null value for photoUrl field in database
            saveGeoDiary(null);
        }
    }

    /**
     * Saves the data to Firebase Database
     *
     * @param photoUrl the link to the photo or null if no photo was taken
     */
    public void saveGeoDiary(String photoUrl){
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        Log.d("USER: ", user.toString());
        if (user != null) {
            String mUserID = user.getUid();
            FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
            DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID);

            // saves data to Firebase Database
            GeoDiary geoContent = new GeoDiary(mTitleEditText.getText().toString(), mBodyEditText.getText().toString());
            String geoDiaryPushID = mDatabaseReference.push().getKey();
            mDatabaseReference.child(getResources().getString(R.string.node_contents) + geoDiaryPushID)
                    .setValue(geoContent);

            GeoDiary geoLocation = new GeoDiary(mLatitude, mLongitude);
            mDatabaseReference.child(getResources().getString(R.string.node_locations) + geoDiaryPushID)
                    .setValue(geoLocation);

            GeoDiary geoOverview = new GeoDiary(photoUrl);
            String locationsNode = getResources().getString(R.string.node_overviews) + geoDiaryPushID;
            mDatabaseReference.child(locationsNode)
                    .setValue(geoOverview);

            Toast.makeText(getApplicationContext(), getResources().getString(R.string.geodiary_saved_message), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Gets triggered when there was STATE_STORAGE_REFERENCE exists.
     * This is useful when user rotates the screen during a database transaction.
     *
     * @param stringRef the storage reference to the child node from Firebase Storage
     */
    public void restoreStorageTask(String stringRef){
        mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all UploadTasks under this StorageReference, in this case only one
        List<UploadTask> tasks = mStorageReference.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasks.get(0);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // gets url of the photo from storage
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    if (downloadUri != null){
                        saveGeoDiary(downloadUri.toString());
                    } else {
                        // something unexpected occurred, do nothing
                        showSnackbar(R.string.download_uri_error);
                    }
                }
            });
        }
    }

    private void showSnackbar(@StringRes int message) {
        Snackbar.make(mRootLayout, message, Snackbar.LENGTH_LONG).show();
    }
}