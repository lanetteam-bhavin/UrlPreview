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
import android.text.Spannable;
import android.text.TextUtils;
import android.text.util.Linkify;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    boolean bgLoaded = false, feviconLoaded = false;
    MessageObject messageObject;

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
                ArrayList<LinkSpec> links = new ArrayList<LinkSpec>();
                gatherLinks(links, etUrl.getText().toString(), Patterns.WEB_URL,
                        new String[]{"http://", "https://", "rtsp://"},
                        Linkify.sUrlMatchFilter, null);
//                String urlToCheck = etUrl.getText().toString();
//                new Urlparser().execute(urlToCheck);

            }
        });
        rlDemo = (RelativeLayout) findViewById(R.id.rlDemo);
        ivBackground = (ImageView) findViewById(R.id.ivBackground);
        ivFevicon = (ImageView) findViewById(R.id.ivFevicon);
        tvUrlTitle = (TextView) findViewById(R.id.tvUrlTitle);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
    }

    public static class LinkSpec {
        String url;
        int start;
        int end;
    }

    private static final ArrayList<LinkSpec> gatherLinks(ArrayList<LinkSpec> links,
                                                         String s, Pattern pattern, String[] schemes,
                                                         Linkify.MatchFilter matchFilter, Linkify.TransformFilter transformFilter) {
        Matcher m = pattern.matcher(s);

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
                LinkSpec spec = new LinkSpec();
                String url = makeUrl(m.group(0), schemes, m, transformFilter);
                spec.url = url;
                spec.start = start;
                spec.end = end;
                links.add(spec);
                Log.d("TAG", "Link found :" + spec.url + ":" + spec.start + ":" + spec.end);
            }
        }
        return links;
    }

    private static final String makeUrl(String url, String[] prefixes,
                                        Matcher m, Linkify.TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(m, url);
        }

        boolean hasPrefix = false;

        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0,
                    prefixes[i].length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefixes[i], 0,
                        prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }

                break;
            }
        }

        if (!hasPrefix) {
            url = prefixes[0] + url;
        }

        return url;
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
            messageObject.setHeight((int) modifyHeight);
            messageObject.setWidth((int) modifyWidth);
            bgLoaded = true;
            getRVSnap();
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
            feviconLoaded = true;
            getRVSnap();
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

    public void getRVSnap() {
        if (bgLoaded && feviconLoaded) {
            rlDemo.setDrawingCacheEnabled(true);
            rlDemo.measure(messageObject.width, messageObject.height);
            rlDemo.layout(0, 0, messageObject.width, messageObject.height);
            Bitmap b = rlDemo.getDrawingCache();
            File file1 = getExternalFilesDir(null);
            File file = new File(file1, Uri.encode(messageObject.getDomainName()));
            OutputStream out;
            try {
                out = new BufferedOutputStream(new FileOutputStream(file));
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Log.d("ScreenshotService", "File save @:" + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e("ScreenshotService", "IOException while trying to save thumbnail, Is /sdcard/ writable?");
                e.printStackTrace();
            }
            bgLoaded = false;
            feviconLoaded = false;
        }
    }

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
            messageObject = (MessageObject) intent.getSerializableExtra("ImageSaved");
            Log.d(TAG, "Domain name is:" + messageObject.getDomainName() + ":File saved at:" + messageObject.getDomainSnap());
            if (!TextUtils.isEmpty(messageObject.getFevicon())) {
                ivFevicon.setTag(feviconTarget);
                Picasso.with(MainActivity.this)
                        .load(messageObject.getFevicon())
                        .into(feviconTarget);
            }
            ivBackground.setTag(target1);
            File file = new File(messageObject.domainSnap);
            if (file.exists()) {
                Picasso.with(MainActivity.this)
                        .load(file)
                        .into(target1);
            }
            tvTitle.setText(messageObject.getTitleDescription());
            tvDescription.setText(messageObject.getSubTitleDescription());
            tvUrlTitle.setText(messageObject.getDomainName());
        }
    };

    public class Urlparser extends AsyncTask<String, Void, String> {

        String title = "";
        String subTitle = "";
        String fevicon = "";
        String domainName = "";
        String image = "";

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
            messageObject.setFevicon(fevicon);
            messageObject.setTitleDescription(title);
            messageObject.setSubTitleDescription(subTitle);
            if (TextUtils.isEmpty(image)) {
//                webView.loadUrl(domainName);
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
                                    try {
                                        URL url1 = new URL(fevicon);
                                        Log.d(TAG, "Url is:" + url1.getAuthority() + ":" + url1.getAuthority() + ":" + url1.getPath());
                                    } catch (MalformedURLException e) {
                                        Log.d(TAG, " :" + e.getMessage());
                                        if (url != null) {
                                            fevicon = url.getProtocol() + "://" + url.getAuthority() + (fevicon.startsWith("/") ? "" : "/") + fevicon;
                                        }
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
