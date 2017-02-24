package com.tanmay.biisit;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    private static final String LOG_TAG = MediaPlaybackService.class.getSimpleName();
    private static final String MY_MEDIA_ROOT_ID = "MyMusic";
    private static final String FAVOURITES_REQUEST_KEY = "FAVOURITES_REQUEST_KEY";
    private static final String MY_MEDIA_FAV_ROOT_ID = "MyMusivFav";
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;


    public MediaPlaybackService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
//        mMediaSession.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
//        if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID, so clients can use onLoadChildren() to retrieve the content hierarchy
        if (rootHints != null)
            if (rootHints.getBoolean(FAVOURITES_REQUEST_KEY))
                return new BrowserRoot(MY_MEDIA_FAV_ROOT_ID, null);
            else
                return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        else
            return null;

//        }
//        else {
            // Returns an empty root, so clients can connect, but no content browsing possible
//            return new BrowserRoot(null, null);
//        }

    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

        // Assume for example that the music catalog is already loaded/cached.

        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID.equals(parentId)) {

            // build the MediaItem objects for the top level,
            // and put them in the mediaItems list
//            mediaItems = all music
//            mediaItems = getLocalMusic()
        } else if (MY_MEDIA_FAV_ROOT_ID.equals(parentId)){
//            mediaItems = fav music
            // examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list
        }
        result.sendResult(mediaItems);



    }
}
