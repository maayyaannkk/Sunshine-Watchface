package com.example.android.sunshine.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Mayank on 18-11-2016.
 */

public class SunshineWatchfaceService extends CanvasWatchFaceService {

    private String TAG = "SunshineWatchfaceServic";
    private static final Typeface BASE_TYPEFACE = Typeface.SANS_SERIF;
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(BASE_TYPEFACE, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(BASE_TYPEFACE, Typeface.BOLD);

    @Override
    public Engine onCreateEngine() {
        return new SunshineEngine();
    }

    private class SunshineEngine extends Engine
            implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        GoogleApiClient mGoogleApiClient;

        //format time
        SimpleDateFormat dateFormat;
        SimpleDateFormat hourFormat;
        SimpleDateFormat minuteFormat;

        // background paint objects
        Paint mBackgroundPaint;

        // text paint objects
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mDatePaint;
        Paint mDateAmbientPaint;
        Paint mMaxTempPaint;
        Paint mMinTempPaint;

        // horizontal divider paint object
        Paint mDividerPaint;

        int mWeatherId = 0;
        double mMaxTemperature = 0;
        double mMinTemperature = 0;

        // bitmaps
        Bitmap mBitmapStatus;
        Bitmap mBitmapClear;
        Bitmap mBitmapClouds;
        Bitmap mBitmapFog;
        Bitmap mBitmapLightClouds;
        Bitmap mBitmapLightRain;
        Bitmap mBitmapRain;
        Bitmap mBitmapSnow;
        Bitmap mBitmapStorm;
        Bitmap mBitmapClearAmbient;
        Bitmap mBitmapCloudsAmbient;
        Bitmap mBitmapFogAmbient;
        Bitmap mBitmapLightCloudsAmbient;
        Bitmap mBitmapLightRainAmbient;
        Bitmap mBitmapRainAmbient;
        Bitmap mBitmapSnowAmbient;
        Bitmap mBitmapStormAmbient;

        //ambient value
        boolean mAmbient;

        //for data api change listeners
        private static final String REQ_PATH = "/weather";
        private static final String REQ_WEATHER_PATH = "/weather-req";
        private static final String KEY_WEATHER_ID = "com.example.key.weather_id";
        private static final String KEY_TEMP_MAX = "com.example.key.max_temp";
        private static final String KEY_TEMP_MIN = "com.example.key.min_temp";

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchfaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SunshineWatchfaceService.this.getResources();
            initializePaints(resources);
            initializeBitmaps(resources);

            dateFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
            hourFormat = new SimpleDateFormat("HH:", Locale.getDefault());
            minuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

            mAmbient = false;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            int upperOffset = Math.round(bounds.height() * 0.075f);
            int lowerOffset = Math.round(bounds.height() * 0.025f);

            long time = System.currentTimeMillis();

            Rect hourBounds = new Rect();
            String hourText = hourFormat.format(time);
            int hourX, hourY;

            Rect minuteBounds = new Rect();
            String minuteText = minuteFormat.format(time);
            int minuteX, minuteY;

            Rect dateBounds = new Rect();
            String dateText = dateFormat.format(time);
            int dateX, dateY;

            // The hour will be drawn above the middle of screen to the left of center
            mHourPaint.getTextBounds(hourText, 0, hourText.length(), hourBounds);
            hourX = Math.abs(bounds.centerX() - hourBounds.width());
            hourY = Math.round((Math.abs(bounds.centerY() / 2)) - (bounds.height() * 0.02f)) + upperOffset;
            canvas.drawText(hourText, hourX, hourY, mHourPaint);

            // The minute will be drawn above the middle of screen to the right of center
            mMinutePaint.getTextBounds(minuteText, 0, minuteText.length(), minuteBounds);
            minuteX = Math.abs(bounds.centerX() + Math.round(minuteBounds.width() * 0.15f));
            minuteY = Math.round((Math.abs(bounds.centerY() / 2)) - (bounds.height() * 0.02f)) + upperOffset;
            canvas.drawText(minuteText, minuteX, minuteY, mMinutePaint);

            // The date will be displayed below the time
            if (mAmbient) {
                mDateAmbientPaint.getTextBounds(dateText, 0, dateText.length(), dateBounds);
                dateX = Math.abs(bounds.centerX() - dateBounds.centerX());
                dateY = Math.round((bounds.centerY() / 2 + dateBounds.height()) + (bounds.height() * 0.02f)) + upperOffset;
                canvas.drawText(dateText, dateX, dateY, mDateAmbientPaint);
            } else {
                mDatePaint.getTextBounds(dateText, 0, dateText.length(), dateBounds);
                dateX = Math.abs(bounds.centerX() - dateBounds.centerX());
                dateY = Math.round((bounds.centerY() / 2 + dateBounds.height()) + (bounds.height() * 0.02f)) + upperOffset;
                canvas.drawText(dateText, dateX, dateY, mDatePaint);
            }

            // The horizontal divider will be drawn at center
            canvas.drawLine(Math.abs(bounds.centerX() - (bounds.width() - (bounds.width() * 0.10f)))
                    , bounds.height() / 2
                    , Math.abs(bounds.centerX() + (bounds.width() * 0.10f))
                    , bounds.height() / 2
                    , mDividerPaint);

            canvas.drawBitmap(getBitmapForWeatherCondition(mWeatherId), bounds.width() / 5, bounds.height() / 2 + lowerOffset, null);

            Rect highBounds = new Rect();
            String highText = String.format(getResources().getString(R.string.format_temperature), mMaxTemperature);
            int highY;

            Rect lowBounds = new Rect();
            String lowText = String.format(getResources().getString(R.string.format_temperature), mMinTemperature);
            int lowY;

            // The temperature high
            mMaxTempPaint.getTextBounds(highText, 0, highText.length(), highBounds);
            highY = highBounds.height();

            // The temperature low
            mMinTempPaint.getTextBounds(lowText, 0, lowText.length(), lowBounds);
            lowY = lowBounds.height();

            //We draw high temperature value
            canvas.drawText(highText, bounds.width() * 4 / 9, bounds.height() / 2 + lowerOffset * 3 + highY, mMaxTempPaint);

            //We draw high temperature value
            canvas.drawText(lowText, bounds.width() * 5 / 8, bounds.height() / 2 + lowerOffset * 3 + lowY, mMinTempPaint);
        }

        private void requestWeatherUpdate() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(REQ_WEATHER_PATH);

            putDataMapRequest.getDataMap().putLong("time", System.currentTimeMillis());

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e("TAG", "Failed to send step count");
                            } else {
                                Log.e("TAG", "Successfully sent step count");
                            }
                        }
                    });
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            invalidate();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                Log.e(TAG, "visible");

                if (null == mGoogleApiClient) {
                    Log.e(TAG, "mGoogleApiClient.build()");

                    mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchfaceService.this)
                            .addConnectionCallbacks(this)
                            .addApi(Wearable.API)
                            .build();
                }

                if (!mGoogleApiClient.isConnected()) {
                    Log.e(TAG, "mGoogleApiClient.connect()");

                    mGoogleApiClient.connect();
                }
            } else {
                Log.d(TAG, "invisible");

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Log.d(TAG, "Wearable.DataApi.removeListener()");
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);

                    Log.d(TAG, "mGoogleApiClient.disconnect()");
                    mGoogleApiClient.disconnect();
                }
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.e(TAG, "onConnected");

            Log.e(TAG, "Wearable.DataApi.addListener()");
            Wearable.DataApi.addListener(mGoogleApiClient, this);

            // request a weather update from the app
            requestWeatherUpdate();

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, connectionResult.toString());
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            Log.e(TAG, "onDataChanged");
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    String path = item.getUri().getPath();
                    Log.d(TAG, "path: " + path);

                    if (path.equals(REQ_PATH)) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                        mWeatherId = dataMap.getInt(KEY_WEATHER_ID);
                        mMaxTemperature = dataMap.getDouble(KEY_TEMP_MAX);
                        mMinTemperature = dataMap.getDouble(KEY_TEMP_MIN);

                        invalidate();
                    }
                }
            }

            dataEventBuffer.release();

        }

        private void initializeBitmaps(Resources resources) {
            mBitmapStatus = BitmapFactory.decodeResource(resources, R.drawable.ic_status);
            mBitmapClear = BitmapFactory.decodeResource(resources, R.drawable.ic_clear);
            mBitmapClouds = BitmapFactory.decodeResource(resources, R.drawable.ic_cloudy);
            mBitmapFog = BitmapFactory.decodeResource(resources, R.drawable.ic_fog);
            mBitmapLightClouds = BitmapFactory.decodeResource(resources, R.drawable.ic_light_clouds);
            mBitmapLightRain = BitmapFactory.decodeResource(resources, R.drawable.ic_light_rain);
            mBitmapRain = BitmapFactory.decodeResource(resources, R.drawable.ic_rain);
            mBitmapSnow = BitmapFactory.decodeResource(resources, R.drawable.ic_snow);
            mBitmapStorm = BitmapFactory.decodeResource(resources, R.drawable.ic_storm);
            mBitmapClearAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_clear_grayscale);
            mBitmapCloudsAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_cloudy_grayscale);
            mBitmapFogAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_fog_grayscale);
            mBitmapLightCloudsAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_light_clouds_grayscale);
            mBitmapLightRainAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_light_rain_grayscale);
            mBitmapRainAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_rain_grayscale);
            mBitmapSnowAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_snow_grayscale);
            mBitmapStormAmbient = BitmapFactory.decodeResource(resources, R.drawable.ic_storm_grayscale);
        }

        private void initializePaints(Resources resources) {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mHourPaint = new TextPaint();
            mHourPaint.setColor(Color.WHITE);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setTypeface(BOLD_TYPEFACE);
            mHourPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics()));

            mMinutePaint = new TextPaint();
            mMinutePaint.setColor(Color.WHITE);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setTypeface(NORMAL_TYPEFACE);
            mMinutePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics()));

            mDatePaint = new TextPaint();
            mDatePaint.setColor(resources.getColor(R.color.primary_light));
            mDatePaint.setAntiAlias(true);
            mDatePaint.setTypeface(NORMAL_TYPEFACE);
            mDatePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics()));

            mDateAmbientPaint = new TextPaint();
            mDateAmbientPaint.setColor(Color.WHITE);
            mDateAmbientPaint.setAntiAlias(true);
            mDateAmbientPaint.setTypeface(NORMAL_TYPEFACE);
            mDateAmbientPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics()));


            mDividerPaint = new TextPaint();
            mDividerPaint.setColor(Color.WHITE);
            mDividerPaint.setAntiAlias(true);
            mDividerPaint.setTypeface(NORMAL_TYPEFACE);
            mDividerPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));

            mMaxTempPaint = new TextPaint();
            mMaxTempPaint.setColor(Color.WHITE);
            mMaxTempPaint.setAntiAlias(true);
            mMaxTempPaint.setTypeface(BOLD_TYPEFACE);
            mMaxTempPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));

            mMinTempPaint = new TextPaint();
            mMinTempPaint.setColor(Color.WHITE);
            mMinTempPaint.setAntiAlias(true);
            mMinTempPaint.setTypeface(NORMAL_TYPEFACE);
            mMinTempPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));

        }

        private Bitmap getBitmapForWeatherCondition(int weatherId) {

            if (weatherId >= 200 && weatherId <= 232) {
                return mAmbient ? mBitmapStormAmbient : mBitmapStorm;
            } else if (weatherId >= 300 && weatherId <= 321) {
                return mAmbient ? mBitmapLightRainAmbient : mBitmapLightRain;
            } else if (weatherId >= 500 && weatherId <= 504) {
                return mAmbient ? mBitmapRainAmbient : mBitmapRain;
            } else if (weatherId == 511) {
                return mAmbient ? mBitmapSnowAmbient : mBitmapSnow;
            } else if (weatherId >= 520 && weatherId <= 531) {
                return mAmbient ? mBitmapRainAmbient : mBitmapRain;
            } else if (weatherId >= 600 && weatherId <= 622) {
                return mAmbient ? mBitmapSnowAmbient : mBitmapSnow;
            } else if (weatherId >= 701 && weatherId <= 761) {
                return mAmbient ? mBitmapFogAmbient : mBitmapFog;
            } else if (weatherId == 761 || weatherId == 781) {
                return mAmbient ? mBitmapStormAmbient : mBitmapStorm;
            } else if (weatherId == 800) {
                return mAmbient ? mBitmapClearAmbient : mBitmapClear;
            } else if (weatherId == 801) {
                return mAmbient ? mBitmapLightCloudsAmbient : mBitmapLightClouds;
            } else if (weatherId >= 802 && weatherId <= 804) {
                return mAmbient ? mBitmapCloudsAmbient : mBitmapClouds;
            }

            // default bitmap
            return mBitmapStatus;
        }


    }
}
