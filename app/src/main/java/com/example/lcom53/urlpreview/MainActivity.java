package com.example.lcom53.urlpreview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    EditText etUrl;
    TextView tvSend;
    private String TAG = MainActivity.class.getSimpleName();
    WebView webView;
    Timer timer = new Timer();
    ImageView ivBackground, ivFevicon;
    TextView tvUrlTitle, tvDescription;
    private TextView tvTitle;
    RelativeLayout rlDemo;
    Point size;
    int width = 0, height = 0;
    private static final String ACTION_IMAGE_SAVED = "imageSaved";

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
        webView = (WebView) findViewById(R.id.lastResource);
        ivBackground = (ImageView) findViewById(R.id.ivBackground);
        ivFevicon = (ImageView) findViewById(R.id.ivFevicon);
        tvUrlTitle = (TextView) findViewById(R.id.tvUrlTitle);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
//        rlDemo.bringChildToFront(ivBackground);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        timer = new Timer();
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "Page started :" + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
//                getScreen(view);
                timer.cancel();
                timer = new Timer();
                timer.schedule(new timerTask(), 2000);
                super.onPageFinished(view, url);
//                getScreen(view);
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {

//                }
//            else {
//                    try {
//                        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
//                        Canvas canvas = new Canvas(bitmap);
//                        view.draw(canvas);
//                        webView.measure(View.MeasureSpec.makeMeasureSpec(
//                                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
//                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//                        webView.layout(0, 0, webView.getMeasuredWidth(),
//                                webView.getMeasuredHeight());
//                        webView.setDrawingCacheEnabled(true);
//                        webView.buildDrawingCache();
//                        Bitmap bm = Bitmap.createBitmap(webView.getMeasuredWidth(),
//                                webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
//
//                        Canvas bigcanvas = new Canvas(bm);
//                        Paint paint = new Paint();
//                        int iHeight = bm.getHeight();
//                        bigcanvas.drawBitmap(bm, 0, iHeight, paint);
//                        webView.draw(bigcanvas);
//                        System.out.println("1111111111111111111111="
//                                + bigcanvas.getWidth());
//                        System.out.println("22222222222222222222222="
//                                + bigcanvas.getHeight());
//
//                        FileOutputStream fos = null;
//                        File file = getExternalFilesDir(null);
//                        File imageSaveAs = new File(file, Uri.encode(url) + ".png");
//                        Log.d(TAG, "File will be save as :" + imageSaveAs);
//                        try {
//                            fos = new FileOutputStream(imageSaveAs);
//                            if (fos != null) {
//                                bm.compress(Bitmap.CompressFormat.PNG, 90, fos);
//                                fos.close();
//                            }
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        });
//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                super.onProgressChanged(view, newProgress);
//                if (newProgress == 100) {
//                    Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
//                    Canvas canvas = new Canvas(bitmap);
//                    webView.draw(canvas);
//                    FileOutputStream fos = null;
//                    File file = getExternalFilesDir(null);
//                    File imageSaveAs = new File(file, Uri.encode(view.getOriginalUrl()) + ".png");
//                    Log.d(TAG, "File will be save as :" + imageSaveAs);
//                    try {
//                        fos = new FileOutputStream(imageSaveAs);
//                        if (fos != null) {
//                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
//                            fos.close();
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
    }

    private void getScreen(View content) {
        content.setDrawingCacheEnabled(true);

        // this is the important code :)
        // Without it the view will have a dimension of 0,0 and the bitmap will be null
        content.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());

        content.buildDrawingCache(true);
        Bitmap bitmap = content.getDrawingCache();
        File file = new File("/sdcard/test.png");
        try {
            file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        content.setDrawingCacheEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class timerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    getScreen(webView);
                    Picture picture = webView.capturePicture();
                    Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    picture.draw(canvas);
                    FileOutputStream fos = null;
                    File file = getExternalFilesDir(null);
                    File imageSaveAs = new File(file, String.valueOf(System.currentTimeMillis()) + ".png");
                    Log.d(TAG, "File will be save as :" + imageSaveAs);
                    try {
                        fos = new FileOutputStream(imageSaveAs);
                        if (fos != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                            fos.close();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public class Urlparser extends AsyncTask<String, Void, String> {

        String title = "";
        String subTitle = "";
        String fevicon = "";
        String domainName = "";
        String image = "";

        @Override
        protected String doInBackground(String... params) {
            domainName = params[0];
            Log.d(TAG, "Url is valid as per Utility : " + URLUtil.isValidUrl(domainName));
            if (!URLUtil.isValidUrl(domainName)) {
                if (!domainName.startsWith("www")) {
                    domainName = "www." + domainName;
                }
                if (!domainName.startsWith("http")) {
                    domainName = "http://" + domainName;
                }
            }
            parseUrl(domainName);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (TextUtils.isEmpty(image)) {
//                webView.loadUrl(domainName);
                Intent intent = new Intent(MainActivity.this, ScreenshotService.class);
                intent.putExtra("URL", domainName);
                intent.putExtra("Width", width);
                intent.putExtra("Height", 400);
                File file = getExternalFilesDir(null);
                File imageSaveAs = new File(file, Uri.encode(domainName) + ".png");
                intent.putExtra("Path", "" + imageSaveAs.getPath());
                startService(intent);
            } else {
                if (!TextUtils.isEmpty(fevicon)) {
                    Picasso.with(MainActivity.this)
                            .load(fevicon)
                            .into(ivFevicon);
                }
                Picasso.with(MainActivity.this)
                        .load(image)
                        .into(ivBackground);
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
                                    Log.d(TAG, "Fevicon icon url is :" + fevicon);
                                    if (fevicon.startsWith("//")) {
                                        fevicon = domainName + fevicon;
                                    }
                                    break;
                                }
                            }
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
                                        subTitle = elements2.attr("content");
                                        Log.d(TAG, "og:title:" + subTitle);
                                    }
                                }
                                if (!TextUtils.isEmpty(name)) {
                                    if (name.equals("Description") || name.equals("description")) {
                                        String description = elements2.attr("content");
                                        Log.d(TAG, "Description is :" + description);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
