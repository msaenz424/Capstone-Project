package com.android.mig.geodiary;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.mig.geodiary.adapters.GeoDiaryViewHolder;
import com.android.mig.geodiary.models.GeoDiary;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
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

    private View mRootView;
    private RecyclerView mGalleryRecyclerView;
    private FloatingActionButton mFabAdd;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseRecyclerAdapter mFirebaseAdapter;

    String mUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        mRootView =  findViewById(R.id.login_root);
        mFabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        mGalleryRecyclerView = (RecyclerView) findViewById(R.id.gallery_recycler_view);
        mGalleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mGalleryRecyclerView.hasFixedSize();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // already signed in
                    onSignedInInitialize(user.getUid());
                    loadData();
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
                intent.putExtra(Intent.EXTRA_UID, mUserID);
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

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                loadData();
                return;
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection);
                    return;
                } else {
                    showSnackbar(R.string.unknown_error);
                    return;
                }
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
                intent.putExtra(Intent.EXTRA_UID, mUserID);             // passes the user id
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
        mFirebaseAdapter.cleanup();
    }

    /**
     * passes the user name from fire auth
     *
     * @param user user id
     */
    private void onSignedInInitialize(String user) {
        mUserID = user;
    }

    /**
     * Loads all the data from database that belongs only to the current user
     */
    private void loadData() {
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mDatabaseReference = mFirebaseDatabase.getReference().child("geodiaries/" + mUserID + getResources().getString(R.string.node_overviews));
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
                        // gets the child key (from database) of the selected item in the list
                        // and pass it to the next activity
                        String geoDiaryKey = mFirebaseAdapter.getRef(position).getKey();
                        Intent intent = new Intent(GalleryActivity.this, GeoDiaryDetailActivity.class);
                        intent.putExtra(Intent.EXTRA_UID, mUserID);             // passes the user id
                        intent.putExtra(Intent.EXTRA_KEY_EVENT, geoDiaryKey);   // passes selected diary key
                        startActivity(intent);
                    }
                });
            }
        };
        mGalleryRecyclerView.setAdapter(mFirebaseAdapter);
    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
