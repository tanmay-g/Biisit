package com.tanmay.biisit.soundCloud.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.tanmay.biisit.BuildConfig;

/**
 * Created by tanmay.godbole on 09-03-2017
 */

public class Track  implements Parcelable{

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
        return mStreamURL + "?client_id=" + BuildConfig.SOUNDCLOUD_CLIENT_ID;
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

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Track> CREATOR
            = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };


    private Track (Parcel in){
        mTitle = in.readString();
        mID = in.readInt();
        mStreamURL = in.readString();
        mArtworkURL = in.readString();
        mDuration = in.readInt();
        mUser = new User(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeInt(mID);
        dest.writeString(mStreamURL);
        dest.writeString(mArtworkURL);
        dest.writeInt(mDuration);
        dest.writeString(mUser.getName());
    }
}

class User{

    User(String name){
        mName = name;
    }

    @SerializedName("username")
    private String mName;

    public String getName() {
        return mName;
    }
}