package com.tanmay.biisit;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.tanmay.biisit.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.tanmay.biisit.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.tanmay.biisit.ACTION_STOP";
    public static final String ACTION_REDRAW = "com.tanmay.biisit.ACTION_REDRAW";

    public static final String BROADCAST_RESUMED_ITEM_POS_KEY = "BROADCAST_RESUMED_ITEM_POS_KEY";
    public static final String BROADCAST_CLIENT_ID_KEY = "BROADCAST_CLIENT_ID_KEY";
    public static final String BROADCAST_CLIENT_ITEM_POS_KEY = "BROADCAST_CLIENT_ITEM_POS_KEY";
    public static final String BROADCAST_SEEK_POSITION_KEY = "BROADCAST_SEEK_POSITION_KEY";
    public static final String BROADCAST_MEDIA_URI_KEY = "BROADCAST_MEDIA_URI_KEY";

    public static final String SERVICE_ACTION_START_PLAY = "com.tanmay.biisit.SERVICE_ACTION_START_PLAY";
    public static final String SERVICE_ACTION_RESUME = "com.tanmay.biisit.SERVICE_ACTION_RESUME";
    public static final String SERVICE_ACTION_PAUSE = "com.tanmay.biisit.SERVICE_ACTION_PAUSE";
    public static final String SERVICE_ACTION_STOP = "com.tanmay.biisit.SERVICE_ACTION_STOP";
    public static final String SERVICE_ACTION_SEEK = "com.tanmay.biisit.SERVICE_ACTION_SEEK";

    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    public static boolean sIsRunning = false;
    private MediaPlayer mMediaPlayer;
    private int resumePosition;
    private AudioManager mAudioManager;
    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private MediaMetadataCompat mMetadata;
    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
//            buildNotification(PlaybackStatus.PAUSED);
        }
    };
    private int mCurrentClient = -1;
    private int mClientItemPos = -1;

    private MediaPlayerServiceReceiver mServiceBroadcastListener = new MediaPlayerServiceReceiver();

    public class ServiceBinder extends Binder{
        public MediaPlayer getMediaPlayer(){
            if (mMediaPlayer == null) {
                Log.e(LOG_TAG, "getMediaPlayer: Bound too early");
                initMediaPlayer();
            }
            return mMediaPlayer;
        }
    };

    public MediaPlayerService() {
    }

    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "onCreate: Service started");
        sIsRunning = true;
        registerBroadcastReceivers();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy: Service ended");
        super.onDestroy();
        sIsRunning = false;

        if (mMediaPlayer != null) {
            stopMediaNoFeedback();
            mMediaPlayer.release();
        }
        removeAudioFocus();
//        removeNotification();
        unregisterBroadcastReceivers();
//        stopMedia();
    }

    private void endSelf(){
        stopForeground(true);
//        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Request audio focus
        if (!requestAudioFocus()) {
            Log.i(LOG_TAG, "onStartCommand: Stopping because couldn't get audiofocus");
            stopMedia();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
//                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                endSelf();
            }
//            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                Log.i(LOG_TAG, "onAudioFocusChange: Stopping because lost audiofocus");
                stopMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.abandonAudioFocus(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        Log.i(LOG_TAG, "onCompletion: Stopping as media completed");
        resumePosition = 0;
        stopMedia();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
        sendServiceBroadcast(ACTION_REDRAW);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
        else
            mMediaPlayer.reset();
            //Reset so that the MediaPlayer is not pointing to another data source

        //Set up MediaPlayer event listeners
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    private void sendServiceBroadcast(String action){
        Log.i(LOG_TAG, "sendServiceBroadcast: " + action);
//        Log.i(LOG_TAG, "sendServiceBroadcast: Sending client pos as " + mClientItemPos);
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(BROADCAST_CLIENT_ID_KEY, mCurrentClient);
        intent.putExtra(BROADCAST_RESUMED_ITEM_POS_KEY, mClientItemPos);
        sendBroadcast(intent);

    }

    private void playMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void stopMediaNoFeedback(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
//        sendServiceBroadcast(ACTION_REDRAW);
        endSelf();
    }

    public void stopMedia() {
        Log.i(LOG_TAG, "stopMedia: call received");
        sendServiceBroadcast(ACTION_STOP);
        stopMediaNoFeedback();
    }

    public void pauseMediaNoFeedback(){
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            resumePosition = mMediaPlayer.getCurrentPosition();
        }
        else
            Log.i(LOG_TAG, "pauseMediaNoFeedback: GOT PAUSED WHILE NOT PLAYING!!!");
        buildNotification(PlaybackStatus.PAUSED);
    }

    public void pauseMedia() {
        Log.i(LOG_TAG, "pauseMedia: call received");
        sendServiceBroadcast(ACTION_PAUSE);
        pauseMediaNoFeedback();
    }

    private void resumeMediaNoFeedback(){
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(resumePosition);
            mMediaPlayer.start();
        }
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void resumeMedia() {
        Log.i(LOG_TAG, "resumeMedia: call received");
        sendServiceBroadcast(ACTION_PLAY);
        resumeMediaNoFeedback();
    }

    private void registerBroadcastReceivers() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);

        intentFilter = new IntentFilter(SERVICE_ACTION_START_PLAY);
        intentFilter.addAction(SERVICE_ACTION_RESUME);
        intentFilter.addAction(SERVICE_ACTION_PAUSE);
        intentFilter.addAction(SERVICE_ACTION_STOP);
        intentFilter.addAction(SERVICE_ACTION_SEEK);
        registerReceiver(mServiceBroadcastListener, intentFilter);
    }
    private void unregisterBroadcastReceivers() {
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(mServiceBroadcastListener);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), LOG_TAG);
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
//        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        mediaSession.setMetadata(mMetadata);

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks, from notification and controller. Service internal callbacks.
            // DO Inform current player about this to update UI
            @Override
            public void onPlay() {
//                Log.i(LOG_TAG, "onPlay in mediaSessionCallback");
                super.onPlay();
                resumeMedia();
            }

            @Override
            public void onPause() {
//                Log.i(LOG_TAG, "onPause in mediaSessionCallback");
                super.onPause();
                pauseMedia();

            }

            @Override
            public void onStop() {
                super.onStop();
//                Log.i(LOG_TAG, "onStop in mediaSessionCallback");
                stopMedia();
            }

            @Override
            public void onSeekTo(long position) {
//                Log.i(LOG_TAG, "seekTo in mediaSessionCallback");
                super.onSeekTo(position);
            }
        });
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }


        Bitmap largeIcon = mMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
//                        .setShowActionsInCompactView(0, 1, 2))
                        .setShowActionsInCompactView(0))
                // Set the Notification color
//                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(mMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setContentInfo(mMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setContentTitle(mMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                // Add playback actions
//                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction);
//                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

//    private void removeNotification() {
////        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
////        notificationManager.cancel(NOTIFICATION_ID);
//        stopForeground(true);
//    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private void setMetadata(MediaMetadataRetriever retriever){
        byte[] by = retriever.getEmbeddedPicture();
        Bitmap b;
        if (by != null)
            b = BitmapFactory.decodeByteArray(by, 0, by.length);
        else
            b = BitmapFactory.decodeResource(getResources(), android.R.drawable.stat_sys_headset);
        mMetadata = new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, b)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                .build();
    }

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    public class MediaPlayerServiceReceiver extends BroadcastReceiver {

        public MediaPlayerServiceReceiver() {
//            Log.i(LOG_TAG, "MediaPlayerServiceReceiver: Created");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //This will be called from an external source, usually from the existing player, but also from new players
            Log.i(LOG_TAG, "onReceive: " + intent.getAction());
            Bundle extras = intent.getExtras();

            if (intent.getAction().equals(SERVICE_ACTION_START_PLAY)){

                Uri localMediaUri = null;
                int clientId = -1;
                int clientItemPos = -1;
                try {
                    localMediaUri = (Uri)extras.get(BROADCAST_MEDIA_URI_KEY);
                    clientId = extras.getInt(BROADCAST_CLIENT_ID_KEY);
                    clientItemPos = extras.getInt(BROADCAST_CLIENT_ITEM_POS_KEY);
                }
                catch (Exception e){
                    e.printStackTrace();
                    stopMedia();
                }
                if (mCurrentClient != -1 && mCurrentClient != clientId){
                    sendServiceBroadcast(ACTION_PAUSE);
                }
                else if (mClientItemPos == clientItemPos){
                    Log.i(LOG_TAG, "onReceive: Got a start request for something that was already playing");
//                    This shouldn't happen, but handle it to be nice
                    resumeMediaNoFeedback();
                    sendServiceBroadcast(ACTION_REDRAW);
                    return;
                }

                mCurrentClient = clientId;
                mClientItemPos = clientItemPos;
                initMediaPlayer();
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    // Set the data source to the mediaFile location
                    if (localMediaUri != null) {
                        mMediaPlayer.setDataSource(MediaPlayerService.this, localMediaUri);
                        retriever.setDataSource(MediaPlayerService.this, localMediaUri);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    stopMedia();
                }
                setMetadata(retriever);
                mMediaPlayer.prepareAsync();
            }

            else if (intent.getAction().equals(SERVICE_ACTION_RESUME)){
                resumeMediaNoFeedback();
                sendServiceBroadcast(ACTION_REDRAW);
            }
            else if (intent.getAction().equals(SERVICE_ACTION_PAUSE)){
                pauseMediaNoFeedback();
                sendServiceBroadcast(ACTION_REDRAW);
            }
            else if (intent.getAction().equals(SERVICE_ACTION_STOP)){
//                sendServiceBroadcast(ACTION_STOP);
                stopMediaNoFeedback();
            }
            else if (intent.getAction().equals(SERVICE_ACTION_SEEK)){
                int newResumePos = extras.getInt(BROADCAST_SEEK_POSITION_KEY);
                if (newResumePos != -1) {
                    resumePosition = newResumePos;
                }
            }
        }
    }
}
