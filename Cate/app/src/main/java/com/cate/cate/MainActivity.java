package com.cate.cate;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
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

import com.android.volley.toolbox.ImageRequest;
import com.cate.cate.api.ThaCatApiImage;
import com.cate.cate.api.ThaCatApiResponse;
import com.cate.cate.api.TheCatAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String CAT_KEY = "MTgwMTEz";
    private static final String CAT_FORMAT_XML = "xml";
    private static final String CAT_FORMAT_HTML = "html";
    private static final String CAT_SIZE_MEDIUM = "medium";
    private static final String CAT_SIZE_SMALL = "small";

    private static String CAT_IMAGE_URL = "";
    private static String CAT_TAG = "CATE";

    private static int WIDTH_SUB_SM = 175;
    private static int WIDTH_SUB_LG = 280;

    private static Snackbar snackbar;

    private Handler mHandler = new Handler();
    private boolean isRunning = true;
    private boolean hasInternet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final WebView webView = (WebView) findViewById(R.id.cat_imageView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findTheCat(webView);
            }
        });

        webView.loadData(getResources().getString(R.string.please_wait), "text/html; charset=utf-8", "utf-8");

        internetChecker();

        randomCatFacts(webView);
    }

    @Override
    protected void onStop() {
        super.onStop();

        clearCatImageUrl();

        VolleyRequestQueue.getInstance(this).cancelAll(CAT_TAG);

        isRunning = false;
    }

    protected void onResume() {
        super.onResume();

        if (! isRunning) {
            isRunning = true;
        }
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
        if (id == R.id.action_share_fb) {
            return true;
        } else if (id == R.id.action_save) {
            if (CAT_IMAGE_URL.equals("")) {
                showSnackbar("No cate to save. Tap the floating button to get one.");
            } else {
                saveCateImage();
                showSnackbar("Picture has been saved in Download folder");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void findTheCat(final WebView webView) {

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        webView.loadData(getResources().getString(R.string.please_wait), "text/html; charset=utf-8", "utf-8");

        if (snackbar != null) {
            snackbar.dismiss();
        }

        String catSize = MainActivity.CAT_SIZE_SMALL;

        if (displayMetrics.widthPixels > 500) {
            catSize = MainActivity.CAT_SIZE_MEDIUM;
        }

        TheCatAPI theCatAPI = TheCatAPI.retrofitCat.create(TheCatAPI.class);

        Call<ThaCatApiResponse> call = theCatAPI.loadCats(MainActivity.CAT_FORMAT_XML, MainActivity.CAT_KEY, catSize);


        call.enqueue(new Callback<ThaCatApiResponse>() {

            @Override
            public void onResponse(Call<ThaCatApiResponse> call, Response<ThaCatApiResponse> response) {
                try {

                    if(response.body().getImageList().size() > 0) {

                        ThaCatApiImage image = response.body().getImageList().get(0);

                        CAT_IMAGE_URL = image.getUrl();

                        int width = displayMetrics.widthPixels-WIDTH_SUB_SM;
                        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) // landscape
                        {
                            width = displayMetrics.widthPixels-WIDTH_SUB_LG;
                        }

                        String content = "<div style='position:relative; width:100%; height:70%; background-color: #FFEBCD;'><a target='_blank' href='"+ image.getSourceUrl() +"'><img style='object-fit: cover; height: auto; width: "+width+"px;' src='"+image.getUrl()+"'></a></div>";
                        webView.loadData(content, "text/html; charset=utf-8", "utf-8");

                        Log.d("MAIN", content);

                    } else {
                        findTheCat(webView);
                    }
                }catch (Exception e) {
                    clearCatImageUrl();
                    e.getLocalizedMessage();
                    // TODO: show popup error
                }
            }

            @Override
            public void onFailure(Call<ThaCatApiResponse> call, Throwable t) {
                clearCatImageUrl();
                // TODO: show popup error
            }
        });
    }

    private void randomCatFacts(final WebView webView) {

        TheCatAPI theCatAPI = TheCatAPI.retrofitFacts.create(TheCatAPI.class);

        Call<String> call = theCatAPI.loadCatFacts(1); // one only

        call.enqueue(new Callback<String>() {

            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {

                    JsonParser jsonParser = new JsonParser();
                    JsonObject jo = (JsonObject)jsonParser.parse(response.body());

                    if (jo.get("success") != null && jo.get("success").getAsBoolean()) {

                        JsonArray facts = jo.get("facts").getAsJsonArray();
                        if (facts != null && facts.size() > 0) {

                            String content = facts.get(0).toString();

                            String html = "<h2>Today's fact about Cate</h2><h3>"+content+"</h3>";

                            setWebView(webView, html);
                        }

                    } else {
                        // TODO: show popup error
                    }

                }catch (Exception e) {
                    // TODO: show popup error
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // TODO: show popup error
            }
        });
    }

    private void setWebView(final WebView webView, String content) {
        webView.loadData(content, "text/html; charset=utf-8", "utf-8");
    }

    private void clearCatImageUrl() {
        CAT_IMAGE_URL = "";
    }

    private void showSnackbar(String message) {

        snackbar = Snackbar.make(findViewById(R.id.cat_imageView), message, Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    private String imageExtension(String url) {
        if(url != null) {
            int lastIndexOfDot = url.lastIndexOf(".");
            return url.substring(lastIndexOfDot, url.length());
        }
        return "jpg"; // default
    }

    private void saveCateImage() {

        ImageRequest imageRequest = new ImageRequest(CAT_IMAGE_URL, new com.android.volley.Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {

                OutputStream fOut = null;

                try {
                    String imageExtension = imageExtension(CAT_IMAGE_URL);

                    final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/cate/");

                    // Make sure the Pictures directory exists.
                    if(!path.exists())
                    {
                        path.mkdirs();
                    }

                    String filename = UUID.randomUUID().toString().replaceAll("-", "");
                    final File file = new File(path, filename + imageExtension);

                    fOut = new FileOutputStream(file);

                    response.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

                }catch (Exception e) {
                    Log.d("MAIN", e.getMessage());
                } finally {
                    try {

                        fOut.close(); // do not forget to close the stream

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 0, null, null, null);

        // Access the RequestQueue through your singleton class.
        VolleyRequestQueue.getInstance(this).addToRequestQueue(imageRequest);
    }

    private void internetChecker() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (isRunning) {
                    try {
                        Thread.sleep(10000);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                // Write your code here to update the UI.
                                checkConnectivity();
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();
    }

    private void checkConnectivity() {

        ConnectivityManager cn=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nf=cn.getActiveNetworkInfo();

        if( nf == null || nf.isConnected() == false )
        {
            hasInternet = false;
            showSnackbar("Network is not available");

        } else {
            hasInternet = true;

            if (snackbar != null) {
                snackbar.dismiss();
            }
        }
    }
}
