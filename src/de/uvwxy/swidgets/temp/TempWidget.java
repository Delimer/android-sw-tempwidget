/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2011-2013, Sony Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB / Sony Mobile
 Communications AB nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.uvwxy.swidgets.temp;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.widget.Widget;
import com.sonyericsson.extras.liveware.extension.util.widget.BaseWidget;

import de.uvwxy.sensors.BarometerReader;
import de.uvwxy.swidgets.temp.R;
import de.uvwxy.units.Unit;

/**
 * This demonstrates how to implement a simple text widget.
 */
class TempWidget extends BaseWidget {
    public static final int WIDGET_WIDTH_CELLS = 1;
    public static final int WIDGET_HEIGHT_CELLS = 1;
    private static final int TOTAL_REFRESH_COUNT = 5;
    private static final long REFRESH_TIMEOUT_MILLIS = 1000;
    private int refreshCount = 0;
    private int longClickCount = 0;

    String unitTemp = "CELSIUS";
    String unitLength = "METRE";

    private TempLogic temp;
    private TempWidgetRegistrationInformation tempRegInfo;
    private OnSharedPreferenceChangeListener listener;
    private SharedPreferences prefs;

    /**
     * Creates a widget extension.
     */
    public TempWidget(WidgetBundle bundle) {
        super(bundle);

        temp = new TempLogic(mContext);
        tempRegInfo = new TempWidgetRegistrationInformation(mContext);

    }

    @Override
    public void onStartRefresh() {
        Log.d(TempWidgetExtensionService.LOG_TAG, "startRefresh");
        temp.loadValue();
        // user action, always continue with N refreshs directly
        restartRefreshLoop(0, true);

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        unitTemp = prefs.getString("baro_unit", "MILLI_BAR");
        unitLength = prefs.getString("length_unit", "METRE");

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                unitTemp = prefs.getString("baro_unit", "MILLI_BAR");
                unitLength = prefs.getString("length_unit", "METRE");
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onStopRefresh() {
        Log.d(TempWidgetExtensionService.LOG_TAG, "stopRefesh");
        temp.storeValue();
        cancelScheduledRefresh(tempRegInfo.getExtensionKey());
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onTouch(final int type, final int x, final int y) {
        Log.d(TempWidgetExtensionService.LOG_TAG, "onTouch() " + type + "/" + longClickCount);
        float baroMillis = temp.getBlockedValue();
        switch (type) {
        case 0:
            restartRefreshLoop(0, true);
            break;
        case 1:
            // nothing todo here
            break;
        }
    }

    private void restartRefreshLoop(long timeoutMillis, boolean restart) {
        if (restart) {
            refreshCount = 0;
        }
        long now = System.currentTimeMillis();
        String key = tempRegInfo.getExtensionKey();
        scheduleRefresh(now + timeoutMillis, key);
    }

    @Override
    public void onScheduledRefresh() {
        updateScreen();

        if (refreshCount < TOTAL_REFRESH_COUNT) {
            refreshCount++;
            // do not restart, always continue
            restartRefreshLoop(REFRESH_TIMEOUT_MILLIS, false);
        }
    }

    private void updateScreen() {
        Unit uLastValue = Unit.from(Unit.MILLI_BAR);
        float baroMillis = temp.getBlockedValue();
        uLastValue.setValue(baroMillis);

        Unit uLastHeight = Unit.from(Unit.METRE);
        if (temp.getValueRelative() > 0) {
            uLastHeight.setValue(BarometerReader.getHeightFromDiff(baroMillis, temp.getValueRelative()));
        } else {
            uLastHeight.setValue(BarometerReader.getHeight(baroMillis));
        }

        // Create a bundle with last read (pressue)
        Bundle bundleTemp = new Bundle();
        bundleTemp.putInt(Widget.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tvTemp);
        bundleTemp.putString(Control.Intents.EXTRA_TEXT, uLastValue.to(Unit.from(unitTemp)).toString());


        Bundle[] layoutData = new Bundle[] { bundleTemp };

        // Send a UI when the widget is visible.
        showLayout(R.layout.layout_widget, layoutData);
    }

    @Override
    public int getWidth() {
        return (int) (mContext.getResources().getDimension(R.dimen.smart_watch_2_widget_cell_width) * WIDGET_WIDTH_CELLS);
    }

    @Override
    public int getHeight() {
        return (int) (mContext.getResources().getDimension(R.dimen.smart_watch_2_widget_cell_height) * WIDGET_HEIGHT_CELLS);
    }

    @Override
    public int getPreviewUri() {
        return R.drawable.swidgets_baro;
    }

    @Override
    public int getName() {
        return R.string.extension_name;
    }
}
