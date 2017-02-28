package com.tanmay.biisit.myMusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;

import com.tanmay.biisit.MediaPlayerService;
import com.tanmay.biisit.R;

import static com.tanmay.biisit.MediaPlayerService.ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.ACTION_PLAY;
import static com.tanmay.biisit.MediaPlayerService.ACTION_REDRAW;
import static com.tanmay.biisit.MediaPlayerService.ACTION_STOP;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ID_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ITEM_POS_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_MEDIA_URI_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_RESUMED_ITEM_POS_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_SEEK_POSITION_KEY;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_RESUME;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_SEEK;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_START_PLAY;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class MyMusicFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, MyMusicRecyclerViewAdapter.OnListFragmentInteractionListener, MediaController.MediaPlayerControl {

    public static final int MY_MUSIC_FRAGMENT_CLIENT_ID = 101;
    private static final String LOG_TAG = MyMusicFragment.class.getSimpleName();
//    private OnListFragmentInteractionListener mListener;
    private static final int CURSOR_LOADER_ID = 1;

//    private MediaSessionCompat mMediaSession;
//    private PlaybackStateCompat.Builder mStateBuilder;

//    private MediaPlayerService player;
    private boolean mServiceBound = false;
    //    private static final String ARG_COLUMN_COUNT = "column-count";
//    private int mColumnCount = 1;
    private RecyclerView mRecyclerView;
    private Uri mCurrentUri = null;
//    private Uri mLastPlayedUri = null;
    private MyMusicRecyclerViewAdapter mRecyclerViewAdapter = null;
    private MyMusicFragmentReceiver mMusicFragmentReceiver = new MyMusicFragmentReceiver();

    private CustomMediaController mController;

    private ServiceConnection mServiceConn;
    private MediaPlayer mServiceMediaPlayer = null;
    private int mLastSelectedPos;

    private class CustomMediaController extends MediaController{

        public CustomMediaController(Context context, boolean useFastForward) {
            super(context, useFastForward);
        }

        public CustomMediaController(Context context) {
            super(context);
        }

        public void actuallyHide(){
            super.hide();
        }

        @Override
        public void hide() {
//            super.hide();
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MyMusicFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate: Fragment created");
        super.onCreate(savedInstanceState);
        registerBroadcastReceivers();
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBound = true;
                mServiceMediaPlayer = (((MediaPlayerService.ServiceBinder)service).getMediaPlayer());
                Log.i(LOG_TAG, "onServiceConnected: Service bound");
                mController.show();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
//                Simply end play if service dies
                Log.i(LOG_TAG, "onServiceDisconnected: Service unbound");
                stopPlayAndUnbind();
            }
        };
        startServiceIfDown();
    }

    private void startServiceIfDown(){
        if (!MediaPlayerService.sIsRunning){
            Intent serviceStartIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(serviceStartIntent);
        }
    }

    private void stopPlayAndUnbind(){
        getActivity().unbindService(mServiceConn);
        mServiceBound = false;
        mServiceMediaPlayer = null;
        mController.actuallyHide();
        playbackStopped();
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy: Fragment destroyed");
        super.onDestroy();
        unregisterBroadcastReceivers();
        getActivity().unbindService(mServiceConn);
    }

    private void registerBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_STOP);
        intentFilter.addAction(ACTION_REDRAW);
        getActivity().registerReceiver(mMusicFragmentReceiver, intentFilter);
    }
    private void unregisterBroadcastReceivers() {
        getActivity().unregisterReceiver(mMusicFragmentReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mymusic, container, false);

        // Set the adapter
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        if (mRecyclerView != null) {
            Context context = view.getContext();
//            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
//            } else {
//                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
//            }
            mRecyclerView.setAdapter(new MyMusicRecyclerViewAdapter(getActivity(), this, null));
        }

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(), drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mController = new CustomMediaController(getActivity(), true);
        mController.setMediaPlayer(MyMusicFragment.this);
        mController.setAnchorView(view.findViewById(R.id.recyclerView));
        mController.setEnabled(true);

        getActivity().getSupportLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(LOG_TAG, "onLoadFinished: got cursor of size " + data.getCount());
        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new MyMusicRecyclerViewAdapter(getActivity(), this, data);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
        else
            Log.w(LOG_TAG, "onLoadFinished: not setting adapter as recycler view was not set" );
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void sendServiceBroadcast(String action){
        sendServiceBroadcast(action, -1);
    }
    private void sendServiceBroadcast(String action, int selectionPosition){
        sendServiceBroadcast(action, selectionPosition, -1);
    }

    private void sendServiceBroadcast(String action, int selectionPosition, int resumePosition) {
        Log.i(LOG_TAG, "sendServiceBroadcast: " + action);
        Intent intent = new Intent();
//        Intent intent = new Intent(getActivity(), MediaPlayerService.class.getCanonicalName());
        intent.setAction(action);
        intent.putExtra(BROADCAST_CLIENT_ID_KEY, MY_MUSIC_FRAGMENT_CLIENT_ID);
        intent.putExtra(BROADCAST_CLIENT_ITEM_POS_KEY, selectionPosition);
        intent.putExtra(BROADCAST_MEDIA_URI_KEY, mCurrentUri);
        intent.putExtra(BROADCAST_SEEK_POSITION_KEY, resumePosition);
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onListFragmentInteraction(Uri mediaUri, boolean toStart, int position) {

        mLastSelectedPos = position;

        boolean sameAsCurrent = mediaUri.equals(mCurrentUri);
        if (!sameAsCurrent && !toStart ) {
//            Stopping new track
            Log.e(LOG_TAG, "onListFragmentInteraction: IMPOSSIBLE!!!");
        }else if (!sameAsCurrent && toStart){
//            Starting new track
//            mLastPlayedUri = mCurrentUri;
            startServiceIfDown();
            mCurrentUri = mediaUri;
            sendServiceBroadcast(SERVICE_ACTION_START_PLAY, position);
        }
        else if (sameAsCurrent && ! toStart){
//            Stopping current track
            sendServiceBroadcast(SERVICE_ACTION_PAUSE);
//            mController.show();
        }
        else if (sameAsCurrent && toStart){
//            Starting current track
            sendServiceBroadcast(SERVICE_ACTION_RESUME);
//            mController.show();
        }

//        if (toStart){
//            if (mediaUri.equals(mLastPlayedUri)) {
//                sendServiceBroadcast(SERVICE_ACTION_RESUME, position);
//            }
//            else {
//                mLastPlayedUri = mCurrentUri;
//                mCurrentUri = mediaUri;
//                sendServiceBroadcast(SERVICE_ACTION_START_PLAY, position);
//            }
//        }
//        else {
//            mLastPlayedUri = mediaUri;
//            sendServiceBroadcast(SERVICE_ACTION_PAUSE, position);
//        }
//        String action = toStart? "Start" : "Stop";
//        Toast.makeText(getActivity(), mediaUri + " is to be " + action + "ed", Toast.LENGTH_SHORT).show();
    }

    private void playbackStopped() {
        Toast.makeText(getActivity(), "Playback stopped", Toast.LENGTH_SHORT).show();
        mRecyclerViewAdapter.deselectCurrentItem();
    }
    private void playbackStarted(int pos) {
        Toast.makeText(getActivity(), "Playback started at " + pos, Toast.LENGTH_SHORT).show();
        mRecyclerViewAdapter.selectItem(pos);
        Uri newUri = mRecyclerViewAdapter.getUriAtPos(pos);
        if (!newUri.equals(mCurrentUri))
            mCurrentUri = newUri;
    }

    @Override
    public void start() {
        if (mServiceMediaPlayer != null)
            mServiceMediaPlayer.start();
        playbackStarted(mLastSelectedPos);
        sendServiceBroadcast(SERVICE_ACTION_RESUME);
    }

    @Override
    public void pause() {
        if (mServiceMediaPlayer != null)
            mServiceMediaPlayer.pause();
        playbackStopped();
//        int newPos = mServiceMediaPlayer.getCurrentPosition();
        sendServiceBroadcast(SERVICE_ACTION_PAUSE);
    }


    @Override
    public int getDuration() {
        if (mServiceMediaPlayer != null)
            return mServiceMediaPlayer.getDuration();
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mServiceMediaPlayer != null)
            return mServiceMediaPlayer.getCurrentPosition();
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (mServiceMediaPlayer != null)
            mServiceMediaPlayer.seekTo(pos);
        sendServiceBroadcast(SERVICE_ACTION_SEEK, -1, pos);
    }

    @Override
    public boolean isPlaying() {
        return mServiceMediaPlayer != null && mServiceMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        if (mServiceMediaPlayer != null)
            return mServiceMediaPlayer.getAudioSessionId();
        return -1;
    }

    public class MyMusicFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "onReceive: " + intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras.getInt(BROADCAST_CLIENT_ID_KEY, -1) != MY_MUSIC_FRAGMENT_CLIENT_ID)
                return;

            if (intent.getAction().equals(ACTION_PLAY)){
                int itemPosToSelect = extras.getInt(BROADCAST_RESUMED_ITEM_POS_KEY);
                Log.i(LOG_TAG, "onReceive: Got item to select as " + itemPosToSelect);
                playbackStarted(itemPosToSelect);
                mController.show();
//                mRecyclerView.getChildAt(itemPosToSelect)
            }
            else if (intent.getAction().equals(ACTION_PAUSE)){
                playbackStopped();
                mController.show();
            }
            else if (intent.getAction().equals(ACTION_STOP)){
                stopPlayAndUnbind();
            }
            else if (intent.getAction().equals(ACTION_REDRAW)){
                if (mServiceBound)
                    mController.show();
                else
                    getActivity().bindService(new Intent(getActivity(), MediaPlayerService.class), mServiceConn, Context.BIND_AUTO_CREATE);
            }
        }
    }
}

