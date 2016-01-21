package com.example.lcom53.urlpreview.workers;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.example.lcom53.urlpreview.MainActivity;
import com.example.lcom53.urlpreview.MessageObject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by lcom17 on 21/1/16.
 */
public class UrlScreenShotWorker extends HandlerThread {

    private Handler mWorkerHandler;
    private Handler mResponseHandler;
    private WeakHashMap<MessageObject, String> mRequestMap = new WeakHashMap<MessageObject, String>();
    private UrlPreviewMainWorker.Callback mCallback;
    private static final String TAG = UrlPreviewMainWorker.class.getSimpleName();
    String title = "";
    String subTitle = "";
    String fevicon = "";
    String domainName = "";
    String image = "";
    URL url;
    String mFevicon = "";

    public UrlScreenShotWorker(String TAG, Handler responseHandler, UrlPreviewMainWorker.Callback callback) {
        super(TAG);
        mResponseHandler = responseHandler;
        mCallback = callback;
    }

    public void queueTask(String url, int side, MessageObject imageView) {
        mRequestMap.put(imageView, url);
        Log.i(TAG, url + " added to the queue");
        mWorkerHandler.obtainMessage(side, imageView)
                .sendToTarget();
    }

    public void prepareHandler() {
        mWorkerHandler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MessageObject imageView = (MessageObject) msg.obj;
                Log.i(TAG, String.format("Processing %s", mRequestMap.get(imageView)));
                handleRequest(imageView);
                return true;
            }
        });
    }

    private void handleRequest(final MessageObject imageView) {
        String urlToCheck = imageView.getDomainName();
        domainName = urlToCheck;
        try {
            url = new URL(domainName.toLowerCase(Locale.US));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            url = null;
        }
        Bitmap feviconBitmap = null;
        Bitmap backgroundBitmap = null;
        if (!TextUtils.isEmpty(urlToCheck)) {
            if (Patterns.WEB_URL.matcher(urlToCheck).matches()) {
                Log.d(TAG, "Look like a web link");
                try {
                    Connection.Response response =
                            Jsoup.connect(urlToCheck)
                                    .userAgent("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30")
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
                        if (relValue.equals("shortcut icon") || relValue.equals("icon")) {
                            if (!TextUtils.isEmpty(relValue)) {
                                if (relValue.equals("icon")) {
                                    mFevicon = element.attr("href");
                                } else if (relValue.equals("shortcut icon")) {
                                    mFevicon = element.attr("href");
                                }
                                if (TextUtils.isEmpty(fevicon)) {
                                    fevicon = mFevicon;
                                } else {
                                    if (!mFevicon.contains(".svg")) {
                                        fevicon = mFevicon;
                                    }
                                }
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
        imageView.setFevicon(fevicon);
        imageView.setTitleDescription(title);
        imageView.setSubTitleDescription(subTitle);
        imageView.setDomainSnap(image);
        if (!TextUtils.isEmpty(fevicon)) {
            ImageSize imageSize = new ImageSize(30, 30);
            feviconBitmap = ImageLoader.getInstance().loadImageSync(fevicon, imageSize);
        }
        Bitmap scaledBitmap = null;
        if (!TextUtils.isEmpty(image)) {
            ImageSize imageSize = new ImageSize(imageView.getWidth(), imageView.getHeight());
            backgroundBitmap = ImageLoader.getInstance().loadImageSync(image, imageSize);
            float originHeight = backgroundBitmap.getHeight();
            float originWidh = backgroundBitmap.getWidth();
            float modifyHeight = originHeight;
            float modifyWidth = originWidh;
            float ratio = originWidh / originHeight;
            if (ratio == 1) {
                if (originHeight < MainActivity.imageMinHeight || originHeight > MainActivity.imageMinHeight) {
                    modifyHeight = MainActivity.imageMinHeight;
                    if (MainActivity.imageMinHeight < MainActivity.imageMaxWidth) {
                        modifyWidth = MainActivity.imageMinHeight;
                    }
                }
            } else if (originHeight > MainActivity.imageMinHeight) {
                modifyHeight = MainActivity.imageMinHeight;
                modifyWidth = (MainActivity.imageMinHeight * originWidh) / originHeight;
                if (modifyWidth > MainActivity.imageMaxWidth) {
                    modifyHeight = (MainActivity.imageMaxWidth * MainActivity.imageMinHeight) / modifyWidth;
                }
            }
            scaledBitmap = Bitmap.createScaledBitmap(backgroundBitmap, (int) modifyWidth, (int) modifyHeight, false);
            imageView.setWidth((int) modifyWidth);
            imageView.setHeight((int) modifyHeight);
        }
        mRequestMap.remove(imageView);
        final Bitmap finalFeviconBitmap = feviconBitmap;
        final Bitmap finalbackgroundBitmap = scaledBitmap;
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onImageDownloaded(imageView, finalFeviconBitmap, finalbackgroundBitmap);
            }
        });
    }

    public void postTask(Runnable task) {
        mWorkerHandler.post(task);
    }

}
