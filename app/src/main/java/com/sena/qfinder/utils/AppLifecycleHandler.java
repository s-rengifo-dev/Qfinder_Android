package com.sena.qfinder.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class AppLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static int activityCount = 0;
    private static boolean isInForeground = false;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        if (activityCount == 0) {
            isInForeground = true;
        }
        activityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            isInForeground = false;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    public static boolean isApplicationInForeground() {
        return isInForeground;
    }
}