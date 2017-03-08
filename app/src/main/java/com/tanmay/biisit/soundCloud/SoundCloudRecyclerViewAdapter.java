package com.tanmay.biisit.soundCloud;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tanmay.biisit.R;


/**
 * {@link RecyclerView.Adapter} that can display a song from a cursor and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
class SoundCloudRecyclerViewAdapter extends RecyclerView.Adapter<SoundCloudRecyclerViewAdapter.ViewHolder> {

    private static final String LOG_TAG = SoundCloudRecyclerViewAdapter.class.getSimpleName();
    private final Cursor mValues;
    private final Context mContext;
    private final int mTitleColumn;
    private final int mIdColumn;
    private final int mArtistColumn;
    private final OnListFragmentInteractionListener mListener;
    private int mSelectedPosition = -1;
    private View mSelectedView;


    SoundCloudRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener, Cursor data) {
        mContext = context;
        mValues = data;
        mListener = listener;

        if (mValues != null && mValues.moveToFirst()) {
            mTitleColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            mIdColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            mArtistColumn = mValues.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
        }
        else  {
            mTitleColumn = 0;
            mIdColumn = 0;
            mArtistColumn = 0;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_soundcloud_list_item, parent, false);
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

        if (position == mSelectedPosition){
            holder.mView.setSelected(true);
            mSelectedView = holder.mView;
            holder.mButton.setImageResource(R.drawable.ic_pause);
        }
        else
            holder.mView.setSelected(false);


    }

    @Override
    public int getItemCount() {
        return (mValues != null && mValues.moveToFirst()) ? mValues.getCount() : 0;
    }

    void selectItem(int position){
        mSelectedPosition = position;
        notifyItemChanged(mSelectedPosition);
    }

    void deselectCurrentItem(){
//        Log.i(LOG_TAG, "deselectCurrentItem: call received");
        unSelectSelectedView();
    }

    private void unSelectSelectedView(){
        if (mSelectedView != null) {
            mSelectedView.setSelected(false);
            ((ImageView)mSelectedView.findViewById(R.id.button)).setImageResource(R.drawable.ic_play);
        }
        mSelectedPosition = -1;
    }

    Uri getUriAtPos(int position){
        mValues.moveToPosition(position);
        Uri mediaUri=
                ContentUris
                        .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                mValues.getInt(mIdColumn));
        return mediaUri;

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
                Uri mediaUri=
                        ContentUris
                                .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        mValues.getInt(mIdColumn));
//                    Log.i(LOG_TAG, "onClick: telling fragment about click at " + adapterPosition);
                    mListener.onListFragmentInteraction(mediaUri, !isAlreadyRunning, adapterPosition);

            }
        };

       ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.music_id);
            mTitleView = (TextView) view.findViewById(R.id.music_title);
            mArtistView = (TextView) view.findViewById(R.id.music_artist);
            mButton = (ImageView) view.findViewById(R.id.button);
            mView.setOnClickListener(mainListener);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}