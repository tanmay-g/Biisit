package com.tanmay.biisit.myMusic.widget;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.tanmay.biisit.MediaPlayerService;
import com.tanmay.biisit.R;

import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ITEM_POS_KEY;
import static com.tanmay.biisit.MediaPlayerService.BROADCAST_MEDIA_URI_KEY;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_PAUSE;
import static com.tanmay.biisit.MediaPlayerService.SERVICE_ACTION_START_PLAY;
import static com.tanmay.biisit.myMusic.MyMusicFragment.MY_MUSIC_FRAGMENT_CLIENT_ID;

public class MyMusicWidgetRemoteViewsService extends RemoteViewsService {
    private static final String LOG_TAG = MyMusicWidgetRemoteViewsService.class.getSimpleName();

    public MyMusicWidgetRemoteViewsService() {
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private final String LOG_TAG = "RemoteViewsFactory";
            int mArtistColumn;
            int mIdColumn;
            int mTitleColumn;
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
//                Log.i(LOG_TAG, "onDataSetChanged: ");
                if (!MediaPlayerService.sIsRunning){
                    Intent serviceStartIntent = new Intent(getApplicationContext(), MediaPlayerService.class);
                    getApplicationContext().startService(serviceStartIntent);
                }
                if (data != null)
                    data.close();
                data = getContentResolver().query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (data != null) {
                    mTitleColumn = data.getColumnIndex
                            (MediaStore.Audio.Media.TITLE);
                    mIdColumn = data.getColumnIndex
                            (MediaStore.Audio.Media._ID);
                    mArtistColumn = data.getColumnIndex
                            (MediaStore.Audio.Media.ARTIST);
                }

            }

            @Override
            public void onDestroy() {
//                Log.i(LOG_TAG, "onDestroy: factory destroyed");
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
//                Log.i(LOG_TAG, "getViewAt: " + position);
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.my_music_widget_list_item);
                views.setTextViewText(
                        R.id.music_id,
                        String.valueOf((int) data.getLong(mIdColumn))
                );
                views.setTextViewText(
                        R.id.music_artist,
                        data.getString(mArtistColumn)
                );
                views.setTextViewText(
                        R.id.music_title,
                        data.getString(mTitleColumn)
                );

                String action;
                if (MediaPlayerService.sCurrentClient == MY_MUSIC_FRAGMENT_CLIENT_ID && MediaPlayerService.sIsPlaying && MediaPlayerService.sCurrentClientItemPos == position) {
                    views.setImageViewResource(R.id.button, R.drawable.ic_pause);
                    action = SERVICE_ACTION_PAUSE;
                }
                else{
                    views.setImageViewResource(R.id.button, R.drawable.ic_play);
                    action = SERVICE_ACTION_START_PLAY;
                }
                final Intent fillInIntent = new Intent();
                Uri mediaUri=
                        ContentUris
                                .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        data.getInt(mIdColumn));
                fillInIntent.setAction(action);
                fillInIntent.putExtra(BROADCAST_MEDIA_URI_KEY, mediaUri);
                fillInIntent.putExtra(BROADCAST_CLIENT_ITEM_POS_KEY, position);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.my_music_widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(mIdColumn);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}
