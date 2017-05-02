package com.cate.cate.api;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TheCatAPI {

    String CAT_ENDPOINT = "http://thecatapi.com/api/images/";
    String FACTS_ENDPOINT = "http://catfacts-api.appspot.com/api/";

    @GET("get")
    Call<ThaCatApiResponse> loadCats(@Query("format") String format, @Query("api_key") String apiKey, @Query("size") String size, @Query("results_per_page") String resultsPerPage);

    @GET("facts")
    Call<String> loadCatFacts(@Query("number") int format);

    public static final Retrofit retrofitCat = new Retrofit.Builder()
            .baseUrl(TheCatAPI.CAT_ENDPOINT)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build();

    public static final Retrofit retrofitFacts = new Retrofit.Builder()
            .baseUrl(TheCatAPI.FACTS_ENDPOINT)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build();
}
