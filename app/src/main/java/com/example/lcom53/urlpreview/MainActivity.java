package com.example.lcom53.urlpreview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    EditText etUrl;
    TextView tvSend;
    private String TAG = MainActivity.class.getSimpleName();
    ImageView ivBackground, ivFevicon;
    TextView tvUrlTitle, tvDescription;
    private TextView tvTitle;
    RelativeLayout rlDemo;
    Point size;
    int width = 0, height = 0;
    public static final String ACTION_IMAGE_SAVED = "imageSaved";
    int imageMinHeight = 0, imageMaxWidth = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Display display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        imageMinHeight = (int) dpToPx(150);
        imageMaxWidth = (int) (width - (dpToPx(10) * 2));
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        etUrl = (EditText) findViewById(R.id.etUrl);
        tvSend = (TextView) findViewById(R.id.tvSend);
        tvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlToCheck = etUrl.getText().toString();
                new Urlparser().execute(urlToCheck);
            }
        });
        rlDemo = (RelativeLayout) findViewById(R.id.rlDemo);
        ivBackground = (ImageView) findViewById(R.id.ivBackground);
        ivFevicon = (ImageView) findViewById(R.id.ivFevicon);
        tvUrlTitle = (TextView) findViewById(R.id.tvUrlTitle);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        File file = new File("/storage/sdcard0/Android/data/com.example.lcom53.urlpreview/files/http%3A%2F%2Fwww.twitter.com.png");
        if (file.exists()) {
            ivBackground.setTag(target1);
            Picasso.with(this).load(file).into(target1);
        }
    }

    Target target1 = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            ivBackground.setImageBitmap(bitmap);
            float originHeight = bitmap.getHeight();
            float originWidh = bitmap.getWidth();
            float modifyHeight = originHeight;
            float modifyWidth = originWidh;
            float ratio = originWidh / originHeight;
            if (ratio == 1) {
                if (originHeight < imageMinHeight || originHeight > imageMinHeight) {
                    modifyHeight = imageMinHeight;
                    if (imageMinHeight < imageMaxWidth) {
                        modifyWidth = imageMinHeight;
                    }
                }
            } else if (originHeight > imageMinHeight) {
                modifyHeight = imageMinHeight;
                modifyWidth = (imageMinHeight * originWidh) / originHeight;
                if (modifyWidth > imageMaxWidth) {
                    modifyHeight = (imageMaxWidth * imageMinHeight) / modifyWidth;
                }
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) modifyWidth, (int) modifyHeight, false);
            ivBackground.setImageBitmap(scaledBitmap);

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(TAG, "Failed bitmap");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d(TAG, "Load Bitmap");
        }
    };

    Target feviconTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            ivFevicon.setImageBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(TAG, "Bitmap failed");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d(TAG, "On Prepare load :");
        }
    };

    public float dpToPx(int dp) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mImageSavedReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mImageSavedReceiver, new IntentFilter(ACTION_IMAGE_SAVED));
        super.onResume();

    }

    private BroadcastReceiver mImageSavedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MessageObject msgObj = (MessageObject) intent.getSerializableExtra("ImageSaved");
            Log.d(TAG, "Domain name is:" + msgObj.getDomainName() + ":File saved at:" + msgObj.getDomainSnap());
            if (!TextUtils.isEmpty(msgObj.getFevicon())) {
                ivFevicon.setTag(feviconTarget);
                Picasso.with(MainActivity.this)
                        .load(msgObj.getFevicon())
                        .into(feviconTarget);
            }
            ivBackground.setTag(target1);
            File file = new File(msgObj.domainSnap);
            if (file.exists()) {
                Picasso.with(MainActivity.this)
                        .load(file)
                        .into(target1);
            }
            tvTitle.setText(msgObj.getTitleDescription());
            tvDescription.setText(msgObj.getSubTitleDescription());
            tvUrlTitle.setText(msgObj.getDomainName());
        }
    };

    public class Urlparser extends AsyncTask<String, Void, String> {

        String title = "";
        String subTitle = "";
        String fevicon = "";
        String domainName = "";
        String image = "";
        MessageObject messageObject;
        URL url;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            messageObject = new MessageObject();
        }

        @Override
        protected String doInBackground(String... params) {
            domainName = params[0];
            messageObject.setDomainName(domainName);
            Log.d(TAG, "Url is valid as per Utility : " + URLUtil.isValidUrl(domainName));
            if (!URLUtil.isValidUrl(domainName)) {
                if (!domainName.startsWith("www")) {
                    domainName = "www." + domainName;
                }
                if (!domainName.startsWith("http")) {
                    domainName = "http://" + domainName;
                }
            }
            try {
                url = new URL(domainName.toLowerCase(Locale.US));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                url = null;
            }
            parseUrl(domainName);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (TextUtils.isEmpty(image)) {
//                webView.loadUrl(domainName);
                messageObject.setFevicon(fevicon);
                messageObject.setTitleDescription(title);
                messageObject.setSubTitleDescription(subTitle);
                Intent intent = new Intent(MainActivity.this, ScreenshotService.class);
                intent.putExtra("messageObject", messageObject);
                intent.putExtra("URL", domainName);
                intent.putExtra("Width", width);
                intent.putExtra("Height", 400);
                File file = getExternalFilesDir(null);
                File imageSaveAs = new File(file, Uri.encode(domainName) + ".png");
                intent.putExtra("Path", "" + imageSaveAs.getPath());
                startService(intent);
            } else {
                if (!TextUtils.isEmpty(fevicon)) {
                    ivFevicon.setTag(feviconTarget);
                    Picasso.with(MainActivity.this)
                            .load(fevicon)
                            .into(feviconTarget);
                }
                ivBackground.setTag(target1);
                Picasso.with(MainActivity.this)
                        .load(image)
                        .into(target1);
                tvTitle.setText(title);
                tvDescription.setText(subTitle);
                tvUrlTitle.setText(domainName);
            }
        }

        public void parseUrl(String urlToCheck) {
            if (!TextUtils.isEmpty(urlToCheck)) {

                if (Patterns.WEB_URL.matcher(urlToCheck).matches()) {
                    Log.d(TAG, "Look like a web link");
                    try {
                        Connection.Response response =
                                Jsoup.connect(urlToCheck)
                                        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1")
                                        .referrer("http://www.google.com")
                                        .timeout(10000)
                                        .followRedirects(true)
                                        .execute();
                        Document document = response.parse();
                        title = document.title();
                        Log.d(TAG, "Title of page is:" + title);
                        Elements elements = document.select("link");
                        fevicon = "";
                        for (Element element : elements) {
                            String relValue = element.attr("rel");
                            if (!TextUtils.isEmpty(relValue)) {
                                if (relValue.equals("shortcut icon") || relValue.equals("icon")) {
                                    fevicon = element.attr("href");
                                    if (fevicon.startsWith("//")) {
                                        fevicon = "http:" + fevicon;
                                    } else if (fevicon.startsWith("/")) {
                                        fevicon = domainName + fevicon;
                                    }
                                    Log.d(TAG, "Fevicon icon url is :" + fevicon);
                                    break;
                                }
                            }
                        }
                        if (TextUtils.isEmpty(fevicon)) {
                            if (url != null) {
                                fevicon = url.getProtocol() + "://" + url.getAuthority() + "/favicon.ico";
                            }
                            Log.d(TAG, "no fevicon found and we are getting from :" + fevicon);
                        }
                        Elements elements1 = document.select("meta");
                        if (elements1.size() > 0) {
                            for (Element elements2 : elements1) {
                                String property = elements2.attr("property");
                                String name = elements2.attr("name");
                                if (!TextUtils.isEmpty(property)) {
                                    if (property.equals("og:image")) {
                                        image = elements2.attr("content");
                                        Log.d(TAG, "og:image:" + image);
                                    } else if (property.equals("og:type")) {
                                        String strType = elements2.attr("content");
                                        Log.d(TAG, "og:type:" + strType);
                                    } else if (property.equals("og:title")) {
                                        if (!TextUtils.isEmpty(elements2.attr("content"))) {
                                            title = elements2.attr("content");
                                            Log.d(TAG, "og:title:" + title);
                                        }
                                    }
                                }
                                if (!TextUtils.isEmpty(name)) {
                                    if (name.equals("Description") || name.equals("description")) {
                                        subTitle = elements2.attr("content");
                                        Log.d(TAG, "Description is :" + subTitle);
                                    }
                                }
                            }
                        } else {
                            image = "";
                            subTitle = "";
                        }
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "Socket Time out : url is not exist");
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        Log.d(TAG, "Malformed url");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        Log.d(TAG, "illegal url :");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
