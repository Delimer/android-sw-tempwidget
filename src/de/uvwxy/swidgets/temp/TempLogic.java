package de.uvwxy.swidgets.temp;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import de.uvwxy.helper.IntentTools;
import de.uvwxy.sensors.BarometerReader;
import de.uvwxy.sensors.SensorReader.SensorResultCallback;

public class TempLogic {
    private static final String TEMP_WIDGET_SETTINGS = "TEMP_SETTINGS";
    private static final String VALUE = "VALUE";

    private Lock lock = new ReentrantLock();

    private SensorResultCallback cb = new SensorResultCallback() {

        @Override
        public void result(float[] f) {
            if (f != null && f.length > 0) {
                value = f[0];
                lock.unlock();
            }
        }
    };

    private BarometerReader baroReader;

    private float value = 0f;

    private Context mContext = null;

    public TempLogic(Context ctx) {
        this.mContext = ctx;
    }

    public void loadValue() {
        value = IntentTools.getSettings(mContext, TEMP_WIDGET_SETTINGS).getFloat(VALUE, 0);
        Log.d(TempWidgetExtensionService.LOG_TAG, "loaded " + value);
    }

    public void storeValue() {
        Editor e = IntentTools.getSettingsEditor(mContext, TEMP_WIDGET_SETTINGS);
        e.putFloat(VALUE, value);
        e.commit();
        Log.d(TempWidgetExtensionService.LOG_TAG, "stored " + value);
    }

    public float getBlockedValue() {
        if (baroReader == null) {
            // -2 stops reader after every start
            baroReader = new BarometerReader(mContext, -2, cb);
        }
        baroReader.startReading();
        Log.d(TempWidgetExtensionService.LOG_TAG, "locked for value");
        lock.tryLock();
        Log.d(TempWidgetExtensionService.LOG_TAG, "unlocked");

        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

}
