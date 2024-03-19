/*
 * GTaskASyncTask 类说明:
 * 这是一个继承自AsyncTask的类，用于在后台执行Google任务同步操作。它可以在一个独立的线程中执行同步任务，并通过通知栏通知用户同步的状态（成功、失败、取消等）。
 * 同时，它提供了接口供调用者监听同步任务的完成。
 */
package net.micode.notes.gtask.remote;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;


public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {

    // 同步通知的唯一ID
    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    // 定义完成监听器接口
    public interface OnCompleteListener {
        void onComplete();
    }

    private Context mContext; // 上下文对象，用于访问应用资源和通知管理器
    private NotificationManager mNotifiManager; // 通知管理器
    private GTaskManager mTaskManager; // Google任务管理器，用于执行实际的同步操作
    private OnCompleteListener mOnCompleteListener; // 同步完成的监听器

    /*
     * 构造函数
     * @param context 应用的上下文环境
     * @param listener 同步完成时的监听器
     */
    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    // 取消同步操作的方法
    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    // 发布进度更新的方法
    public void publishProgess(String message) {
        publishProgress(new String[]{
                message
        });
    }

    /*
     * 显示通知的方法
     * @param tickerId 通知的Ticker文本资源ID
     * @param content 通知的内容文本
     */
    private void showNotification(int tickerId, String content) {
        PendingIntent pendingIntent;
        // 根据不同的通知状态设置不同的Intent
        if (tickerId != R.string.ticker_success) {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), 0);

        } else {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), 0);
        }

        // 构建通知并显示
        Notification.Builder builder = new Notification.Builder(mContext)
                .setAutoCancel(true)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);
        Notification notification = builder.getNotification();
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
    }

    /*
     * 在后台执行同步操作的方法
     * @return 同步操作的状态码
     */
    @Override
    protected Integer doInBackground(Void... unused) {
        // 开始同步时的进度更新
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));
        return mTaskManager.sync(mContext, this);
    }

    /*
     * 更新进度的方法，会在调用publishProgress后被调用
     * @param progress 进度更新的内容
     */
    @Override
    protected void onProgressUpdate(String... progress) {
        // 显示当前同步进度
        showNotification(R.string.ticker_syncing, progress[0]);
        // 如果上下文是一个GTaskSyncService实例，发送广播更新进度
        if (mContext instanceof GTaskSyncService) {
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    /*
     * 同步任务完成后的处理方法
     * @param result 同步操作的状态码
     */
    @Override
    protected void onPostExecute(Integer result) {
        // 根据不同的状态显示不同的通知
        if (result == GTaskManager.STATE_SUCCESS) {
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }
        // 如果设置了完成监听器，则在一个新线程中调用其onComplete方法
        if (mOnCompleteListener != null) {
            new Thread(new Runnable() {

                public void run() {
                    mOnCompleteListener.onComplete();
                }
            }).start();
        }
    }
}
