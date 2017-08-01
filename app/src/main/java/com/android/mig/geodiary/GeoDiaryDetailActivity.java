package com.android.mig.geodiary;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.android.mig.geodiary.fragments.GeoDiaryDetailFragment;

public class GeoDiaryDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geodiary_detail);

        GeoDiaryDetailFragment mGeoDiaryDetailFragment = new GeoDiaryDetailFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.geodiary_detail_container, mGeoDiaryDetailFragment)
                .commit();
    }
}
