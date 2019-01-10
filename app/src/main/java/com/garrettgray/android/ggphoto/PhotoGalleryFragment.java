package com.garrettgray.android.ggphoto;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    final static private String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecycler;
    private List<GalleryItem> mItems = new ArrayList<>();
    private static boolean isPage1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    public PhotoGalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle sis){
        super.onCreate(sis);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        updateItems();

        //PollService.setServiceAlarm(getContext(),true);

        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                photoHolder.bindGalleryItem(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gal_search,menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG,"QueryTextSubmit: " + s);

                QueryPreferences.setStoredQuery(getActivity(),s);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG,"QueryTextChange: " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });

        MenuItem mi = menu.findItem(R.id.menu_item_toggle_polling);

        if (PollService.isServiceAlarmOn(getActivity())){
            mi.setTitle(getResources().getString(R.string.start_polling));
        } else {
            mi.setTitle(getResources().getString(R.string.stop_polling));

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;

                case R.id.menu_item_toggle_polling:
                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                    getActivity().invalidateOptionsMenu();
                    return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getContext());
        new FetchItemsTask(query).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fagment_photo_gallery,container,false);

        isPage1 = true;
        mPhotoRecycler = v.findViewById(R.id.photo_recycler_view);
        mPhotoRecycler.setLayoutManager(new GridLayoutManager(getActivity(),3));
        mPhotoRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && isPage1) {
                    AlertDialog ad = new AlertDialog.Builder(getContext()).create();
                    ad.setTitle("End Of Page 1!");
                    ad.setMessage("Would You Like To View Page 2?");
                    ad.setButton(DialogInterface.BUTTON_POSITIVE, "YUP!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateItems();
                            isPage1 = false;
                            dialog.dismiss();
                        }
                    });

                    ad.setButton(DialogInterface.BUTTON_NEGATIVE, "NAW...", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isPage1 = false;

                            dialog.dismiss();
                        }
                    });

                    ad.show();

                }
            }
        });
        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background thread destroyed!");
    }

    private void setupAdapter(){
        if (isAdded()){
            mPhotoRecycler.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);

            mItemImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bindGalleryItem(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @NonNull @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup,false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.drawable.black_fist_africa);

            photoHolder.bindGalleryItem(placeHolder);

            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... pageNum) {
            //String query = QueryPreferences.getStoredQuery(getContext());

            if (mQuery == null){
                return new FlickrFetcher().fetchRecentPhotos();
            } else {
                return new FlickrFetcher().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems = items;
            setupAdapter();
        }
    }

}