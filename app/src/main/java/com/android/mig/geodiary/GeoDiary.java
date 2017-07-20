package com.android.mig.geodiary;

public class GeoDiary {
    private final static int ONE_SECOND_IN_MILLISECONDS = 1000;

    private String mTitle;
    private String mContent;
    private String mPhotoUrl;
    private long mDate;
    private double mLongitude;
    private double mLatitude;

    public GeoDiary() {
    }

    public GeoDiary(String mTitle, String mContent, String mPhotoUrl, double mLongitude, double mLatitude) {
        this.mTitle = mTitle;
        this.mContent = mContent;
        this.mPhotoUrl = mPhotoUrl;
        this.mDate =  (System.currentTimeMillis() / ONE_SECOND_IN_MILLISECONDS);
        this.mLongitude = mLongitude;
        this.mLatitude = mLatitude;
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

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public double getmLatitude() {
        return mLatitude;
    }

    public void setmLatitude(double latitude) {
        this.mLatitude = latitude;
    }
}
