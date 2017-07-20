package com.android.mig.geodairy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class AddGeodairyActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    ImageView mThumbnailImageView;
    FloatingActionButton mFabPlus, mFabSubmit, mFabTakePhoto;
    Animation mScaleUpAnimation, mScaleDownAnimation,
            mFabClockwiseAnimation, mFabCounterClockwiseAnimation;
    boolean isOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_geodairy);

        mThumbnailImageView = (ImageView) findViewById(R.id.thumbnail_image_view);
        mFabPlus = (FloatingActionButton) findViewById(R.id.fab_plus);
        mFabSubmit = (FloatingActionButton) findViewById(R.id.fab_submit);
        mFabTakePhoto = (FloatingActionButton) findViewById(R.id.fab_take_photo);

        mScaleUpAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_scale_up);
        mScaleDownAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_scale_down);
        mFabClockwiseAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_clockwise);
        mFabCounterClockwiseAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_counterclockwise);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // receives result code from camera intent launched previously
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mThumbnailImageView.setImageBitmap(imageBitmap);
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
                dispatchTakePictureIntent();
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
     * Launches camera intent
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
}
