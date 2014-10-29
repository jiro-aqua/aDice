package jp.sblo.pandora.adice;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SupportActionBar {

    public static void addBackButton(Activity activity)
    {
        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
