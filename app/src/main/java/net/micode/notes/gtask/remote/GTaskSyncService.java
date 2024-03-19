/*
 * GTaskSyncService类用于处理与Google任务同步相关的服务操作。
 */
package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class GTaskSyncService extends Service {
    // 同步操作的类型
    public final static String ACTION_STRING_NAME = "sync_action_type";

    // 启动同步
    public final static int ACTION_START_SYNC = 0;

    // 取消同步
    public final static int ACTION_CANCEL_SYNC = 1;

    // 无效操作
    public final static int ACTION_INVALID = 2;

    // 服务广播的名称
    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service";

    // 广播中是否正在同步的标志
    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing";

    // 广播中的同步进度消息
    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg";

    // 静态变量用于存储当前同步任务实例
    private static GTaskASyncTask mSyncTask = null;

    // 存储同步进度的字符串
    private static String mSyncProgress = "";

    /*
     * 启动同步任务。
     * 如果当前没有同步任务在执行，将创建一个新的同步任务并执行。
     */
    private void startSync() {
        if (mSyncTask == null) {
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() {
                public void onComplete() {
                    // 同步任务完成时的处理：重置静态变量，发送广播，停止服务
                    mSyncTask = null;
                    sendBroadcast("");
                    stopSelf();
                }
            });
            sendBroadcast("");
            mSyncTask.execute();
        }
    }

    /*
     * 取消当前的同步任务。
     */
    private void cancelSync() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync();
        }
    }

    /*
     * 服务创建时的初始化操作，重置同步任务为null。
     */
    @Override
    public void onCreate() {
        mSyncTask = null;
    }

    /*
     * 处理服务启动时的命令。
     * 根据传入的意图参数决定是启动同步还是取消同步。
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) {
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) {
                case ACTION_START_SYNC:
                    startSync();
                    break;
                case ACTION_CANCEL_SYNC:
                    cancelSync();
                    break;
                default:
                    break;
            }
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /*
     * 低内存时取消同步任务。
     */
    @Override
    public void onLowMemory() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync();
        }
    }

    // 服务绑定时返回null，此服务不提供绑定功能
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * 发送同步状态的广播。
     * 更新同步进度，并通过广播发送当前的同步状态和进度消息。
     */
    public void sendBroadcast(String msg) {
        mSyncProgress = msg;
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME);
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null);
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg);
        sendBroadcast(intent);
    }

    /*
     * 从Activity启动同步。
     * 设置活动上下文并启动同步服务。
     */
    public static void startSync(Activity activity) {
        GTaskManager.getInstance().setActivityContext(activity);
        Intent intent = new Intent(activity, GTaskSyncService.class);
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC);
        activity.startService(intent);
    }

    /*
     * 从Context取消同步。
     * 发送取消同步的意图到服务。
     */
    public static void cancelSync(Context context) {
        Intent intent = new Intent(context, GTaskSyncService.class);
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC);
        context.startService(intent);
    }

    /*
     * 检查是否正在同步。
     * 返回当前是否有一个同步任务在执行。
     */
    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    /*
     * 获取同步进度的字符串。
     * 返回当前同步任务的进度消息。
     */
    public static String getProgressString() {
        return mSyncProgress;
    }
}
