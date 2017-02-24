package com.tanmay.biisit.myMusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
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
import com.tanmay.biisit.myMusic.interfaces.OnListFragmentInteractionListener;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {link OnListFragmentInteractionListener}
 * interface.
 */
public class MyMusicFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnListFragmentInteractionListener, MediaPlayerService.MediaPlayerServiceEventListener {

    private static final String LOG_TAG = MyMusicFragment.class.getSimpleName();
    //    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
//    private OnListFragmentInteractionListener mListener;
    private static final int CURSOR_LOADER_ID = 1;
    private RecyclerView mRecyclerView;

    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

    private MediaPlayerService.MediaPlayerServiceBinder binder;
    private MediaPlayerService player;
    private boolean serviceBound;
    private Uri mUriToPlay = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            binder = (MediaPlayerService.MediaPlayerServiceBinder) service;
            binder.addEventListener(MyMusicFragment.this);
            binder.addSource(mUriToPlay);
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(getActivity(), "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private Uri mPrevMediaUri = null;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MyMusicFragment() {
    }

//    // TODO: Customize parameter initialization
//    @SuppressWarnings("unused")
//    public static MyMusicFragment newInstance(int columnCount) {
//        MyMusicFragment fragment = new MyMusicFragment();
//        Bundle args = new Bundle();
//        args.putInt(ARG_COLUMN_COUNT, columnCount);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (getArguments() != null) {
//            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
//        }

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(getActivity(), LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE
                );
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
//        mMediaSession.setCallback(new MySessionCallback());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView: Starting");
        View view = inflater.inflate(R.layout.fragment_mymusic, container, false);

        // Set the adapter
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        if (mRecyclerView != null) {
            Context context = view.getContext();
            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
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
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnListFragmentInteractionListener) {
//            mListener = (OnListFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnListFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(LOG_TAG, "onLoadFinished: got cursor of size " + data.getCount());
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(new MyMusicRecyclerViewAdapter(getActivity(), this, data));
        else
            Log.w(LOG_TAG, "onLoadFinished: not setting adapter as recycler view was not set" );

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onListFragmentInteraction(Uri mediaUri, boolean toStart) {
        if (toStart){
            if (mediaUri.equals(mPrevMediaUri))
                player.resumeMedia();
            else
                playAudio(mediaUri);
        }
        else {
            mPrevMediaUri = mediaUri;
            player.pauseMedia();
        }
//        String action = toStart? "Start" : "Stop";
//        Toast.makeText(getActivity(), mediaUri + " is to be " + action + "ed", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onPlaybackStopped() {

    }

    private void playAudio(Uri media) {
        mUriToPlay = media;
        //Check is service is active
        if (!serviceBound) {
            Intent playerIntent = new Intent(getActivity(), MediaPlayerService.class);
            if (! MediaPlayerService.sIsRunning)
                getActivity().startService(playerIntent);
            getActivity().bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            binder.addSource(media);
            player = binder.getService();
        }
    }
}

