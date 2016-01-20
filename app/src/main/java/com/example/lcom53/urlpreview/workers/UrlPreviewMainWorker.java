package com.example.lcom53.urlpreview.workers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.example.lcom53.urlpreview.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ParthS
 * @since 20/1/16.
 */
public class UrlPreviewMainWorker extends HandlerThread {

    private Handler mWorkerHandler;
    private Handler mResponseHandler;
    private Map<ImageView, String> mRequestMap = new HashMap<ImageView, String>();
    private Callback mCallback;
    private static final String TAG = UrlPreviewMainWorker.class.getSimpleName();

    public interface Callback {
        public void onImageDownloaded(ImageView imageView, Bitmap bitmap, int side);
    }

    public UrlPreviewMainWorker(String TAG, Handler responseHandler, Callback callback) {
        super(TAG);
        mResponseHandler = responseHandler;
        mCallback = callback;
    }

    public void queueTask(String url, int side, ImageView imageView) {
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
                ImageView imageView = (ImageView) msg.obj;
                String side = msg.what == MainActivity.LEFT_SIDE ? "left side" : "right side";
                Log.i(TAG, String.format("Processing %s, %s", mRequestMap.get(imageView), side));
                handleRequest(imageView, msg.what);
                try {
                    msg.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private void handleRequest(final ImageView imageView, final int side) {
        String url = mRequestMap.get(imageView);
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(url).openConnection();
            final Bitmap bitmap = BitmapFactory
                    .decodeStream((InputStream) connection.getContent());
            mRequestMap.remove(imageView);
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onImageDownloaded(imageView, bitmap, side);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void postTask(Runnable task) {
        mWorkerHandler.post(task);
    }
}
