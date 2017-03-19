package com.tanmay.biisit.soundCloud;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tanmay.biisit.R;
import com.tanmay.biisit.soundCloud.pojo.Track;

import java.util.List;

import static com.tanmay.biisit.NavigationDrawerActivity.getTimeString;


/**
 * {@link RecyclerView.Adapter} that can display a song from a cursor and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
class SoundCloudRecyclerViewAdapter extends RecyclerView.Adapter<SoundCloudRecyclerViewAdapter.ViewHolder> {

    private static final String LOG_TAG = SoundCloudRecyclerViewAdapter.class.getSimpleName();
    private final List<Track> mValues;
    private final Context mContext;
    private final OnListFragmentInteractionListener mListener;
    private int mSelectedPosition = -1;
    private View mSelectedView;


    SoundCloudRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener, List<Track> data) {
        mContext = context;
        mValues = data;
        mListener = listener;
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

        Track selectedTrack = mValues.get(position);
        holder.mDurationView.setText(
                getTimeString(selectedTrack.getDuration())
        );
        holder.mTitleView.setText(
                selectedTrack.getTitle()
        );
        holder.mArtistView.setText(
                selectedTrack.getUserName()
        );

        if (position == mSelectedPosition){
            holder.mView.setSelected(true);
            mSelectedView = holder.mView;
            holder.mButton.setImageResource(R.drawable.ic_pause);
            holder.mButton.setContentDescription(mContext.getString(R.string.tap_to_pause_message));
        }
        else {
            holder.mButton.setImageResource(R.drawable.ic_play);
            holder.mView.setSelected(false);
            holder.mButton.setContentDescription(mContext.getString(R.string.tap_to_play_message));
        }

    }

    @Override
    public int getItemCount() {
        if (mValues != null)
            return mValues.size();
        else
            return 0;
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
            ImageView button = (ImageView)mSelectedView.findViewById(R.id.button);
            button.setImageResource(R.drawable.ic_play);
            button.setContentDescription(mContext.getString(R.string.tap_to_play_message));
        }
        mSelectedPosition = -1;
    }

//    Uri getUriAtPos(int position){
//        return Uri.parse(mValues.get(position).getStreamURL());
//        return mediaUri;

//    }

    Track getTrackAtPos(int position){
        return mValues.get(position);
    }

    interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Track trackToPlay, boolean start, int position);
//        void onStarToggled();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mTitleView;
        final TextView mArtistView;
        final ImageView mButton;
        final TextView mDurationView;
        private final View.OnClickListener mainListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = getAdapterPosition();
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
//                Uri mediaUri= Uri.parse(mValues.get(adapterPosition).getStreamURL());
//                    Log.i(LOG_TAG, "onClick: telling fragment about click at " + adapterPosition);
                mListener.onListFragmentInteraction(mValues.get(adapterPosition), !isAlreadyRunning, adapterPosition);

            }
        };

       ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.music_title);
            mArtistView = (TextView) view.findViewById(R.id.music_artist);
            mDurationView = (TextView) view.findViewById(R.id.music_duration);
            mButton = (ImageView) view.findViewById(R.id.button);
            mView.setOnClickListener(mainListener);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }
}
