package com.example.android.sunshine.app.wearable;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Mayank on 18-11-2016.
 */

public class SunshineWearableListener extends WearableListenerService implements DataApi.DataListener{
    private static final String TAG = "WearableListenerService";

    private static final String REQ_WEATHER_PATH = "/weather-req";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        super.onDestroy();
    }
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);

        String path = messageEvent.getPath();
        Log.d(TAG, "message path: " + path);

        // Check to see if the message is a request for the updated weather conditions
        if (path.equals(REQ_WEATHER_PATH)) {
            // start the service sending the updated weather condition to the wearable
            Context context = this.getApplicationContext();
            context.startService(new Intent(context, WearableIntentService.class));
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.e(TAG,"data changed");
        for (DataEvent dataEvent : dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals(REQ_WEATHER_PATH)){
                    Log.e(TAG,"request for new data from watch"+dataMap.getDouble("time"));
                    Context context = this.getApplicationContext();
                    context.startService(new Intent(context, WearableIntentService.class));
                }
            }
        }
    }
}
