package com.android.mig.geodiary;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.android.mig.geodiary.models.GeoDiary;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int ZOOM_LEVEL = 12;
    private static final int ZOOM_DURATION = 2000;

    private GoogleMap mMap;
    private String mUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mUserID = getIntent().getStringExtra(Intent.EXTRA_UID);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void loadData(final GoogleMap googleMap){
        mMap = googleMap;

        // gets values from database in the "locations" node
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID + getResources().getString(R.string.node_locations));
        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // counter to check if loop reached last value in dataSnapshot
                long childrenCount = dataSnapshot.getChildrenCount();
                int i = 0;

                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                    i++;
                    GeoDiary geoDiary = dsp.getValue(GeoDiary.class);
                    double latitude = geoDiary.getLatitude();
                    double longitude = geoDiary.getLongitude();
                    LatLng coordinate = new LatLng(latitude, longitude);

                    // add a pin point in the map with a coordinate position
                    mMap.addMarker(new MarkerOptions().position(coordinate)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.redpen45)));

                    if (i == childrenCount){
                        // if it last children then zoom to the last location
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(coordinate));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(ZOOM_LEVEL), ZOOM_DURATION, null);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        loadData(googleMap);
    }
}
