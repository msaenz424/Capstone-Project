package com.android.mig.geodairy;

import android.content.Intent;
import android.support.annotation.StringRes;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.View;

import com.android.mig.geodairy.adapters.GeodairyViewHolder;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class GalleryActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    private View mRootView;
    private RecyclerView mGalleryRecyclerView;
    private FloatingActionButton mFabAdd;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private FirebaseRecyclerAdapter mFirebaseAdapter;

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
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        if (mFirebaseAuth.getCurrentUser() != null) {
            // already signed in
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

        mDatabaseReference = mFirebaseDatabase.getReference().child("geodairies");

        // insert data for test purposes
        //GeoDairy geo = new GeoDairy("title", "content", "url goes here", -34, 15.11);
        //mDatabaseReference.push().setValue(geo);

        mFirebaseAdapter = new FirebaseRecyclerAdapter<GeoDairy, GeodairyViewHolder>(
                GeoDairy.class,
                R.layout.item_gallery,
                GeodairyViewHolder.class,
                mDatabaseReference) {
            @Override
            protected void populateViewHolder(GeodairyViewHolder viewHolder, GeoDairy geoDairy, int position) {
                viewHolder.setPhoto(geoDairy.getPhotoUrl());
                // formats long timestamp to readable date string
                long date = geoDairy.getDate()*1000L;
                String dateString = DateUtils.formatDateTime(getApplicationContext(), date, DateUtils.FORMAT_SHOW_YEAR);
                viewHolder.setDate(dateString);
            }
        };
        mGalleryRecyclerView.setAdapter(mFirebaseAdapter);

        // opens activity to add a new geodairy
        mFabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GalleryActivity.this, AddGeodairyActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                /** TODO: loads data from database*/
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
    protected void onDestroy() {
        super.onDestroy();
        mFirebaseAdapter.cleanup();
    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mRootView, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }
}
