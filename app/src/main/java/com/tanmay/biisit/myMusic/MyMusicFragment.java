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
import android.support.annotation.NonNull;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.MediaController;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tanmay.biisit.CustomMediaController;
import com.tanmay.biisit.MediaPlayerService;
import com.tanmay.biisit.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class MyMusicFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        MyMusicRecyclerViewAdapter.OnListFragmentInteractionListener,
        MediaController.MediaPlayerControl {

    public static final int MY_MUSIC_FRAGMENT_CLIENT_ID = 101;
    private static final String LOG_TAG = MyMusicFragment.class.getSimpleName();
    private static final int CURSOR_LOADER_ID_ALL = 1;
    private static final int CURSOR_LOADER_ID_FAV = 2;
    private static final String SPINNER_SELECTED_KEY = "SPINNER_SELECTED_KEY";
    private static final String SELECTED_POS_KEY = "SELECTED_POS_KEY";
    private static final String CURRENT_URI_KEY = "CURRENT_URI_KEY";
    private static final String IS_PLAYING_KEY = "IS_PLAYING_KEY";
    private static final String FAV_ID_KEY = "FAV_ID_KEY";

    private boolean mServiceBound = false;
    private RecyclerView mRecyclerView;
    private Uri mCurrentUri = null;

    private MyMusicRecyclerViewAdapter mRecyclerViewAdapter = null;
    private MyMusicFragmentReceiver mMusicFragmentReceiver = new MyMusicFragmentReceiver();

    private CustomMediaController mController;

    private ServiceConnection mServiceConn;
    private MediaPlayer mServiceMediaPlayer = null;
    private int mLastSelectedPos = -1;

    private boolean mOnlyFav = false;

    private DatabaseReference mRootRef;
    private DatabaseReference mUserInfoReference;
    private DatabaseReference mSpecificUserDataReference;
    private static final String USER_INFO_KEY = "user_info";
    private List<Integer> mFavouriteIds = null;
    private Spinner mSpinner;
    private int mSpinnerSelectedPos = -1;
    private ValueEventListener mUserValueEventListener;

    private boolean mIsLoggedIn = false;
    private String mUserId;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private boolean mIsPlaying = false;

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
        setHasOptionsMenu(true);
//        setRetainInstance(true);
        startServiceIfDown();
    }

    private void startServiceIfDown(){
        if (!MediaPlayerService.sIsRunning){
            Intent serviceStartIntent = new Intent(getActivity(), MediaPlayerService.class);
            getActivity().startService(serviceStartIntent);
        }
    }

    private void stopPlayAndUnbind(){
        if (mServiceBound) {
            getActivity().unbindService(mServiceConn);
            mServiceBound = false;
        }
        mCurrentUri = null;
        mServiceMediaPlayer = null;
        mController.actuallyHide();
        playbackStopped();
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
        if (mSpecificUserDataReference != null)
            mSpecificUserDataReference.removeEventListener(mUserValueEventListener);
        FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        Log.i(LOG_TAG, "onSaveInstanceState: saving state");
        super.onSaveInstanceState(outState);
        outState.putInt(SPINNER_SELECTED_KEY, mSpinnerSelectedPos);
//        outState.putInt(SELECTED_POS_KEY, mLastSelectedPos);
        outState.putParcelable(CURRENT_URI_KEY, mCurrentUri);
//        outState.putBoolean(IS_PLAYING_KEY, mIsPlaying);
        outState.putIntegerArrayList(FAV_ID_KEY, (ArrayList<Integer>) mFavouriteIds);
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
        Log.i(LOG_TAG, "onCreateView");
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
//            mRecyclerView.setAdapter(new MyMusicRecyclerViewAdapter(getActivity(), this, null, false));
        }

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar_mm);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                getActivity(), drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mController = new CustomMediaController(getActivity(), true);
        mController.setMediaPlayer(MyMusicFragment.this);
        mController.setAnchorView(view.findViewById(R.id.recyclerView));
        mController.setEnabled(true);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserInfoReference = mRootRef.child(USER_INFO_KEY);

        mUserValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mOnlyFav){
//                    Log.i(LOG_TAG, "onDataChange: UserData updated/listener's first call");
                    List<Integer> newFavouriteIds = new ArrayList<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()){
                        newFavouriteIds.add(Integer.valueOf(i.getKey()));
                    }

                    if (newFavouriteIds.isEmpty())
                        showEmptyView();
                    else if (mFavouriteIds == null || !(mFavouriteIds.containsAll(newFavouriteIds) && newFavouriteIds.containsAll(mFavouriteIds))) {
                        mFavouriteIds = newFavouriteIds;
//                    if (mOnlyFav || mFavouriteIds == null){
//                        Log.i(LOG_TAG, "onDataChange: fav actually changed, so will restart loader");
                        restartCursorLoader();
//                    }
                    }
                    else {
                        initCursorLoader();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    mUserId = user.getUid();
//                    Log.d(LOG_TAG, "onAuthStateChanged:signed_in:" + mUserId);
                    mIsLoggedIn = true;
                    mSpecificUserDataReference = mUserInfoReference.child(mUserId);
                    respondToSpinnerValueChanage();
                } else {
                    // User is signed out
//                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                    if (mSpecificUserDataReference != null) {
                        mSpecificUserDataReference.removeEventListener(mUserValueEventListener);
                        mSpecificUserDataReference = null;
                    }
                    mIsLoggedIn = false;
                    mFavouriteIds = null;
                    if (mOnlyFav){
                        showEmptyView();
                    }
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);

        if (savedInstanceState != null){
//            Log.i(LOG_TAG, "onCreateView: Restoring from saved state");
            mSpinnerSelectedPos = savedInstanceState.getInt(SPINNER_SELECTED_KEY);
//            mLastSelectedPos = savedInstanceState.getInt(SELECTED_POS_KEY);
            mCurrentUri = savedInstanceState.getParcelable(CURRENT_URI_KEY);
//            mIsPlaying = savedInstanceState.getBoolean(IS_PLAYING_KEY);
            mFavouriteIds = savedInstanceState.getIntegerArrayList(FAV_ID_KEY);
//            respondToSpinnerValueChanage();
        }

        if (MediaPlayerService.sCurrentClient == MY_MUSIC_FRAGMENT_CLIENT_ID) {
            mLastSelectedPos = MediaPlayerService.sCurrentClientItemPos;
            mIsPlaying = MediaPlayerService.sIsPlaying;
        }
        else {
            mLastSelectedPos = -1;
            mIsPlaying = false;
        }


        return view;
    }

    private void restartCursorLoader(){
        if (mOnlyFav)
            getActivity().getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID_FAV, null, MyMusicFragment.this);
        else
            getActivity().getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID_ALL, null, MyMusicFragment.this);
    }

    private void initCursorLoader(){
        if (mOnlyFav)
            getActivity().getSupportLoaderManager().initLoader(CURSOR_LOADER_ID_FAV, null, MyMusicFragment.this);
        else
            getActivity().getSupportLoaderManager().initLoader(CURSOR_LOADER_ID_ALL, null, MyMusicFragment.this);
    }


    private void respondToSpinnerValueChanage(){
//        if (mCurrentUri != null)
//            mController.show();
//            sendServiceBroadcast(SERVICE_ACTION_STOP);
        if (mIsLoggedIn) {
            if (mOnlyFav) {
//                Log.i(LOG_TAG, "respondToSpinnerValueChanage: Adding permanent listener");
                if (mSpecificUserDataReference != null)
                    mSpecificUserDataReference.addValueEventListener(mUserValueEventListener);
                else
                    showEmptyView();
            }
            else {
                if (mSpecificUserDataReference != null)
                    mSpecificUserDataReference.removeEventListener(mUserValueEventListener);
//                Log.i(LOG_TAG, "respondToSpinnerValueChanage: Adding one time listener");
//                if (mSpecificUserDataReference != null)
//                    mSpecificUserDataReference.addListenerForSingleValueEvent(mUserValueEventListener);
                initCursorLoader();
            }
        }
        else if (! mOnlyFav){
            initCursorLoader();
        }
        else
            showEmptyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(LOG_TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mymusic_fragment_menu, menu);
        mSpinner = (Spinner) (menu.findItem(R.id.favourites_spinner_menu_item).getActionView());
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.favourites_spinner_choices,
                R.layout.custom_spinner_item);
        spinnerAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        if (mSpinner != null) {
            mSpinner.setAdapter(spinnerAdapter);
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
//                    if (mSpinnerSelectedPos != position) {
//                        Log.i(LOG_TAG, "onItemSelected: spinner was selected with new pos " + position);
                        mSpinnerSelectedPos = position;
                        mOnlyFav = (position == 1);
//                        Log.i(LOG_TAG, "onItemSelected: removing listener for User1Ref");
                        respondToSpinnerValueChanage();
//                        getActivity().getSupportLoaderManager().restartLoader(CURSOR_LOADER_ID_ALL, null, MyMusicFragment.this);
//                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }
        else
            Log.e(LOG_TAG, "Didn't get the spinner");
        if (mSpinnerSelectedPos != -1){
            mSpinner.setSelection(mSpinnerSelectedPos);
        }

    }

    private void showEmptyView(){
        //            TODO set empty view visible, and remove the below
        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new MyMusicRecyclerViewAdapter(getActivity(), this, null, false);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(LOG_TAG, "onCreateLoader");

        switch (id) {
            case CURSOR_LOADER_ID_FAV:
                if (mFavouriteIds == null || mFavouriteIds.isEmpty()){
                    Log.e(LOG_TAG, "onCreateLoader: Bad call to loader create!!!!!!!!!!!!!!!!");
                    return null;
                }
//                Log.w(LOG_TAG, "onCreateLoader: " + mFavouriteIds.toString());
                String whereStr = " _ID in (" + TextUtils.join(", ", Arrays.toString(mFavouriteIds.toArray()).split("[\\[\\]]")[1].split(", ")) + ")";
//                Log.w(LOG_TAG, "onCreateLoader: " + whereStr);
                return new CursorLoader(getActivity(), android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, whereStr, null, null);

            case CURSOR_LOADER_ID_ALL:
                return new CursorLoader(getActivity(), android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(LOG_TAG, "onLoadFinished: got cursor of size " + data.getCount());
        if (mRecyclerView != null) {
            mRecyclerViewAdapter = new MyMusicRecyclerViewAdapter(getActivity(), this, data, mOnlyFav);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
            if (mIsPlaying && mLastSelectedPos != -1){
                playbackStarted(mLastSelectedPos);
            }
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

    @SuppressWarnings("all")
    @Override
    public void onListFragmentInteraction(Uri mediaUri, boolean toStart, int position) {

        mLastSelectedPos = position;
        mIsPlaying = toStart;
        boolean sameAsCurrent;
        if (MediaPlayerService.sCurrentClient == MY_MUSIC_FRAGMENT_CLIENT_ID) {
            sameAsCurrent = mediaUri.equals(mCurrentUri);
        }
        else
            sameAsCurrent = false;
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
//        Log.i(LOG_TAG, "playbackStopped");
        mRecyclerViewAdapter.deselectCurrentItem();
        mIsPlaying = false;
    }
    private void playbackStarted(int pos) {
//        Log.i(LOG_TAG, "playbackStarted: at " + pos);
        mRecyclerViewAdapter.selectItem(pos);
        mIsPlaying = true;
        Uri newUri = mRecyclerViewAdapter.getUriAtPos(pos);
        if (newUri != null && !newUri.equals(mCurrentUri))
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
            Bundle extras = intent.getExtras();
            if (extras.getInt(BROADCAST_CLIENT_ID_KEY, -1) != MY_MUSIC_FRAGMENT_CLIENT_ID)
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
                refreshOrBind();
            }
        }

        private void refreshOrBind(){
            if (mServiceBound) {
                mController.show();
                if (MediaPlayerService.sCurrentClient == MY_MUSIC_FRAGMENT_CLIENT_ID && mLastSelectedPos != MediaPlayerService.sCurrentClientItemPos) {
                    mLastSelectedPos = MediaPlayerService.sCurrentClientItemPos;
                    if (mIsPlaying)
                        playbackStopped();
//                    if (mIsPlaying != MediaPlayerService.sIsPlaying)
                        mIsPlaying = MediaPlayerService.sIsPlaying;
                    if (mIsPlaying)
                        playbackStarted(mLastSelectedPos);
                }
            }
            else
                getActivity().bindService(new Intent(getActivity(), MediaPlayerService.class), mServiceConn, Context.BIND_AUTO_CREATE);
        }
    }
}

