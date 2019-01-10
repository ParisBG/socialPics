package com.garrettgray.android.ggphoto;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbDownListener;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener listener){
        mThumbDownListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if (msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;

                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));

                    handleRequest(target);

                }
            }

        };
    }

    private void handleRequest(final T target){
        try {
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap Created");

            mResponseHandler.post(new Runnable(){
                @Override
                public void run() {
                    if (!mRequestMap.get(target).equals(url) || mHasQuit ||mRequestMap.get(target) == null){
                        return;
                    }

                    mRequestMap.remove(target);

                    mThumbDownListener.onThumbnailDownloaded(target,bitmap);
                }
            });

        }catch(IOException ioe){
            Log.e(TAG,"Errpr downloading image",ioe);
        }
    }

    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    @Override
    public boolean quit(){
        mHasQuit = true;
        return super.quit();

    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG,"Got a URL: " + url);

        if (url == null){
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target,url);
        }

        mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
    }
}
