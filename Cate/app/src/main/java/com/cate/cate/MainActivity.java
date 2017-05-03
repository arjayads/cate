package com.cate.cate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String CAT_KEY = "MTgwMTEz";
    private static final String CAT_FORMAT_XML = "xml";
    private static final String CAT_SIZE_MEDIUM = "medium";
    private static final String CAT_SIZE_SMALL = "small";
    private static final String CAT_RESULT_PER_PAGE = "15";

    private static String CAT_IMAGE_URL = "";
    private static String CAT_IMAGE_RESOURCE_URL = "";
    private static String CAT_TAG = "CATE";

    private static Snackbar snackbar;

    private Handler mHandler = new Handler();
    private boolean isRunning = true;
    private boolean hasInternet = false;

    private boolean settingImageToWebview = false;

    private List<ThaCatApiImage> imageList = new ArrayList<>();

    private WebView webView;
    final DisplayMetrics displayMetrics = new DisplayMetrics();

    private String[] badWords = {
            "masturbate",
            "fuck",
            "f*ck",
            "f**k",
            "f#ck",
            "shit",
            "sh!t",
            "sh*t",
            "sh*t",
            "dick",
            "d*ck",
            "d!ck",
            "bitch",
            "b!tch",
            "b*tch",
            "asshole",
            "a**hole",
            "ahole",
            "arsehole",
            "slut",
            "sl*t",
            "douche",
            "d**che",
            "fag",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        webView = (WebView) findViewById(R.id.cat_imageView);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findTheCat();
            }
        });

        webView.loadData(getResources().getString(R.string.please_wait), "text/html; charset=utf-8", "utf-8");

        internetChecker();

        randomCatFacts();
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
        if (id == R.id.action_share_fb ) {

            if ( ! CAT_IMAGE_URL.equals("")) {

                String urlToShare = CAT_IMAGE_URL;
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");

                intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

                // See if official Facebook app is found
                boolean facebookAppFound = false;
                List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
                for (ResolveInfo info : matches) {
                    if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
                        intent.setPackage(info.activityInfo.packageName);
                        facebookAppFound = true;
                        break;
                    }
                }

                // As fallback, launch sharer.php in a browser
                if (!facebookAppFound) {
                    String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
                }

                startActivity(intent);

            } else {
                showSnackbar("No cate to share. Tap the floating button to get one.");
            }

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

    private void findTheCat() {

        clearCatImageUrl();

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        webView.loadData(getResources().getString(R.string.please_wait), "text/html; charset=utf-8", "utf-8");

        if (snackbar != null) {
            snackbar.dismiss();
        }

        if(! imageList.isEmpty() && !settingImageToWebview) { // prevent overlapping tasks
            new WebViewImageSetter().execute("");
            return;
        }

        String catSize = MainActivity.CAT_SIZE_SMALL;

        if (displayMetrics.widthPixels > 500) {
            catSize = MainActivity.CAT_SIZE_MEDIUM;
        }

        TheCatAPI theCatAPI = TheCatAPI.retrofitCat.create(TheCatAPI.class);

        Call<ThaCatApiResponse> call = theCatAPI.loadCats(MainActivity.CAT_FORMAT_XML, MainActivity.CAT_KEY, catSize, MainActivity.CAT_RESULT_PER_PAGE);


        call.enqueue(new Callback<ThaCatApiResponse>() {

            @Override
            public void onResponse(Call<ThaCatApiResponse> call, Response<ThaCatApiResponse> response) {
                try {

                    if(response.body().getImageList().size() > 0) {
                        imageList = response.body().getImageList();
                        new WebViewImageSetter().execute("");

                    } else {
                        findTheCat();
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

    private String webViewContent() {
        return "<body style='margin:0;padding:0;'><img style='padding: 0; object-fit: cover; height: auto; width: 100%;' src='"+CAT_IMAGE_URL+"'></body>";
    }

    private void randomCatFacts() {

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

                            if (! containsBadWord(content)) {

                                String html = "<h2>Today's fact about Cate</h2><h3>"+content+"</h3>";

                                setWebView(webView, html);

                            } else {
                                randomCatFacts();
                            }
                        }

                    } else {
                        randomCatFacts();
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

    private boolean containsBadWord(String content) {

        if (content != null) {
            String contentLower = content.toLowerCase();

            for (int x=0; x<badWords.length; x ++) {

                if (contentLower.contains(badWords[x])) {
                    return true;
                }
            }
        }

        return false;
    }

    private void setWebView(final WebView webView, String content) {
        webView.loadData(content, "text/html; charset=utf-8", "utf-8");
    }

    private void clearCatImageUrl() {
        CAT_IMAGE_URL = "";
        CAT_IMAGE_RESOURCE_URL = "";
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


    private class WebViewImageSetter extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            try {

                if(! imageList.isEmpty()) {

                    ThaCatApiImage image = imageList.get(0);

                    CAT_IMAGE_URL = image.getUrl();
                    CAT_IMAGE_RESOURCE_URL = image.getSourceUrl();

                    imageList.remove(0);

                    URL u = new URL(CAT_IMAGE_URL);

                    connection = (HttpURLConnection) u.openConnection();
                    connection.setRequestMethod("GET");
                    int code = connection.getResponseCode();

                    if (code == 200) return "OK";

                } else {
                    return "EMPTY";
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return "Failed";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == "OK") {

                webView.loadData(webViewContent(), "text/html; charset=utf-8", "utf-8");

            } else if (result == "EMPTY") {

                findTheCat();

            } else {
                new WebViewImageSetter().execute("");
            }

            settingImageToWebview = false;
        }

        @Override
        protected void onPreExecute() {
            settingImageToWebview = true;
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}
