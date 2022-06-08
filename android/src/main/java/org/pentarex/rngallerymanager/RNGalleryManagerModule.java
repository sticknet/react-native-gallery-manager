/**
 * Created by pentarex on 26.01.18.
 */

package org.pentarex.rngallerymanager;

import android.database.Cursor;

import android.net.Uri;
import android.os.Build;

import android.provider.MediaStore;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;


public class RNGalleryManagerModule extends ReactContextBaseJavaModule {

    public static final String RNGALLERY_MANAGER = "RNGalleryManager";
    private static ReactApplicationContext reactContext = null;


    public RNGalleryManagerModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public String getName() {
        return RNGALLERY_MANAGER;
    }


    @ReactMethod
    public void getAssets(final ReadableMap params, final Promise promise) {
        if (isJellyBeanOrLater()) {
            promise.reject(new Exception("Version of Android must be > JellyBean"));
            return;
        }

        String requestedType = "all";
        if (params.hasKey("type")) {
            requestedType = params.getString("type");
        }

        Integer limit = 10;
        if (params.hasKey("limit")) {
            limit = params.getInt("limit");
        }
        Integer startFrom = 0;
        if (params.hasKey("startFrom")) {
            startFrom = params.getInt("startFrom");
        }
        String albumName = null;
        if (params.hasKey("albumName")) {
            albumName = params.getString("albumName");
        }

        WritableMap response = new WritableNativeMap();

        Cursor gallery = null;
        try {
            gallery = GalleryCursorManager.getAssetCursor(requestedType, albumName, reactContext);
            WritableArray assets = new WritableNativeArray();

            if (gallery.getCount() <= startFrom) {
                promise.reject("gallery index out of bound", "");
                return;
            } else {
                response.putInt("totalAssets", gallery.getCount());
                boolean hasMore = gallery.getCount() > startFrom + limit;
                response.putBoolean("hasMore", hasMore);
                if (hasMore) {
                    response.putInt("next", startFrom + limit);
                } else {
                    response.putInt("next", gallery.getCount());
                }
                gallery.moveToPosition(startFrom);
            }

            do {
                WritableMap asset = getAsset(gallery);
                assets.pushMap(asset);
                if (gallery.getPosition() == (startFrom + limit) - 1) break;
            } while (gallery.moveToNext());

            response.putArray("assets", assets);

            promise.resolve(response);

        } finally {
            if (gallery != null) gallery.close();
        }
    }


    @ReactMethod
    public void getAlbums(final Promise promise) {
        if (isJellyBeanOrLater()) {
            promise.reject(new Exception("Version of Android must be > JellyBean"));
            return;
        }

        WritableMap response = new WritableNativeMap();


        Cursor gallery = null;
        try {
            gallery = GalleryCursorManager.getAlbumCursor(reactContext);
            WritableArray albums = new WritableNativeArray();
            response.putInt("totalAlbums", gallery.getCount());
            gallery.moveToFirst();
            do {
                WritableMap album = getAlbum(gallery);
                albums.pushMap(album);
            } while (gallery.moveToNext());

            response.putArray("albums", albums);

            promise.resolve(response);

        } catch (SecurityException ex) {
            System.err.println(ex);
        } finally {
            if (gallery != null) gallery.close();
        }

    }

    private WritableMap getAsset(Cursor gallery) {
        WritableMap asset = new WritableNativeMap();
        int mediaType = gallery.getInt(gallery.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
        String mimeType = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
        String creationDate = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED));
        String fileName = gallery.getString(gallery.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME));
        Double height = gallery.getDouble(gallery.getColumnIndex(MediaStore.Images.Media.HEIGHT));
        Double width = gallery.getDouble(gallery.getColumnIndex(MediaStore.Images.Media.WIDTH));
        asset.putString("mimeType", mimeType);
        asset.putString("creationDate", creationDate);
        asset.putDouble("height", height);
        asset.putDouble("width", width);
        asset.putString("filename", fileName);
        if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
            int id = gallery.getInt(gallery.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri mediaUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }
            Uri contentUri = Uri.withAppendedPath(mediaUri, String.valueOf(id));
            String uri = contentUri.toString();
            asset.putInt("id", id);
            asset.putString("uri", uri);
            asset.putDouble("duration", 0);
            asset.putString("type", "image");
        } else if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            int id = gallery.getInt(gallery.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
            Uri mediaUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            Uri contentUri = Uri.withAppendedPath(mediaUri, String.valueOf(id));
            String uri = contentUri.toString();
            asset.putInt("id", id);
            asset.putString("uri", uri);
            Double duration = gallery.getDouble(gallery.getColumnIndex(MediaStore.Video.VideoColumns.DURATION));
            asset.putDouble("duration", duration / 1000);
            asset.putString("type", "video");
        }
        return asset;
    }

    private WritableMap getAlbum(Cursor gallery) {
        WritableMap album = new WritableNativeMap();
        String albumName = gallery.getString(gallery.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
        int assetCount = gallery.getInt(gallery.getColumnIndex("assetCount"));
        album.putString("title", albumName);
        album.putInt("assetCount", assetCount);
        return album;
    }


    private Boolean isJellyBeanOrLater() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;
    }
}
