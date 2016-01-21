package com.example.lcom53.urlpreview;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.lcom53.urlpreview.workers.UrlPreviewMainWorker;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ScreenshotService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Message msg;
    private WebView webview;
    String domainName, PathToSave;
    int width = 0, height = 0;
    MessageObject messageObject;
    private final IBinder binder = new LocalBinder();
    private UrlPreviewMainWorker.Callback serviceCallbacks;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            webview = new WebView(ScreenshotService.this);
            //without this toast message, screenshot will be blank, dont ask me why...
//            Toast.makeText(ScreenshotService.this, "Taking screenshot...", Toast.LENGTH_SHORT).show();
            // This is the important code :)
            webview.setDrawingCacheEnabled(true);
            webview.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            webview.getSettings().setJavaScriptEnabled(true);
            //width x height of your webview and the resulting screenshot
            webview.measure(width, height);
            webview.layout(0, 0, width, height);
            webview.loadUrl(domainName);
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    //without this method, your app may crash...
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    new takeScreenshotTask().execute();
                    stopSelf();
                }
            });
        }
    }

    private class takeScreenshotTask extends AsyncTask<Void, Void, Void> {
        Bitmap feviconBitmap = null, scaledBitmap = null;

        @Override
        protected Void doInBackground(Void[] p1) {

            //allow the webview to render
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
            }
            //here I save the bitmap to file
            Bitmap backgroundBitmap = webview.getDrawingCache();
            File file = new File(PathToSave);
            OutputStream out;
            try {
                out = new BufferedOutputStream(new FileOutputStream(file));
                backgroundBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Log.d("ScreenshotService", "File save @:" + file.getAbsolutePath());
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
                messageObject.setWidth((int) modifyWidth);
                messageObject.setHeight((int) modifyHeight);
                messageObject.setDomainSnap(file.getAbsolutePath());
//                Intent intent = new Intent(MainActivity.ACTION_IMAGE_SAVED);
//                intent.putExtra("ImageSaved", messageObject);
//                LocalBroadcastManager.getInstance(ScreenshotService.this).sendBroadcast(intent);
            } catch (IOException e) {
                Log.e("ScreenshotService", "IOException while trying to save thumbnail, Is /sdcard/ writable?");

                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(messageObject.getFevicon())) {
                ImageSize imageSize = new ImageSize(30, 30);
                feviconBitmap = ImageLoader.getInstance().loadImageSync(messageObject.getFevicon(), imageSize);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (serviceCallbacks != null) {
                serviceCallbacks.onImageDownloaded(messageObject, feviconBitmap, scaledBitmap);
            } else {
                Log.d("ScreenshotService", "Service call back is null");
            }
            Log.d("ScreenshotService", "Screenshot taken");
        }
    }

    //service related stuff below, its probably easyer to use intentService...
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        domainName = intent.getStringExtra("URL");
        width = intent.getIntExtra("Width", 600);
        height = intent.getIntExtra("Height", 400);
        PathToSave = intent.getStringExtra("Path");
        messageObject = (MessageObject) intent.getSerializableExtra("messageObject");
        msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return binder;
    }

    @Override
    public void onDestroy() {

    }

    public class LocalBinder extends Binder {
        ScreenshotService getService() {
            return ScreenshotService.this;
        }
    }

    public void setCallbacks(UrlPreviewMainWorker.Callback callbacks) {
        serviceCallbacks = callbacks;
    }
}
