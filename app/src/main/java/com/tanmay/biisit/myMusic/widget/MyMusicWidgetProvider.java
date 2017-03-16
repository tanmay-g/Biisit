package com.tanmay.biisit.myMusic.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.tanmay.biisit.NavigationDrawerActivity;
import com.tanmay.biisit.R;

import static com.tanmay.biisit.MediaPlayerService.BROADCAST_CLIENT_ID_KEY;
import static com.tanmay.biisit.NavigationDrawerActivity.FRAGMENT_TO_LAUNCH;
import static com.tanmay.biisit.NavigationDrawerActivity.MY_MUSIC_FRAGMENT;
import static com.tanmay.biisit.myMusic.MyMusicFragment.MY_MUSIC_FRAGMENT_CLIENT_ID;

/**
 * Implementation of App Widget functionality.
 */
public class MyMusicWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = MyMusicWidgetProvider.class.getSimpleName();

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Intent intentToLaunchActivity = new Intent(context, NavigationDrawerActivity.class);
        intentToLaunchActivity.putExtra(FRAGMENT_TO_LAUNCH, MY_MUSIC_FRAGMENT);


        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.my_music_widget);

        views.setRemoteAdapter(R.id.widget_list, new Intent(context, MyMusicWidgetRemoteViewsService.class));
        views.setOnClickPendingIntent(R.id.widget, PendingIntent.getActivity(context, 0, intentToLaunchActivity, 0));

//        Intent serviceBroadcastIntent = new Intent(context, MediaPlayerService.class);
        Intent serviceBroadcastIntent = new Intent();
        serviceBroadcastIntent.putExtra(BROADCAST_CLIENT_ID_KEY, MY_MUSIC_FRAGMENT_CLIENT_ID);
        PendingIntent broadcastPendingIntent = PendingIntent.getBroadcast(context, 0, serviceBroadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);



        views.setPendingIntentTemplate(R.id.widget_list, broadcastPendingIntent);
        views.setEmptyView(R.id.widget_list, R.id.widget_empty);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
//        Log.i(LOG_TAG, "onUpdate: got call to update widget");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Bundle extras = intent.getExtras();
        if (extras == null || extras.getInt(BROADCAST_CLIENT_ID_KEY, -1) != MY_MUSIC_FRAGMENT_CLIENT_ID)
            return;
        Log.i(LOG_TAG, "onReceive: " + intent.getAction());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, getClass()));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
    }
}

