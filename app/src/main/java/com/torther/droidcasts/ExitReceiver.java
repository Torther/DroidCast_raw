package com.torther.droidcasts;

import static android.content.Context.NOTIFICATION_SERVICE;

import static com.torther.droidcasts.DroidCastSService.NOTICE_ID;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra("exit", true)) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTICE_ID);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}