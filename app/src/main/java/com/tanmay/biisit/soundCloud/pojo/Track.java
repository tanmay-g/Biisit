package com.tanmay.biisit.soundCloud.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tanmay.godbole on 09-03-2017
 */

public class Track {

    @SerializedName("title")
    private String mTitle;

    @SerializedName("id")
    private int mID;

    @SerializedName("stream_url")
    private String mStreamURL;

    @SerializedName("artwork_url")
    private String mArtworkURL;

    @SerializedName("duration")
    private int mDuration;

    @SerializedName("user")
    private User mUser;

    public String getTitle() {
        return mTitle;
    }

    public int getID() {
        return mID;
    }

    public String getStreamURL() {
        return mStreamURL;
    }

    public String getArtworkURL() {
        return mArtworkURL;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getUserName() {
        return mUser.getName();
    }

}

class User{

    @SerializedName("username")
    private String mName;

    public String getName() {
        return mName;
    }
}