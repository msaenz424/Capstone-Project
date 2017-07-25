package com.android.mig.geodiary.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class OpenQuotesJsonUtils {

    /**
     * Breaks down the JSON string received into a JSON array and
     * save quotes into an array of strings
     *
     * @param quotesJsonResponse a JSON string response
     * @return an array list containing the quotes
     */
    public static ArrayList<String> getQuotesArrayFromJson(String quotesJsonResponse){
        final String M_QUOTE = "quote";
        ArrayList<String> mQuotesArray = new ArrayList<>();

        try {
            JSONArray quotesJsonArray = new JSONArray(quotesJsonResponse);

            for (int i = 0; i < quotesJsonArray.length(); i++) {
                JSONObject resultJsonObject = quotesJsonArray.getJSONObject(i);
                String quote = resultJsonObject.getString(M_QUOTE);
                mQuotesArray.add(quote);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mQuotesArray;
    }
}
