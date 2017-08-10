package com.android.mig.geodiary;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.annotation.NonNull;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mig.geodiary.adapters.GeoDiaryViewHolder;
import com.android.mig.geodiary.models.GeoDiary;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class GalleryActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    private RecyclerView mGalleryRecyclerView;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseRecyclerAdapter mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        FloatingActionButton mFabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        mGalleryRecyclerView = (RecyclerView) findViewById(R.id.gallery_recycler_view);
        mGalleryRecyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns()));
        mGalleryRecyclerView.hasFixedSize();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // already signed in
                    loadData(user.getUid());
                } else {
                    // not signed in
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(!BuildConfig.DEBUG)  // disables it for debug
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        // opens activity to add a new GeoDiary
        mFabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GalleryActivity.this, AddGeoDiaryActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_CANCELED) {
                // Sign in failed
                if (response != null) {
                    if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                        Toast.makeText(this, getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                }
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_geodairy_map:
                Intent intent = new Intent(GalleryActivity.this, MapsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_sign_out:
                AuthUI.getInstance().signOut(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * stops listening for changes on database when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFirebaseAdapter != null){
            mFirebaseAdapter.cleanup();
        }

    }

    /**
     * Loads all the data from database that belongs only to the current user
     */
    private void loadData(String userId) {
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference().child("geodiaries/" + userId + getResources().getString(R.string.node_overviews));
        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // displays a message if nothing was retrieved from database
                TextView mNoItemsTextView = (TextView) findViewById(R.id.no_items_text_view);
                if (!dataSnapshot.hasChildren()){
                    mNoItemsTextView.setVisibility(View.VISIBLE);
                }else{
                    mNoItemsTextView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });

        mFirebaseAdapter = new FirebaseRecyclerAdapter<GeoDiary, GeoDiaryViewHolder>(
                GeoDiary.class,
                R.layout.item_gallery,
                GeoDiaryViewHolder.class,
                mDatabaseReference) {
            @Override
            protected void populateViewHolder(GeoDiaryViewHolder viewHolder, GeoDiary geoDiary, final int position) {
                viewHolder.setPhoto(geoDiary.getPhotoUrl());
                // formats long timestamp to readable date string
                long date = geoDiary.getDate() * 1000L;
                String dateString = DateUtils.formatDateTime(getApplicationContext(), date, DateUtils.FORMAT_SHOW_YEAR);
                viewHolder.setDate(dateString);

                // activates the click listener to each item in the list
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Bundle bundle = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            ImageView sharedView = (ImageView) view.findViewById(R.id.photo_image_view);
                            // takes care of the shared element transition
                            bundle = ActivityOptions
                                    .makeSceneTransitionAnimation(GalleryActivity.this, sharedView, sharedView.getTransitionName())
                                    .toBundle();
                        }
                        // gets the child key (from database) of the selected item in the list
                        // and pass it to the next activity
                        String geoDiaryKey = mFirebaseAdapter.getRef(position).getKey();
                        Intent intent = new Intent(GalleryActivity.this, GeoDiaryDetailActivity.class);
                        intent.putExtra(Intent.EXTRA_KEY_EVENT, geoDiaryKey);   // passes selected diary key
                        startActivity(intent, bundle);
                    }
                });
            }
        };
        mGalleryRecyclerView.setAdapter(mFirebaseAdapter);
    }

    /**
     * Helps finding the appropriate number of columns for the recyclerview
     * based on screen size and orientation
     *
     * @return proper number of columns to be used
     */
    private int numberOfColumns() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // You can change this divider to adjust the size of the poster
        int widthDivider = 400;
        int width = displayMetrics.widthPixels;
        int nColumns = width / widthDivider;
        if (nColumns < 2) return 2;
        return nColumns;
    }
}
