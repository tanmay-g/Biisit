package com.tanmay.biisit.soundCloud;

import com.tanmay.biisit.BuildConfig;
import com.tanmay.biisit.soundCloud.pojo.Track;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by tanmay.godbole on 09-03-2017
 */

public interface SCService {

    @GET("/tracks?client_id=" + BuildConfig.SOUNDCLOUD_CLIENT_ID)
    Call<List<Track>> getTracksByTag(@Query("tags") String tags);

    @GET("/tracks?client_id=" + BuildConfig.SOUNDCLOUD_CLIENT_ID)
    Call<List<Track>> getTracksByKey(@Query("q") String query);

}
