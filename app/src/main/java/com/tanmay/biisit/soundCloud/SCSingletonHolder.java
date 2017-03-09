package com.tanmay.biisit.soundCloud;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.tanmay.biisit.soundCloud.SoundCloudFragment.API_URL;

/**
 * Created by tanmay.godbole on 09-03-2017
 */

public class SCSingletonHolder {

    private static final Retrofit sRetrofitObj = new Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final SCService sSCService = sRetrofitObj.create(SCService.class);

    public static SCService getService() {
        return sSCService;
    }

}
