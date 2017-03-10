package com.tanmay.biisit.soundCloud;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
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
import android.widget.ArrayAdapter;
import android.widget.MediaController;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.tanmay.biisit.CustomMediaController;
import com.tanmay.biisit.MediaPlayerService;
import com.tanmay.biisit.NavigationDrawerActivity;
import com.tanmay.biisit.R;
import com.tanmay.biisit.soundCloud.pojo.Track;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

import static com.tanmay.biisit.MediaPlayerService.ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.ACTION_PLAY;
import static com.tanmay.biisit.MediaPlayerService.ACTION_REDRAW;
import static com.tanmay.biisit.MediaPlayerService.ACTION_STOP;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ID_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ITEM_POS_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_MEDIA_TRACK_KEY;
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
public class SoundCloudFragment extends Fragment
        implements
        SoundCloudRecyclerViewAdapter.OnListFragmentInteractionListener,
        MediaController.MediaPlayerControl {

    public static final String CLIENT_ID = "YOUR_CLIENT_ID";
    public static final String API_URL = "https://api.soundcloud.com";

    public static final int SOUNDCLOUD_FRAGMENT_CLIENT_ID = 102;
    private static final String LOG_TAG = SoundCloudFragment.class.getSimpleName();
    private static final String SPINNER_SELECTED_KEY = "SPINNER_SELECTED_KEY";
    private static final String SELECTED_POS_KEY = "SELECTED_POS_KEY";
    private static final String CURRENT_URI_KEY = "CURRENT_URI_KEY";
    private static final String IS_PLAYING_KEY = "IS_PLAYING_KEY";
    private boolean mServiceBound = false;
    private RecyclerView mRecyclerView;
    private Track mCurrentTrack = null;
    private SoundCloudRecyclerViewAdapter mRecyclerViewAdapter = null;
    private SoundCloudFragmentReceiver mSoundCloudFragmentReceiver = new SoundCloudFragmentReceiver();
    private CustomMediaController mController;
    private ServiceConnection mServiceConn;
    private MediaPlayer mServiceMediaPlayer = null;
    private int mLastSelectedPos = -1;
    private Spinner mSpinner;
    private int mSpinnerSelectedPos = -1;
    private boolean mIsPlaying = false;

    private ProgressDialog mProgressDialog;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SoundCloudFragment() {
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
        setHasOptionsMenu(false);
//        setRetainInstance(true);
        startServiceIfDown();
    }

    private void startServiceIfDown(){
        if (!MediaPlayerService.sIsRunning){
            Intent serviceStartIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(serviceStartIntent);
        }
//        else
//            getActivity().bindService(new Intent(getActivity(), MediaPlayerService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void stopPlayAndUnbind(){
        if (mServiceBound) {
            getActivity().unbindService(mServiceConn);
            mServiceBound = false;
        }
        mCurrentTrack = null;
        mServiceMediaPlayer = null;
        mController.actuallyHide();
        playbackStopped();
    }

    private void unBind(){
        if (mServiceBound) {
            getActivity().unbindService(mServiceConn);
            mServiceBound = false;
        }
        mServiceMediaPlayer = null;
        mController.actuallyHide();
    }


    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy: Fragment destroyed");
        super.onDestroy();
        if (mController != null)
            mController.actuallyHide();
        unregisterBroadcastReceivers();
        try {
            if (mServiceBound)
                getActivity().unbindService(mServiceConn);


        } catch (IllegalArgumentException i){
            Log.d(LOG_TAG, "onDestroy: Was actually not bound");
        }
        mServiceBound = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SPINNER_SELECTED_KEY, mSpinnerSelectedPos);
        outState.putInt(SELECTED_POS_KEY, mLastSelectedPos);
        outState.putParcelable(CURRENT_URI_KEY, mCurrentTrack);
        outState.putBoolean(IS_PLAYING_KEY, mIsPlaying);
    }

    private void registerBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        intentFilter.addAction(ACTION_STOP);
        intentFilter.addAction(ACTION_REDRAW);
        getActivity().registerReceiver(mSoundCloudFragmentReceiver, intentFilter);
    }

    private void unregisterBroadcastReceivers() {
        getActivity().unregisterReceiver(mSoundCloudFragmentReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_soundcloud, container, false);
        // Set the adapter
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        if (mRecyclerView != null) {
            Context context = view.getContext();
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
//            mRecyclerView.setAdapter(new SoundCloudRecyclerViewAdapter(getActivity(), this, null, false));
        }

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
//        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(), drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("Fetching track");

        mController = new CustomMediaController(getActivity(), true);
        mController.setMediaPlayer(SoundCloudFragment.this);
        mController.setAnchorView(view.findViewById(R.id.inner_coordinator));
        mController.setEnabled(true);

        if (savedInstanceState != null){
            Log.i(LOG_TAG, "onCreateView: Restoring from saved state");
            mSpinnerSelectedPos = savedInstanceState.getInt(SPINNER_SELECTED_KEY);
            mLastSelectedPos = savedInstanceState.getInt(SELECTED_POS_KEY);
            mCurrentTrack = savedInstanceState.getParcelable(CURRENT_URI_KEY);
            mIsPlaying = savedInstanceState.getBoolean(IS_PLAYING_KEY);
//            respondToSpinnerValueChanage();
        }

        mSpinner = (Spinner) view.findViewById(R.id.search_type_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.search_spinner_choices,
                R.layout.custom_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mSpinner.setAdapter(spinnerAdapter);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) view.findViewById(R.id.search_view);
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return view;
    }

    private void showEmptyView(){
        //            TODO set empty view visible, and remove the below
        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new SoundCloudRecyclerViewAdapter(getActivity(), this, null);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
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
        intent.putExtra(BROADCAST_CLIENT_ID_KEY, SOUNDCLOUD_FRAGMENT_CLIENT_ID);
        intent.putExtra(BROADCAST_CLIENT_ITEM_POS_KEY, selectionPosition);
        if (action.equals(SERVICE_ACTION_START_PLAY))
            intent.putExtra(BROADCAST_MEDIA_TRACK_KEY, mCurrentTrack);
        intent.putExtra(BROADCAST_SEEK_POSITION_KEY, resumePosition);
//        intent.putExtra("track", mRecyclerViewAdapter.getTrackAtPos(selectionPosition));
        getActivity().sendBroadcast(intent);
    }

    @SuppressWarnings("all")
    @Override
    public void onListFragmentInteraction(Track mediaUri, boolean toStart, int position) {

        mLastSelectedPos = position;
        mIsPlaying = toStart;

        boolean sameAsCurrent = (mCurrentTrack != null) && (mediaUri.getID() == mCurrentTrack.getID());
        if (!sameAsCurrent && !toStart ) {
//            Stopping new track
            Log.e(LOG_TAG, "onListFragmentInteraction: IMPOSSIBLE!!!");
        }else if (!sameAsCurrent && toStart){
//            Starting new track
//            mLastPlayedUri = mCurrentTrack;
            startServiceIfDown();
            unBind();
            mCurrentTrack = mediaUri;
            sendServiceBroadcast(SERVICE_ACTION_START_PLAY, position);
            mProgressDialog.show();
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

    }

    private void playbackStopped() {
        Toast.makeText(getActivity(), "Playback stopped", Toast.LENGTH_SHORT).show();
        mRecyclerViewAdapter.deselectCurrentItem();
        mIsPlaying = false;
    }

    private void playbackStarted(int pos) {
        Toast.makeText(getActivity(), "Playback started at " + pos, Toast.LENGTH_SHORT).show();
        mRecyclerViewAdapter.selectItem(pos);
        mIsPlaying = true;
        Track newUri = mRecyclerViewAdapter.getTrackAtPos(pos);
        if (newUri.getID() != mCurrentTrack.getID())
            mCurrentTrack = newUri;
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

    public void handleSearch(String query) {
        Log.i(LOG_TAG, "handleSearch: Got query: " + query);
        if (mSpinner == null)
            return;
        SCAsyncTask scAsyncTask = new SCAsyncTask(getActivity());
        scAsyncTask.execute(String.valueOf(mSpinner.getSelectedItemPosition()), query);
    }

    private void handleSCResponse(Response<List<Track>> response){
        if (response == null){
            ((NavigationDrawerActivity)getActivity()).showSnackbar("Network Error");
            showEmptyView();
        }
        else if (response.isSuccessful()) {
            List<Track> tracks = response.body();
            Log.i(LOG_TAG, "onResponse: got tracks");
            mRecyclerViewAdapter = new SoundCloudRecyclerViewAdapter(getActivity(), this, tracks);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
//            TODO mSearchview.clearFocus()
        } else {
            Log.e(LOG_TAG, "onResponse: " + "Error code " + response.code());
            ((NavigationDrawerActivity)getActivity()).showSnackbar("Error while running search");
            showEmptyView();
        }
    }

    private class SCAsyncTask extends AsyncTask<String, Void, Response<List<Track>>> {

        private Context mContext;



        SCAsyncTask (Context context){
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Response<List<Track>> doInBackground(String... params) {

            SCService scService = SCSingletonHolder.getService();
            String query = params[1];
            Response<List<Track>> response;
            try {
                if (params[0].equals("0"))
                    response= scService.getTracksByKey(query).execute();
                else
                    response= scService.getTracksByTag(query).execute();

                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Response<List<Track>> response) {
            mProgressDialog.dismiss();
            handleSCResponse(response);
            super.onPostExecute(response);
        }
    }

    public class SoundCloudFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras.getInt(BROADCAST_CLIENT_ID_KEY, -1) != SOUNDCLOUD_FRAGMENT_CLIENT_ID)
                return;
            Log.i(LOG_TAG, "onReceive: " + intent.getAction());

            if (intent.getAction().equals(ACTION_PLAY)){
                int itemPosToSelect = extras.getInt(BROADCAST_RESUMED_ITEM_POS_KEY);
//                Log.i(LOG_TAG, "onReceive: Got item to select as " + itemPosToSelect);
                playbackStarted(itemPosToSelect);
                refreshOrBind();
//                mRecyclerView.getChildAt(itemPosToSelect)
            }
            else if (intent.getAction().equals(ACTION_PAUSE)){
                playbackStopped();
                refreshOrBind();
            }
            else if (intent.getAction().equals(ACTION_STOP)){
                stopPlayAndUnbind();
            }
            else if (intent.getAction().equals(ACTION_REDRAW)){
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                refreshOrBind();
            }
        }

        private void refreshOrBind(){
            if (mServiceBound)
                mController.show();
            else
                getActivity().bindService(new Intent(getActivity(), MediaPlayerService.class), mServiceConn, Context.BIND_AUTO_CREATE);
        }
    }
}

