package com.example.lcom53.urlpreview;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.lcom53.urlpreview.workers.UrlPreviewMainWorker;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements UrlPreviewMainWorker.Callback, DownloadSnaps {

    EditText etUrl;
    TextView tvSend;
    String TAG = MainActivity.class.getSimpleName();
    ImageView ivBackground, ivFevicon;
    TextView tvUrlTitle, tvDescription;
    TextView tvTitle;
    RelativeLayout rlDemo;
    Point size;
    int width = 0, height = 0;
    public static final String ACTION_IMAGE_SAVED = "imageSaved";
    public static int imageMinHeight = 0, imageMaxWidth = 0;

    boolean bgLoaded = false, feviconLoaded = false;
    MessageObject messageObject;
    RecyclerView rvlist;
    MyRVAdapter adapter;
    ArrayList<MessageObject> messageObjectArrayList;
    protected ImageLoader imageLoader = ImageLoader.getInstance();
    protected ImageLoaderConfiguration config;
    protected File customCacheDirectory;
    protected DisplayImageOptions options;
    public static final int LEFT_SIDE = 0;
    public static final int RIGHT_SIDE = 1;
    private UrlPreviewMainWorker mWorkerThread;
    Random random;
    ArrayList<MessageObject> queueForSnapShot = new ArrayList<>();

    @Override
    public void onRequestDownload(int positionForDownload, String URL, String originMsg) {
        MessageObject messageObject = new MessageObject();
        messageObject.setOriginalMsg(originMsg);
        messageObject.setDomainName(URL);
        messageObject.setWidth(width);
        messageObject.setHeight(400);
        messageObject.setPosition(positionForDownload);
        Log.d(TAG, "We are sending request :" + messageObject.toString());
        mWorkerThread.queueTask(URL, random.nextInt(2), messageObject);
    }

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
        random = new Random();
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
                if (!TextUtils.isEmpty(etUrl.getText().toString())) {
                    MessageObject messageObject = new MessageObject();
                    messageObject.setOriginalMsg(etUrl.getText().toString());
                    messageObjectArrayList.add(messageObject);
                    adapter.notifyItemInserted(messageObjectArrayList.size() - 1);
                    etUrl.setText("");
                }
            }
        });
        rlDemo = (RelativeLayout) findViewById(R.id.rlDemo);
        ivBackground = (ImageView) findViewById(R.id.ivBackground);
        ivFevicon = (ImageView) findViewById(R.id.ivFevicon);
        tvUrlTitle = (TextView) findViewById(R.id.tvUrlTitle);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        rvlist = (RecyclerView) findViewById(R.id.rvlist);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        messageObjectArrayList = new ArrayList<>();
        adapter = new MyRVAdapter(this, messageObjectArrayList);
        adapter.setDownloadSnaps(this);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                Log.d(TAG, "Adapter changed");
                super.onChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                Log.d(TAG, "Item changed :" + positionStart + ":itemcount:" + itemCount);
                super.onItemRangeChanged(positionStart, itemCount, payload);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                Log.d(TAG, "Item inserted :" + positionStart + ":" + itemCount);
                super.onItemRangeInserted(positionStart, itemCount);
            }
        });
        rvlist.setAdapter(adapter);
        rvlist.setLayoutManager(linearLayoutManager);
        configureUIL();
        mWorkerThread = new UrlPreviewMainWorker("myWorkerThread", new Handler(), this);
        mWorkerThread.start();
        mWorkerThread.prepareHandler();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            if (screenshotService != null) {
                screenshotService.setCallbacks(null); // unregister
                unbindService(serviceConnection);
                bound = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mWorkerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mWorkerThread.quitSafely();
            } else {
                mWorkerThread.quit();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onImageDownloaded(MessageObject messageObject2, Bitmap bitmapFevicon, Bitmap bitmapBackground) {
        if (TextUtils.isEmpty(messageObject2.getDomainSnap())) {
//                webView.loadUrl(domainName);
            if (queueForSnapShot.size() > 0) {
                Log.d(TAG, "Wait for queue Mr. " + messageObject2.getDomainName() + ":" + messageObject2.getmSequence());
                queueForSnapShot.add(messageObject2);
            } else {
                Log.d(TAG, "No traffic ahead and you are ready to go Mr. " + messageObject2.getDomainName() + ":" + messageObject2.getmSequence());
                Intent intent = new Intent(MainActivity.this, ScreenshotService.class);
                intent.putExtra("messageObject", messageObject2);
                intent.putExtra("URL", messageObject2.domainName);
                intent.putExtra("Width", width);
                intent.putExtra("Height", 400);
                File file = getExternalFilesDir(null);
                File imageSaveAs = new File(file, Uri.encode(messageObject2.getDomainName() + "_bg") + ".png");
                intent.putExtra("Path", "" + imageSaveAs.getPath());
                Log.d(TAG, "In Image Download going for snap shot service:" + messageObject2.toString());
                startService(intent);
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        } else {
            Log.d(TAG, "In Image Download going for rvsnap:" + messageObject2.toString());
            if (bitmapFevicon != null) {
                ivFevicon.setImageBitmap(bitmapFevicon);
                ivFevicon.setVisibility(View.VISIBLE);
            } else {
                ivFevicon.setVisibility(View.INVISIBLE);
                ivFevicon.setImageDrawable(null);
            }
            if (bitmapBackground != null)
                ivBackground.setImageBitmap(bitmapBackground);
            else
                ivBackground.setImageBitmap(null);
            tvTitle.setText(messageObject2.getTitleDescription());
            tvDescription.setText(messageObject2.getSubTitleDescription());
            tvUrlTitle.setText(messageObject2.getDomainName());
            getRVSnapNoCheck(messageObject2);
            if (queueForSnapShot.contains(messageObject2.getmSequence())) {
                queueForSnapShot.remove(messageObject2.getmSequence());
            }
            if (queueForSnapShot.size() > 0) {
                Log.d(TAG, "We have queue for Snaps. Let's take snap");
                MessageObject mSnapObject = queueForSnapShot.get(0);
                Intent intent = new Intent(MainActivity.this, ScreenshotService.class);
                intent.putExtra("messageObject", mSnapObject);
                intent.putExtra("URL", mSnapObject.domainName);
                intent.putExtra("Width", width);
                intent.putExtra("Height", 400);
                File file = getExternalFilesDir(null);
                File imageSaveAs = new File(file, Uri.encode(mSnapObject.getDomainName() + "_bg") + ".png");
                intent.putExtra("Path", "" + imageSaveAs.getPath());
                startService(intent);
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Log.d(TAG, "No more work. No one in queue found");
            }
        }
    }

    ScreenshotService screenshotService;
    boolean bound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ScreenshotService.LocalBinder binder = (ScreenshotService.LocalBinder) service;
            screenshotService = binder.getService();
            bound = true;
            screenshotService.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    public class MyRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        LayoutInflater layoutInflater;
        ArrayList<MessageObject> mCurrentuser;
        String msgToDisplay;
        Context context;
        private DownloadSnaps downloadSnaps;

        public MyRVAdapter(Context context, ArrayList<MessageObject> currentUser) {
            layoutInflater = LayoutInflater.from(context);
            mCurrentuser = currentUser;
            this.context = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = layoutInflater.inflate(R.layout.row_chat_item, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
            ChatViewHolder viewHolder = (ChatViewHolder) vh;
            MessageObject chatItem = mCurrentuser.get(position);
            msgToDisplay = chatItem.getOriginalMsg().trim();
            Log.d(TAG, "message to display" + msgToDisplay);
            if (msgToDisplay.startsWith("LoadUrl_")) {
                msgToDisplay = msgToDisplay.replace("LoadUrl_", "").trim();
            }
            viewHolder.tvUrlTitle.setText(msgToDisplay);
            ArrayList<LinkSpec> linkSpecs = gatherLinks(msgToDisplay);
            if (linkSpecs.size() > 0) {
                viewHolder.llimagecontainer.removeAllViews();
                for (int i = 0; i < linkSpecs.size(); i++) {
                    ImageView imageView = createImageView(i, viewHolder.llimagecontainer);
                    File file1 = context.getExternalFilesDir(null);
                    File file = new File(file1, Uri.encode(linkSpecs.get(i).url) + ".png");
                    if (file.exists()) {
                        viewHolder.llimagecontainer.setVisibility(View.VISIBLE);
                        ImageLoader.getInstance().displayImage("file://" + file.getAbsolutePath(), imageView);
                        Log.d(TAG, "File exists :" + file.getAbsolutePath());
                    } else {
                        viewHolder.llimagecontainer.setVisibility(View.GONE);
                        Log.d(TAG, "File not exists so lets request for download:" + file.getAbsolutePath());
                        if (downloadSnaps != null) {
                            downloadSnaps.onRequestDownload(position, linkSpecs.get(i).url, msgToDisplay);
                        }
                    }
                }
            }
        }

        public ImageView createImageView(int index, LinearLayout parent) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            imageView.setId((index * 100) + index);
            parent.addView(imageView, index);
            return imageView;
        }

        @Override
        public int getItemCount() {
            return mCurrentuser.size();
        }

        public void setDownloadSnaps(DownloadSnaps downloadSnaps) {
            this.downloadSnaps = downloadSnaps;
        }

        public class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llimagecontainer;
            TextView tvUrlTitle;

            ChatViewHolder(View itemView) {
                super(itemView);
                llimagecontainer = (LinearLayout) itemView.findViewById(R.id.llimagecontainer);
                llimagecontainer.setVisibility(View.GONE);
                tvUrlTitle = (TextView) itemView.findViewById(R.id.tvUrlTitle);
            }
        }
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

    public static final ArrayList<LinkSpec> gatherLinks(String s) {
        ArrayList<LinkSpec> links = new ArrayList<>();
        Pattern pattern = Patterns.WEB_URL;
        String[] schemes = new String[]{"http://", "https://", "rtsp://"};
        Linkify.MatchFilter matchFilter = Linkify.sUrlMatchFilter;
        Linkify.TransformFilter transformFilter = null;
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
            url = prefixes[0] + (url.startsWith("www") ? "" : "www.") + url;
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
//            getRVSnap();
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
            feviconLoaded = true;
            getRVSnap();
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
            File file = new File(file1, Uri.encode(messageObject.getDomainName()) + ".png");
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
            messageObjectArrayList.remove(messageObject.getPosition());
            messageObjectArrayList.add(messageObject.getPosition(), messageObject);
            adapter.notifyItemChanged(messageObject.getPosition());
        }
    }

    public void getRVSnapNoCheck(MessageObject messageObject1) {
        rlDemo.setDrawingCacheEnabled(true);
        rlDemo.measure(messageObject1.width, messageObject1.height);
        rlDemo.layout(0, 0, messageObject1.width, messageObject1.height);
        Bitmap b = rlDemo.getDrawingCache();
        File file1 = getExternalFilesDir(null);
        File file = new File(file1, Uri.encode(messageObject1.getDomainName()) + ".png");
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
        Log.d(TAG, "Snapshot created for No RVSnap Check:" + messageObject1.toString());
        messageObjectArrayList.remove(messageObject1.getPosition());
        messageObjectArrayList.add(messageObject1.getPosition(), messageObject1);
        adapter.notifyItemChanged(messageObject1.getPosition());
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

    private void configureUIL() {
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        config = new ImageLoaderConfiguration.Builder(this).build();
        imageLoader.init(config);
    }
}