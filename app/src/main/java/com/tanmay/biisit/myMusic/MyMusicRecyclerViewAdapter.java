package com.tanmay.biisit.myMusic;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tanmay.biisit.R;

import java.util.Arrays;

import static com.tanmay.biisit.R.id.checkBox;


/**
 * {@link RecyclerView.Adapter} that can display a song from a cursor and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
class MyMusicRecyclerViewAdapter extends RecyclerView.Adapter<MyMusicRecyclerViewAdapter.ViewHolder> {

    private static final String LOG_TAG = MyMusicRecyclerViewAdapter.class.getSimpleName();
    private final Cursor mValues;
    private int [] mExternalPositions;
    private final Context mContext;
    private final int mTitleColumn;
    private final int mIdColumn;
    private final int mArtistColumn;
    private final OnListFragmentInteractionListener mListener;
    private boolean mOnlyFav;
    private int mSelectedPosition = -1;
    private View mSelectedView;

    private DatabaseReference mRootRef;
    private DatabaseReference mUserInfoReference;
    private DatabaseReference mSpecificUserDataReference;
    private static final String USER_INFO_KEY = "user_info";
//    private static final String USER_1_KEY = "user_1";
//    private static final String ITEM_KEY_PREFIX = "item_";

    private boolean mIsLoggedIn = false;
    private String mUserId;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private int mSelectedExternalPosition = -2;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mValues != null && mValues.moveToFirst())
            FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mValues != null && mValues.moveToFirst())
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
    }

    MyMusicRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener, final Cursor data, boolean onlyFav) {
        mContext = context;
        mValues = data;
        mListener = listener;
        mOnlyFav = onlyFav;

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserInfoReference = mRootRef.child(USER_INFO_KEY);

        if (mValues != null && mValues.moveToFirst()) {
            mTitleColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            mIdColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            mArtistColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            mExternalPositions = new int[mValues.getCount()];
            Arrays.fill(mExternalPositions, -1);
        }
        else  {
            mTitleColumn = 0;
            mIdColumn = 0;
            mArtistColumn = 0;
            return;
        }

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
                    mSpecificUserDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
//                                Log.i(LOG_TAG, "onDataChange: STARTING SET OF EXTERNAL VALS, with selected pos: " + mSelectedPosition + " and external selected pos: " + mSelectedExternalPosition);
                            for (int i = 0; i < mValues.getCount(); i++) {
                                mValues.moveToPosition(i);
                                DataSnapshot c = dataSnapshot.child(String.valueOf((int) mValues.getLong(mIdColumn)));
                                if (c.exists()) {
                                    mExternalPositions[i] = c.getValue(int.class);
                                    notifyItemChanged(i);
                                }
                            }
//                                Log.i(LOG_TAG, "onDataChange: EXTERNAL POS VALUES SET");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    // User is signed out
//                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                    mSpecificUserDataReference = null;
                    mIsLoggedIn = false;
                }
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_mymusic_list_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
//        holder.mItem = mValues.moveToPosition(position);

        mValues.moveToPosition(position);
        holder.mIdView.setText(
                String.valueOf((int) mValues.getLong(mIdColumn))
        );
        holder.mTitleView.setText(
                mValues.getString(mTitleColumn)
        );
        holder.mArtistView.setText(
                mValues.getString(mArtistColumn)
        );

//        Log.i(LOG_TAG, "onBindViewHolder: position: " + position + ", mSelectedPosition: "+mSelectedPosition+", mExternalPositions[position]: "+mExternalPositions[position]+", mSelectedExternalPosition:" + mSelectedExternalPosition);
        if (position == mSelectedPosition || mExternalPositions[position] == mSelectedExternalPosition){
//            Log.i(LOG_TAG, "onBindViewHolder: setting selected");
            holder.mView.setSelected(true);
            mSelectedView = holder.mView;
            holder.mButton.setImageResource(R.drawable.ic_pause);
            holder.mButton.setContentDescription(mContext.getString(R.string.tap_to_pause_message));
        }
        else {
//            Log.i(LOG_TAG, "onBindViewHolder: setting not selected");
            holder.mButton.setImageResource(R.drawable.ic_play);
            holder.mView.setSelected(false);
            holder.mButton.setContentDescription(mContext.getString(R.string.tap_to_play_message));
        }

        if (mIsLoggedIn) {
            holder.mStar.setEnabled(true);
            holder.mStar.setChecked(mExternalPositions[position] != -1);
        }
        else {
//            holder.mStar.setClickable(false);
            holder.mStar.setEnabled(false);
        }
    }

//    void updateFavOnlyState(boolean onlyFav){
//        Log.i(LOG_TAG, "updateFavOnlyState: " + onlyFav);
//        mOnlyFav = onlyFav;
//    }

    @Override
    public int getItemCount() {
        return (mValues != null && mValues.moveToFirst()) ? mValues.getCount() : 0;
    }

    void selectItem(int position){
        int actualPos = mOnlyFav ? getInternalPos(position) : position;
//        Log.i(LOG_TAG, "selectItem: Got asked to select: " + position + ". Will actually select " + actualPos);
        mSelectedPosition = actualPos;
        if (actualPos != -1)
            notifyItemChanged(mSelectedPosition);
        else
            mSelectedExternalPosition = position;
    }

    void deselectCurrentItem(){
//        Log.i(LOG_TAG, "deselectCurrentItem: call received");
        unSelectSelectedView();
    }

    private void unSelectSelectedView(){
        if (mSelectedView != null) {
            mSelectedView.setSelected(false);
            ImageView button = (ImageView)mSelectedView.findViewById(R.id.button);
            button.setImageResource(R.drawable.ic_play);
            button.setContentDescription(mContext.getString(R.string.tap_to_play_message));
        }
        mSelectedPosition = -1;
    }

    Uri getUriAtPos(int position){
        int actualPos = mOnlyFav ? getInternalPos(position) : position;
        try {
            mValues.moveToPosition(actualPos);
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mValues.getInt(mIdColumn));
        }
        catch (Exception e){
            return null;
        }
    }
    private int getInternalPos(int externalPos){
        for (int i = 0; i < mValues.getCount(); i++) {
            if (mExternalPositions[i] == externalPos) {
                return i;
            }
        }
        return -1;
    }
    interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Uri mediaUriToPlay, boolean start, int position);
//        void onStarToggled();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mIdView;
        final TextView mTitleView;
        final TextView mArtistView;
        final ImageView mButton;
        final CheckBox mStar;

        private final View.OnClickListener mainListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = getAdapterPosition();
                mValues.moveToPosition(adapterPosition);
//                Log.i(LOG_TAG, "onClick: position " + adapterPosition);
                boolean isAlreadyRunning = mView.isSelected();
                unSelectSelectedView();
                if (!isAlreadyRunning){
                    mSelectedPosition = adapterPosition;
                    mSelectedView = mView;
                }
                else {
                    mSelectedPosition = -1;
                    mSelectedView = null;
                }
                mView.setSelected(!isAlreadyRunning);
                mButton.setImageResource(
                        !isAlreadyRunning ? R.drawable.ic_pause : R.drawable.ic_play
                );
                mButton.setContentDescription(
                        !isAlreadyRunning ? mContext.getString(R.string.tap_to_pause_message) : mContext.getString(R.string.tap_to_play_message)
                );
                Uri mediaUri=
                        ContentUris
                                .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        mValues.getInt(mIdColumn));
                if (mOnlyFav) {
//                    Log.i(LOG_TAG, "onClick: telling fragment about click at the actual " + mActualPos);
                    mListener.onListFragmentInteraction(mediaUri, !isAlreadyRunning, mExternalPositions[adapterPosition]);
                } else {
//                    Log.i(LOG_TAG, "onClick: telling fragment about click at " + adapterPosition);
                    mListener.onListFragmentInteraction(mediaUri, !isAlreadyRunning, adapterPosition);
                }

            }
        };

        private final View.OnClickListener starListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mIsLoggedIn){
//                    Log.i(LOG_TAG, "onClick: click without login");
                    return;
                }
                CheckBox checkBox = (CheckBox) v;
                boolean isChecked = checkBox.isChecked();
//                Log.i(LOG_TAG, "onClickStar: Set to " + isChecked + " for the star at " + getAdapterPosition());
                if (isChecked)
                    mSpecificUserDataReference.child((String) mIdView.getText()).setValue(getAdapterPosition());
                else
                    mSpecificUserDataReference.child((String) mIdView.getText()).setValue(null);

            }
        };

        ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.music_id);
            mTitleView = (TextView) view.findViewById(R.id.music_title);
            mArtistView = (TextView) view.findViewById(R.id.music_artist);
            mButton = (ImageView) view.findViewById(R.id.button);
            mStar = ((CheckBox) mView.findViewById(checkBox));
            mView.setOnClickListener(mainListener);
            mStar.setOnClickListener(starListener);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}
