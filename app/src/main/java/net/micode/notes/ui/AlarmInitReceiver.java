/*
 * 该类是广播接收器，用于在应用启动时初始化提醒设置。
 * 当系统启动时，它会检查数据库中所有设置了提醒的笔记，并为每个笔记设置相应的提醒。
 */
package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;


public class AlarmInitReceiver extends BroadcastReceiver {

    // 查询笔记时需要的列
    private static final String[] PROJECTION = new String[]{
            NoteColumns.ID,
            NoteColumns.ALERTED_DATE
    };

    // 列的索引
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_ALERTED_DATE = 1;

    /**
     * 当接收到广播时执行的操作。主要用于设置所有已记录的提醒时间。
     *
     * @param context 上下文，提供访问应用全局功能的入口。
     * @param intent  携带了触发该接收器的广播信息。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取当前日期和时间
        long currentDate = System.currentTimeMillis();
        // 查询数据库中所有需要提醒的笔记
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[]{String.valueOf(currentDate)},
                null);

        if (c != null) {
            // 遍历查询结果，为每个需要提醒的笔记设置提醒
            if (c.moveToFirst()) {
                do {
                    // 获取提醒日期
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    // 创建Intent，用于在提醒时间触发AlarmReceiver
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    // 创建PendingIntent，它是一个延迟的意图，可以在特定时间由系统触发
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    // 获取AlarmManager服务，用于设置提醒
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    // 设置提醒
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext());
            }
            // 关闭Cursor，释放资源
            c.close();
        }
    }
}

