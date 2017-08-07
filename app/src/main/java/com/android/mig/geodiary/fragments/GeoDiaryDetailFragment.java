package com.android.mig.geodiary.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mig.geodiary.services.FetchAddressIntentService;
import com.android.mig.geodiary.R;
import com.android.mig.geodiary.models.GeoDiary;
import com.android.mig.geodiary.utils.Constants;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GeoDiaryDetailFragment extends Fragment {

    private CollapsingToolbarLayout mTitleCollapsingLayout;
    private TextView mContentTextView, mDateTextView, mLocationTextView;
    private ImageView mPhotoImageView;

    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReferenceOverview, mDatabaseReferenceLocation, mDatabaseReferenceContent;

    public GeoDiaryDetailFragment(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_geodiary_detail, container, false);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        String mUserID = user.getUid();

        final String mGeoDiaryKey = getActivity().getIntent().getStringExtra(Intent.EXTRA_KEY_EVENT);

        mTitleCollapsingLayout = (CollapsingToolbarLayout) rootView.findViewById(R.id.det_collapsing_layout);
        mContentTextView = (TextView) rootView.findViewById(R.id.det_content_text_view);
        mDateTextView = (TextView) rootView.findViewById(R.id.det_date_text_view);
        mLocationTextView = (TextView) rootView.findViewById(R.id.det_location_text_view);
        mPhotoImageView = (ImageView) rootView.findViewById(R.id.det_photo_image_view);

        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.det_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReferenceOverview = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID + getResources().getString(R.string.node_overviews)).child(mGeoDiaryKey);
        mDatabaseReferenceLocation = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID + getResources().getString(R.string.node_locations)).child(mGeoDiaryKey);
        mDatabaseReferenceContent = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID + getResources().getString(R.string.node_contents)).child(mGeoDiaryKey);

        mDatabaseReferenceOverview.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // retrieves photo and date from the overviews node
                GeoDiary geoDiary = dataSnapshot.getValue(GeoDiary.class);
                String dateString = null;
                if (geoDiary != null) {
                    String photoUrl = geoDiary.getPhotoUrl();
                    if (photoUrl != null){
                        Glide.with(getContext()).load(photoUrl).into(mPhotoImageView);
                    }
                    long date = geoDiary.getDate() * 1000L;
                    dateString = DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_YEAR);
                }
                mDateTextView.setText(dateString);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        mDatabaseReferenceLocation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // retrieves latitude and longitude from the locations node
                if (dataSnapshot.exists()){
                    GeoDiary geoDiary = dataSnapshot.getValue(GeoDiary.class);
                    if (geoDiary != null) {
                        double lat = geoDiary.getLatitude();
                        double lon = geoDiary.getLongitude();
                        showLocation(lat, lon);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        mDatabaseReferenceContent.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // retrieves title and content from the contents node
                GeoDiary geoDiary = dataSnapshot.getValue(GeoDiary.class);
                if (geoDiary != null) {
                    mTitleCollapsingLayout.setTitle(geoDiary.getTitle());
                    mContentTextView.setText(geoDiary.getContent());
                    mContentTextView.setContentDescription(geoDiary.getTitle() + "\n" + geoDiary.getContent());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        return rootView;
    }

    /**
     * Starts an intent service to try obtain the location
     *
     * @param latitude  latitude
     * @param longitude longitude
     */
    private void showLocation(double latitude, double longitude){
        LocationResultReceiver mLocationResultReceiver = new LocationResultReceiver(new Handler());

        Intent intent = new Intent(getActivity(), FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mLocationResultReceiver);
        intent.putExtra(Constants.LATITUDE, latitude);
        intent.putExtra(Constants.LONGITUDE, longitude);
        getActivity().startService(intent);
    }

    /**
     * Class to receive the result after the Intent Service tried to obtain the location
     */
    private class LocationResultReceiver extends ResultReceiver {
        public LocationResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String locationString = resultData.getString(Constants.RESULT_DATA_KEY);
            // Shows the location if it was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                mLocationTextView.setText(locationString);
            } else {
                mLocationTextView.setVisibility(View.GONE);
            }

        }
    }
}
