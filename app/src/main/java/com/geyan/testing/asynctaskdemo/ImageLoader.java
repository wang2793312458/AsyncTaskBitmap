package com.geyan.testing.asynctaskdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by geyan on 2016/7/25.
 */
public class ImageLoader {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), sThreadFactory);

    private static final int MESSAGE_POST_RESULT = 1;
    private final LruCache<String, Bitmap> mMemoryCache;
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            //对象开始传入主线程中，开始处理
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            String url = (String) imageView.getTag();
            if (url.equals(result.url)) {
                imageView.setImageBitmap(result.bitmap);
            } else {
                Log.d("gy", "when set image bitmap, but url has changed!");
            }
        }
    };

    /**
     * 在构造函数中创建LruCache，一般创建完该对象后，应该实现该对象的添加和查找逻辑。
     */
    public ImageLoader() {
        //可使用的最大内存，转换成KB
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 这边我考虑使用线程池，而不是使用线程的方法
     *
     * @param imageView
     * @param urlString
     */
    public void showImageByThreads(final ImageView imageView, final String urlString) {

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                imageView.setTag(urlString);
                Bitmap bitmap = loadBitmap(urlString);
                if (bitmap != null) {
                    //将对应的imageView,url,bitmap封装成一个对象，然后将对象传入Handler
                    LoaderResult result = new LoaderResult(imageView, urlString, bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();

                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /**
     * 这里我使用AsyncTask来异步加载图片
     *
     * @param imageView
     * @param urlString
     */
    public void showImagebyAsyncTask(ImageView imageView, String urlString) {

        new NewsBitmapAsyncTask(imageView, urlString).execute(urlString);
    }

    class NewsBitmapAsyncTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView mImageView;
        private String mUrl;

        public NewsBitmapAsyncTask(ImageView imageView, String url) {
            this.mImageView = imageView;
            this.mUrl = url;
            mImageView.setTag(mUrl);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return loadBitmap(params[0]);
        }

        //在主线程中操作，设置图片
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (mUrl.equals(mImageView.getTag())) {
                mImageView.setImageBitmap(bitmap);
            }
        }
    }


    /**
     * 加载图片，先不考虑磁盘，我估计手机有问题，因为我前面使用我封装好的ImageLoader，说是找不到file
     * 1.从内存中加载
     * 2.内存中若是没有，则从网上加载
     *
     * @param urlString
     * @return
     */
    private Bitmap loadBitmap(String urlString) {
        Bitmap bitmap = loadBitmapFromMemoryCache(urlString);
        if (bitmap != null) {
            Log.d("gy", "loadBitmapFromMemoryCache,url:" + urlString);
            return bitmap;
        }
        bitmap = loadBitmapFromHttp(urlString);
        return bitmap;
    }

    //从网络中加载
    //主要的操作还是Java IO流，源：urlString，目的：Bitmap
    private Bitmap loadBitmapFromHttp(String urlString) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI Thread.");
        }
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            BufferedInputStream bufIn = new BufferedInputStream(urlConnection.getInputStream());
            Bitmap bitmap = BitmapFactory.decodeStream(bufIn);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //从内存中加载，使用LruCache，先将urlString做MD5编码，因为url中总是含有特殊字符
    private Bitmap loadBitmapFromMemoryCache(String urlString) {
        String key = hashKeyFromUrl(urlString);
        return getBitmapFromMemoryCache(key);
    }

    //url做MD5编码，转化成key
    private String hashKeyFromUrl(String urlString) {
        String cacheKey;
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(urlString.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(urlString.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    class LoaderResult {
        private ImageView imageView;
        private String url;
        private Bitmap bitmap;

        public LoaderResult(ImageView imageView, String url, Bitmap bitmap) {
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }
}
