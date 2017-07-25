package com.android.mig.geodiary.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.mig.geodiary.R;
import com.android.mig.geodiary.adapters.QuotesAdapter;
import com.android.mig.geodiary.utils.NetworkUtils;
import com.android.mig.geodiary.utils.OpenQuotesJsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.net.URL;

public class QuotesFragment extends Fragment {

    View rootView;
    QuotesAdapter mQuotesAdapter;

    public QuotesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_quotes, container, false);
        RecyclerView mQuotesRecyclerView = (RecyclerView) rootView.findViewById(R.id.quotes_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false);
        mQuotesRecyclerView.setLayoutManager(linearLayoutManager);
        mQuotesRecyclerView.setHasFixedSize(true);
        mQuotesAdapter = new QuotesAdapter();
        mQuotesRecyclerView.setAdapter(mQuotesAdapter);
        FetchQuotesTask fetchQuotesTask = new FetchQuotesTask();
        fetchQuotesTask.execute();

        return rootView;
    }

    public class FetchQuotesTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            URL movieUrl = NetworkUtils.buildURI();
            try {
                String strResponse = NetworkUtils.getResponseFromHttpUrl(movieUrl);
                ArrayList<String> quotesArrayFromJson = OpenQuotesJsonUtils.getQuotesArrayFromJson(strResponse);
                return quotesArrayFromJson;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> quotesData) {
            if (quotesData != null) {
                mQuotesAdapter.setQuotesAdapter(quotesData);
            }
        }
    }
}