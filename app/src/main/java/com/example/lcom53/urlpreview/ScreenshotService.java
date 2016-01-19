package com.example.lcom53.urlpreview;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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
            Bitmap b = webview.getDrawingCache();
            File file = new File(PathToSave);
            OutputStream out;
            try {
                out = new BufferedOutputStream(new FileOutputStream(file));
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Log.d("ScreenshotService", "File save @:" + file.getAbsolutePath());
                Intent intent = new Intent(MainActivity.ACTION_IMAGE_SAVED);
                messageObject.setDomainSnap(file.getAbsolutePath());
                intent.putExtra("ImageSaved", messageObject);
                LocalBroadcastManager.getInstance(ScreenshotService.this).sendBroadcast(intent);
            } catch (IOException e) {
                Log.e("ScreenshotService", "IOException while trying to save thumbnail, Is /sdcard/ writable?");

                e.printStackTrace();
            }
            Log.d("ScreenshotService", "Screenshot taken");
            return null;
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
        return null;
    }

    @Override
    public void onDestroy() {

    }


}
