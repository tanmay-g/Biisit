package com.tanmay.biisit.myMusic;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.tanmay.biisit.R;
import com.tanmay.biisit.myMusic.interfaces.OnListFragmentInteractionListener;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {link OnListFragmentInteractionListener}
 * interface.
 */
public class MyMusicFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnListFragmentInteractionListener {

    private static final String LOG_TAG = MyMusicFragment.class.getSimpleName();
    //    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
//    private OnListFragmentInteractionListener mListener;
    private static final int CURSOR_LOADER_ID = 1;
    private RecyclerView mRecyclerView;

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView: Starting");
        View view = inflater.inflate(R.layout.fragment_mymusic_list, container, false);

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
    public void onListFragmentInteraction(String songName, boolean toStart) {
        String action = toStart? "Start" : "Stop";
        Toast.makeText(getActivity(), songName + " is to be " + action + "ed", Toast.LENGTH_SHORT).show();
    }


}

