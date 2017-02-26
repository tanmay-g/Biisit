package com.tanmay.biisit.myMusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.Toast;

import com.tanmay.biisit.MediaPlayerService;
import com.tanmay.biisit.R;

import static com.tanmay.biisit.MediaPlayerService.ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.ACTION_PLAY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ID_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ITEM_POS_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_MEDIA_URI_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_RESUMED_ITEM_POS;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_RESUME;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_START_PLAY;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class MyMusicFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, MyMusicRecyclerViewAdapter.OnListFragmentInteractionListener{

    private static final String LOG_TAG = MyMusicFragment.class.getSimpleName();
//    private OnListFragmentInteractionListener mListener;
    private static final int CURSOR_LOADER_ID = 1;
    //    private static final String ARG_COLUMN_COUNT = "column-count";
//    private int mColumnCount = 1;
    private RecyclerView mRecyclerView;

//    private MediaSessionCompat mMediaSession;
//    private PlaybackStateCompat.Builder mStateBuilder;

//    private MediaPlayerService player;
//    private boolean serviceBound;

    private Uri mCurrentUri = null;
//    private Uri mLastPlayedUri = null;
    private MyMusicRecyclerViewAdapter mRecyclerViewAdapter = null;

    public static final int MY_MUSIC_FRAGMENT_CLIENT_ID = 101;

    private MyMusicFragmentReceiver mMusicFragmentReceiver = new MyMusicFragmentReceiver();


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
        if (!MediaPlayerService.sIsRunning){
            Intent serviceStartIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(serviceStartIntent);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy: Fragment destroyed");
        super.onDestroy();
        unregisterBroadcastReceivers();
    }

    private void registerBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
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

    private void sendServiceBroadcast(String action, int position){
        Log.i(LOG_TAG, "sendServiceBroadcast: " + action);
        Intent intent = new Intent();
//        Intent intent = new Intent(getActivity(), MediaPlayerService.class.getCanonicalName());
        intent.setAction(action);
        intent.putExtra(BROADCAST_CLIENT_ID_KEY, MY_MUSIC_FRAGMENT_CLIENT_ID);
        intent.putExtra(BROADCAST_CLIENT_ITEM_POS_KEY, position);
        intent.putExtra(BROADCAST_MEDIA_URI_KEY, mCurrentUri);
        getActivity().sendBroadcast(intent);

    }

    @Override
    public void onListFragmentInteraction(Uri mediaUri, boolean toStart, int position) {

        Log.i(LOG_TAG, "onListFragmentInteraction: Reporting click at " + position);
        boolean sameAsCurrent = mediaUri.equals(mCurrentUri);
        if (!sameAsCurrent && !toStart ) {
//            Stopping new track
            Log.e(LOG_TAG, "onListFragmentInteraction: IMPOSSIBLE!!!");
        }else if (!sameAsCurrent && toStart){
//            Starting new track
//            mLastPlayedUri = mCurrentUri;
            mCurrentUri = mediaUri;
            sendServiceBroadcast(SERVICE_ACTION_START_PLAY, position);
        }
        else if (sameAsCurrent && ! toStart){
//            Stopping current track
            sendServiceBroadcast(SERVICE_ACTION_PAUSE, position);
        }
        else if (sameAsCurrent && toStart){
//            Starting current track
            sendServiceBroadcast(SERVICE_ACTION_RESUME, position);
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

    public class MyMusicFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "onReceive: " + intent.getAction());
            Bundle extras = intent.getExtras();
            if (extras.getInt(BROADCAST_CLIENT_ID_KEY, -1) != MY_MUSIC_FRAGMENT_CLIENT_ID)
                return;

            if (intent.getAction().equals(ACTION_PLAY)){
                int itemPosToSelect = extras.getInt(BROADCAST_RESUMED_ITEM_POS);
                Log.i(LOG_TAG, "onReceive: Got item to select as " + itemPosToSelect);
                playbackStarted(itemPosToSelect);
//                mRecyclerView.getChildAt(itemPosToSelect)
            }
            else if (intent.getAction().equals(ACTION_PAUSE)){
                playbackStopped();
            }
        }
    }
}

