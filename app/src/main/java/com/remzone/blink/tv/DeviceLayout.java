package com.remzone.blink.tv;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

final class DeviceLayout {
    static boolean isTV(Context context){UiModeManager mode=context.getSystemService(UiModeManager.class);return mode!=null&&mode.getCurrentModeType()==Configuration.UI_MODE_TYPE_TELEVISION;}
    static boolean twoColumns(int widthDp){return widthDp>=700;}
}
