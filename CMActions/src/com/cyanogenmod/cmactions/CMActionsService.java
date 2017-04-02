/*
 * Copyright (c) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.cmactions;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

public class CMActionsService extends Service {
    private static final String TAG = "CMActionsService";
    private static final boolean DEBUG = false;

    private static final int POCKET_DELTA_NS = 1000 * 1000 * 1000;

    private Context mContext;
    private DiorProximitySensor mSensor;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private boolean mHandwaveGestureEnabled = false;
    private boolean mPocketGestureEnabled = false;

    class DiorProximitySensor implements SensorEventListener {
        private SensorManager mSensorManager;
        private Sensor mProxSensor;
        private boolean mProxEnabled = false;

        private boolean mSawNear = false;
        private long mInPocketTime = 0;

        public DiorProximitySensor(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mProxSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            boolean isNear = event.values[0] < mProxSensor.getMaximumRange();
            if (mSawNear && !isNear) {
                if (shouldPulse(event.timestamp)) {
                    launchDozePulse();
                }
            } else {
                mInPocketTime = event.timestamp;
            }
            mSawNear = isNear;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /* Empty */
        }

        private boolean shouldPulse(long timestamp) {
            long delta = timestamp - mInPocketTime;

            if (mHandwaveGestureEnabled && mPocketGestureEnabled) {
                return true;
            } else if (mHandwaveGestureEnabled) {
                return delta < POCKET_DELTA_NS;
            } else if (mPocketGestureEnabled) {
                return delta >= POCKET_DELTA_NS;
            }

            return false;
        }

        private void setProxEnabled(boolean enable) {
            if (mProxEnabled == enable) {
                return;
            }
            mProxEnabled = enable;
            if (enable) {
                mSensorManager.registerListener(this, mProxSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                mSensorManager.unregisterListener(this, mProxSensor);
            }
        }
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "CMActionsService Started");
        mContext = this;
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mSensor = new DiorProximitySensor(mContext);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CMActionsWakeLock");

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mHandwaveGestureEnabled =
                sharedPrefs.getBoolean(Constants.PREF_GESTURE_HAND_WAVE_KEY, false);
        mPocketGestureEnabled = sharedPrefs.getBoolean(Constants.PREF_GESTURE_POCKET_KEY, false);
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        if (!isInteractive() && areGesturesEnabled()) {
            mSensor.setProxEnabled(true);
        }
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "CMActionsService Stopped");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        mContext.unregisterReceiver(mScreenStateReceiver);
        mSensor.setProxEnabled(false);
        holdWakelock(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchDozePulse() {
        mContext.sendBroadcastAsUser(new Intent(Constants.DOZE_INTENT), UserHandle.CURRENT);
    }

    private boolean isInteractive() {
        return mPowerManager.isInteractive();
    }

    private boolean areGesturesEnabled() {
        return (mHandwaveGestureEnabled || mPocketGestureEnabled) &&
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.DOZE_ENABLED, 1) != 0;
    }

    private void holdWakelock(boolean hold) {
        if (DEBUG) Log.d(TAG, "hold=" + hold + ", held=" + mWakeLock.isHeld());
        if (hold == mWakeLock.isHeld()) {
            return;
        }
        if (hold) {
            mWakeLock.acquire();
        } else {
            mWakeLock.release();
        }
    }

    private void onDisplayOn() {
        if (DEBUG) Log.d(TAG, "Display on");
        mSensor.setProxEnabled(false);
        if (areGesturesEnabled()) {
            holdWakelock(true);
        }
    }

    private void onDisplayOff() {
        if (DEBUG) Log.d(TAG, "Display off");
        if (areGesturesEnabled()) {
            mSensor.setProxEnabled(true);
        }
        holdWakelock(false);
    }

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            }
        }
    };

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                        String key) {
                    if (Constants.PREF_GESTURE_HAND_WAVE_KEY.equals(key)) {
                        mHandwaveGestureEnabled = sharedPreferences.getBoolean(
                                Constants.PREF_GESTURE_HAND_WAVE_KEY, false);
                    } else if (Constants.PREF_GESTURE_POCKET_KEY.equals(key)) {
                        mPocketGestureEnabled = sharedPreferences
                                .getBoolean(Constants.PREF_GESTURE_POCKET_KEY, false);
                    }

                    if (areGesturesEnabled()) {
                        holdWakelock(true);
                    }
                }
            };
}
