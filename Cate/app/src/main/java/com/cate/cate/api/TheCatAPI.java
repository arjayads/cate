package com.cate.cate.api;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TheCatAPI {

    String ENDPOINT = "http://thecatapi.com/";

    @GET("api/images/get")
    Call<ThaCatApiResponse> loadCats(@Query("format") String format, @Query("api_key") String apiKey, @Query("size") String size);

    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(TheCatAPI.ENDPOINT)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build();
}
