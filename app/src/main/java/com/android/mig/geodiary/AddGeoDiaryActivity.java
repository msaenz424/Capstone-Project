package com.android.mig.geodiary;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.mig.geodiary.models.GeoDiary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class AddGeoDiaryActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST_USE_CAMERA = 10;        // code should be bigger than 0
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 20;

    EditText mTitleEditText, mBodyEditText;
    ImageView mThumbnailImageView;
    FloatingActionButton mFabPlus, mFabInsertQuote, mFabTakePhoto;
    Animation mScaleUpAnimation, mScaleDownAnimation,
            mFabClockwiseAnimation, mFabCounterClockwiseAnimation;
    Uri mPhotoPath;
    boolean isOpen = false;
    double mLatitude, mLongitude;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geodiary);

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
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("geodiaries");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageReference = mFirebaseStorage.getReference().child("geodiary_photos");

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
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mThumbnailImageView.setImageBitmap(imageBitmap);
            // gets the URI from the bitmap
            mPhotoPath = getImageUri(getApplicationContext(), imageBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_geodiary_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_submit) {
            uploadGeoDiary();
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
        mLocationRequest = LocationRequest.create();
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
        mFabTakePhoto.setClickable(true);
        mFabInsertQuote.startAnimation(mScaleUpAnimation);
        mFabInsertQuote.setClickable(true);
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
    }

    /**
     * Hides fab buttons with animation
     */
    private void disableButtons() {
        mFabInsertQuote.startAnimation(mScaleDownAnimation);
        mFabTakePhoto.startAnimation(mScaleDownAnimation);
        mFabPlus.startAnimation(mFabCounterClockwiseAnimation);
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
     * Launches camera intent
     */
    private void dispatchCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Gets the Uri from the photo that was taken
     *
     * @param context   activity context
     * @param image     photo that was taken
     * @return a Uri path to the image file
     */
    public Uri getImageUri(Context context, Bitmap image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), image, "Title", null);
        return Uri.parse(path);
    }

    /**
     * Saves the data entered by the user to Firebase Database, and Firebase Storage for the photo
     */
    private void uploadGeoDiary(){
        // names the child as the last path segment
        StorageReference photoRef = mStorageReference.child(mPhotoPath.getLastPathSegment());

        // uploads file to Firebase Storage
        photoRef.putFile(mPhotoPath).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // gets url of the photo from storage
                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                // saves data to Firebase Database
                GeoDiary geo = new GeoDiary(
                        mTitleEditText.getText().toString(),
                        mBodyEditText.getText().toString(),
                        downloadUrl.toString(),
                        mLongitude,
                        mLatitude);
                mDatabaseReference.push().setValue(geo);
            }
        });
        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.add_geodiary_coordinator_layout),
                R.string.geodiary_saved_message, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }
}
