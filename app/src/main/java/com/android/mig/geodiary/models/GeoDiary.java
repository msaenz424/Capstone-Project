package com.android.mig.geodiary.models;

public class GeoDiary {
    private final static int ONE_SECOND_IN_MILLISECONDS = 1000;

    private String mTitle;
    private String mContent;
    private String mPhotoUrl;
    private long mDate;
    private double mLatitude;
    private double mLongitude;

    public GeoDiary() {
    }

    public GeoDiary(String title, String content){
        this.mTitle = title;
        this.mContent = content;
    }

    public GeoDiary(double latitude, double longitude){
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }

    public GeoDiary(String photoUrl){
        this.mPhotoUrl = photoUrl;
        this.mDate =  (System.currentTimeMillis() / ONE_SECOND_IN_MILLISECONDS);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.mPhotoUrl = photoUrl;
    }

    public Long getDate() {
        if (mDate == 0){
            return null;
        }
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public Double getLatitude() {
        if (this.mLatitude == 0){
            return null;
        }
        return mLatitude;
    }

    public Double getLongitude() {
        if (this.mLongitude == 0){
            return null;
        }
        return mLongitude;
    }

    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }
}
