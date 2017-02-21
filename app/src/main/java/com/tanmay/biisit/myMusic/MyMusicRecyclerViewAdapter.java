package com.tanmay.biisit.myMusic;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tanmay.biisit.R;
import com.tanmay.biisit.dummy.DummyContent.DummyItem;
import com.tanmay.biisit.myMusic.interfaces.OnListFragmentInteractionListener;

//import com.tanmay.biisit.myMusic.MyMusicFragment.OnListFragmentInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyMusicRecyclerViewAdapter extends RecyclerView.Adapter<MyMusicRecyclerViewAdapter.ViewHolder> {

    private static final String LOG_TAG = MyMusicRecyclerViewAdapter.class.getSimpleName();
    private final Cursor mValues;
    private final Context mContext;
    private final int mTitleColumn;
    private final int mIdColumn;
    private final int mArtistColumn;
    private final OnListFragmentInteractionListener mListener;
    private int mSelectedPosition = -1;
    private View mSelectedView;

//    public MyMusicRecyclerViewAdapter(List<DummyItem> items) {
//        mValues = items;
////        mListener = listener;
//    }

    public MyMusicRecyclerViewAdapter(Context context, OnListFragmentInteractionListener listener, Cursor data) {
        mContext = context;
        mValues = data;
        mListener = listener;

        if (mValues != null && mValues.moveToFirst()) {
            mTitleColumn = mValues.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            mIdColumn = mValues.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            mArtistColumn = mValues.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
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
                .inflate(R.layout.fragment_mymusic_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
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
        }
        else
            holder.mView.setSelected(false);
//        holder.mView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (null != mListener) {
//                    // Notify the active callbacks interface (the activity, if the
//                    // fragment is attached to one) that an item has been selected.
//
//                }
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return (mValues != null && mValues.moveToFirst()) ? mValues.getCount() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        public final TextView mIdView;
        public final TextView mTitleView;
        public final TextView mArtistView;
        public final ImageView mButton;
//        public DummyItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.music_id);
            mTitleView = (TextView) view.findViewById(R.id.music_title);
            mArtistView = (TextView) view.findViewById(R.id.music_artist);
            mButton = (ImageView) view.findViewById(R.id.button);
            mView.setOnClickListener(this);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mValues.moveToPosition(adapterPosition);
            Log.i(LOG_TAG, "onClick: position " + adapterPosition);
            boolean isRunning = mView.isSelected();
            if (mSelectedView != null)
                ((ImageView)mSelectedView.findViewById(R.id.button)).setImageResource(R.drawable.ic_play);
//                ((ImageView)mSelectedView.findViewById(R.id.button)).setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_play));
            if (!isRunning){
                if (mSelectedView != null) {
                    mSelectedView.setSelected(false);
                }
                mSelectedPosition = adapterPosition;
                mSelectedView = mView;
            }
            else {
                mSelectedPosition = -1;
                mSelectedView = null;
            }
            mView.setSelected(!isRunning);
            mButton.setImageResource(
                    !isRunning ? R.drawable.ic_pause : R.drawable.ic_play
            );
//            mButton.setBackground(
//                    ContextCompat.getDrawable(mContext, !isRunning ? R.drawable.ic_pause : R.drawable.ic_play)
//            );
            mListener.onListFragmentInteraction(mValues.getString(mTitleColumn), !isRunning);

        }
    }
}
