package com.android.mig.geodiary;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class AddGeoDiaryActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST_USE_CAMERA = 10;     // code should be bigger than 0

    EditText mTitleEditText, mBodyEditText;
    ImageView mThumbnailImageView;
    FloatingActionButton mFabPlus, mFabSubmit, mFabTakePhoto;
    Animation mScaleUpAnimation, mScaleDownAnimation,
            mFabClockwiseAnimation, mFabCounterClockwiseAnimation;
    Uri mPhotoPath;
    boolean isOpen = false;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geodiary);

        mTitleEditText = (EditText) findViewById(R.id.title_edit_text);
        mBodyEditText = (EditText) findViewById(R.id.body_edit_text);
        mThumbnailImageView = (ImageView) findViewById(R.id.thumbnail_image_view);
        mFabPlus = (FloatingActionButton) findViewById(R.id.fab_plus);
        mFabSubmit = (FloatingActionButton) findViewById(R.id.fab_submit);
        mFabTakePhoto = (FloatingActionButton) findViewById(R.id.fab_take_photo);

        mScaleUpAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_up);
        mScaleDownAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_scale_down);
        mFabClockwiseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_clockwise);
        mFabCounterClockwiseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_counterclockwise);

        mFabPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // fab buttons are visible
                if (isOpen){
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receives result code from camera intent launched previously
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //mPhotoPath = mPhotoIntent = data;
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
        if (item.getItemId() == R.id.action_submit){
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
        }
    }

    /**
     * Shows fab buttons with animation and set their OnClick methods
     */
    private void enableButtons(){
        mFabTakePhoto.startAnimation(mScaleUpAnimation);
        mFabTakePhoto.setClickable(true);
        mFabSubmit.startAnimation(mScaleUpAnimation);
        mFabSubmit.setClickable(true);
        mFabPlus.startAnimation(mFabClockwiseAnimation);
        isOpen = true;
        mFabTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryUsingCamera();
            }
        });

        mFabSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    /**
     * Hides fab buttons with animation
     */
    private void disableButtons(){
        mFabSubmit.startAnimation(mScaleDownAnimation);
        mFabTakePhoto.startAnimation(mScaleDownAnimation);
        mFabPlus.startAnimation(mFabCounterClockwiseAnimation);
        isOpen = false;
    }

    /**
     * checks for permission to use camera. Result is passed to onRequestPermissionsResult
     */
    private void tryUsingCamera(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_USE_CAMERA);
        } else if (permissionCheck == PackageManager.PERMISSION_GRANTED){
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
     * @return          a Uri path to the image file
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
                        -34,
                        15.11);
                mDatabaseReference.push().setValue(geo);
            }
        });
    }
}
