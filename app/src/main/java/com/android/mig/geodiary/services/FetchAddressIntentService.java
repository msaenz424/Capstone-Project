package com.android.mig.geodiary.services;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.android.mig.geodiary.utils.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    private static final int MAX_RESULTS = 1;

    protected ResultReceiver mReceiver;

    public FetchAddressIntentService(){
        super("FetchAddressIntentService");
    }

    public FetchAddressIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // obtains the values passed from GeoDiaryDetailFragment
        double mLatitude = intent.getDoubleExtra(Constants.LATITUDE, 0);
        double mLongitude = intent.getDoubleExtra(Constants.LONGITUDE, 0);
        this.mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(mLatitude, mLongitude, MAX_RESULTS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null) {
            // sends a success code and the string of a concatenated admin area, locality and country
            String adminArea = addresses.get(0).getAdminArea();
            String cityName = addresses.get(0).getLocality();
            String countryName = addresses.get(0).getCountryName();
            deliverResultToReceiver(Constants.SUCCESS_RESULT, cityName + ", " + adminArea + "\n" + countryName);
        } else {
            // sends a fail code and and empty string
            deliverResultToReceiver(Constants.FAILURE_RESULT, "");
        }

    }

    /**
     * Delivers the string result
     *
     * @param resultCode    an integer that represent success or fail
     * @param resultData    the location represented as a string
     */
    private void deliverResultToReceiver(int resultCode, String resultData) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, resultData);
        mReceiver.send(resultCode, bundle);
    }
}
