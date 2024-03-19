/*
 * AlarmReceiver类 - 用于处理闹钟广播接收
 * 当接收到闹钟相关的广播时，该类会启动一个指定的Activity
 *
 * extends BroadcastReceiver: 继承自Android的BroadcastReceiver类
 */
package net.micode.notes.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    /*
     * onReceive方法 - 系统调用的接收广播的方法
     * 当接收到广播时，该方法会被调用，然后启动AlarmAlertActivity
     *
     * @param context 上下文对象，提供了调用环境的信息
     * @param intent  包含广播的内容
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 设置Intent的类，以便启动AlarmAlertActivity
        intent.setClass(context, AlarmAlertActivity.class);
        // 添加标志，表示在一个新的任务中启动Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 根据设置的Intent启动Activity
        context.startActivity(intent);
    }
}
