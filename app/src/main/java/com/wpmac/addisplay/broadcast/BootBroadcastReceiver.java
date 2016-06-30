package com.wpmac.addisplay.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wpmac.addisplay.avtivity.LaunchActivity;

/**
 * Created by wpmac on 16/6/20.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
//        //启动应用，参数为需要自动启动的应用的包名
//        final String ACTION = "android.intent.action.BOOT_COMPLETED";
//        if (intent.getAction().equals(ACTION)) {
//            Intent mainActivityIntent = new Intent(context, LaunchActivity.class);  // 要启动的Activity
//            context.startActivity(mainActivityIntent);
//        }
//
//        if (paramIntent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
//        {
//            Log.d("BootReceiver", "system boot completed");
//            final Intent localIntent = new Intent(paramContext, LaunchActivity.class);
//            localIntent.setAction("android.intent.action.MAIN");
//            localIntent.addCategory("android.intent.category.LAUNCHER");
//            localIntent.setFlags(268435456);
//            new Timer().schedule(new TimerTask()
//                                 {
//                                     public void run()
//                                     {
//                                         paramContext.startActivity(localIntent);
//                                     }
//                                 }
//                    , 20000L);
//        }

        Intent bootIntent = new Intent(context,LaunchActivity.class);
        context.startActivity(bootIntent);

    }
}
