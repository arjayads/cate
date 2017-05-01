package com.cate.cate;

import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import com.cate.cate.api.ThaCatApiImage;
import com.cate.cate.api.ThaCatApiResponse;
import com.cate.cate.api.TheCatAPI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String CAT_KEY = "MTgwMTEz";
    private static final String CAT_FORMAT_XML = "xml";
    private static final String CAT_FORMAT_HTML = "html";
    private static final String CAT_SIZE_MEDIUM = "medium";
    private static final String CAT_SIZE_SMALL = "small";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                WebView webView = (WebView) findViewById(R.id.cat_imageView);
                webView.loadData("Please wait...", "text/html; charset=utf-8", "utf-8");

                // TODO: internet availability
                findTheCat(webView);
            }
        });

        fab.callOnClick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_random_facts) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void findTheCat(final WebView webView) {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        String catSize = MainActivity.CAT_SIZE_SMALL;

        if (displayMetrics.widthPixels > 500) {
            catSize = MainActivity.CAT_SIZE_MEDIUM;
        }

        TheCatAPI theCatAPI = TheCatAPI.retrofit.create(TheCatAPI.class);

        Call<ThaCatApiResponse> call = theCatAPI.loadCats(MainActivity.CAT_FORMAT_XML, MainActivity.CAT_KEY, catSize);

        call.enqueue(new Callback<ThaCatApiResponse>() {

            @Override
            public void onResponse(Call<ThaCatApiResponse> call, Response<ThaCatApiResponse> response) {
                try {

                    if(response.body().getImageList().size() > 0) {

                        ThaCatApiImage image = response.body().getImageList().get(0);

                        int width = displayMetrics.widthPixels-175;
                        if(getResources().getConfiguration().orientation == 2) // landscape
                        {
                            width = displayMetrics.widthPixels-280;
                        }

                        String content = "<a target='_blank' href='"+ image.getSourceUrl() +"'><img style='object-fit: cover; height: auto; width: "+width+"px;' src='"+image.getUrl()+"'></a>";
                        webView.loadData(content, "text/html; charset=utf-8", "utf-8");

                    } else {
                        findTheCat(webView);
                    }
                }catch (Exception e) {
                    e.getLocalizedMessage();
                    // TODO: show popup error
                }
            }

            @Override
            public void onFailure(Call<ThaCatApiResponse> call, Throwable t) {
                // TODO: show popup error
            }
        });
    }
}
