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
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String LOG_TAG = MediaPlayerService.class.getSimpleName();
    public static boolean sIsRunning = false;

    private final IBinder mBinder = new MediaPlayerServiceBinder();

    private MediaPlayer mMediaPlayer;
    private int resumePosition;
    private AudioManager mAudioManager;
    private MediaPlayerServiceEventListener mCurrentListener;

    public static final String ACTION_PLAY = "com.valdioveliu.valdio.audioplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.valdioveliu.valdio.audioplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.valdioveliu.valdio.audioplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.valdioveliu.valdio.audioplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.valdioveliu.valdio.audioplayer.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    private MediaMetadataCompat mMetadata;

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }



    public MediaPlayerService() {
    }

    @Override
    public void onCreate() {
        sIsRunning = true;
        registerBecomingNoisyReceiver();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service ended", Toast.LENGTH_SHORT).show();
        super.onDestroy();
        sIsRunning = false;

        if (mMediaPlayer != null) {
            stopMedia();
            mMediaPlayer.release();
        }
        removeAudioFocus();
//        removeNotification();
        unregisterBecomingNoisyReceiver();
//        stopMedia();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "Service unbinding", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Request audio focus
        if (!requestAudioFocus()) {
            stopMedia();
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
//                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
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
                stopMedia();
                stopSelf();
//                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
//                mMediaPlayer.release();
//                mMediaPlayer = null;
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
        stopMedia();
        //stop the service
        stopSelf();
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

    private void playMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void stopMediaNoFeedback(){
        if (mMediaPlayer == null)
            return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        stopSelf();
    }
    public void stopMedia() {
        Log.i(LOG_TAG, "stopMedia: call received");
        if (mCurrentListener != null)
            mCurrentListener.onPlaybackStopped();
        stopMediaNoFeedback();
    }

    public void pauseMediaNoFeedback(){
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            resumePosition = mMediaPlayer.getCurrentPosition();
        }
        buildNotification(PlaybackStatus.PAUSED);
    }
    public void pauseMedia() {
        Log.i(LOG_TAG, "pauseMedia: call received");
        if (mCurrentListener != null)
            mCurrentListener.onPlaybackStopped();
        pauseMediaNoFeedback();
    }

    public void resumeMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(resumePosition);
            mMediaPlayer.start();
        }
//        TODO inform the listener about this
        buildNotification(PlaybackStatus.PLAYING);
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
//            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }
    private void unregisterBecomingNoisyReceiver() {
        unregisterReceiver(becomingNoisyReceiver);
    }

    public class MediaPlayerServiceBinder extends Binder implements MediaPlayerServiceUpdater {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }

        @Override
        public void addSource(Uri localMediaUri) {
            initMediaPlayer();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                // Set the data source to the mediaFile location
                mMediaPlayer.setDataSource(MediaPlayerService.this, localMediaUri);
                retriever.setDataSource(MediaPlayerService.this, localMediaUri);
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
            setMetadata(retriever);
            mMediaPlayer.prepareAsync();
        }

        @Override
        public void addSource(String streamingURL) {
            initMediaPlayer();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                // Set the data source to the mediaFile location
                mMediaPlayer.setDataSource(streamingURL);
                retriever.setDataSource(streamingURL);
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
            setMetadata(retriever);
            mMediaPlayer.prepareAsync();
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

        @Override
        public void addEventListener(MediaPlayerServiceEventListener listener) {
            Log.i(LOG_TAG, "addEventListener: new listener for playback stops");
            if (mCurrentListener != null)
                mCurrentListener.onPlaybackStopped();
            mCurrentListener = listener;
        }

        @Override
        public void removeEventListener(MediaPlayerServiceEventListener listener) {
            if (mCurrentListener.equals(listener))
                mCurrentListener = null;
        }
    }

    public interface MediaPlayerServiceEventListener{
        public void onPlaybackStopped();
    }

    public interface MediaPlayerServiceUpdater {
        public void addSource(Uri localMediaUri);
        public void addSource(String streamingURL);
        public void addEventListener(MediaPlayerServiceEventListener listener);
        public void removeEventListener(MediaPlayerServiceEventListener listener);
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
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
            }

            @Override
            public void onPause() {
                Log.i(LOG_TAG, "onPause in mediaSessionCallback");
                super.onPause();
                pauseMedia();

            }

//            @Override
//            public void onSkipToNext() {
//                super.onSkipToNext();
//                skipToNext();
//                updateMetaData();
//                buildNotification(PlaybackStatus.PLAYING);
//            }
//
//            @Override
//            public void onSkipToPrevious() {
//                super.onSkipToPrevious();
//                skipToPrevious();
//                updateMetaData();
//                buildNotification(PlaybackStatus.PLAYING);
//            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
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

    private void removeNotification() {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
    }

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
//            case 2:
//                // Next track
//                playbackAction.setAction(ACTION_NEXT);
//                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
//            case 3:
//                // Previous track
//                playbackAction.setAction(ACTION_PREVIOUS);
//                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
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
//        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
//            transportControls.skipToNext();
//        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
//            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

}
