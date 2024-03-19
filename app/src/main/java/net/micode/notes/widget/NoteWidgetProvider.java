/*
 * 注意：此代码段的版权归 MiCode 开源社区所有（www.micode.net）
 * 本代码遵循 Apache 2.0 许可证，您可以在 http://www.apache.org/licenses/LICENSE-2.0 查看许可证内容。
 */

package net.micode.notes.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NoteEditActivity;
import net.micode.notes.ui.NotesListActivity;

/**
 * 笔记小部件提供者抽象类，扩展自AppWidgetProvider，用于管理和更新笔记小部件的内容。
 */
public abstract class NoteWidgetProvider extends AppWidgetProvider {
    // 查询笔记时用到的列名数组
    public static final String[] PROJECTION = new String[]{
            NoteColumns.ID,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.SNIPPET
    };

    // 列的索引常量
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_BG_COLOR_ID = 1;
    public static final int COLUMN_SNIPPET = 2;

    // 日志标签
    private static final String TAG = "NoteWidgetProvider";

    /**
     * 当小部件被删除时调用，更新数据库中对应小部件的ID为无效ID。
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        for (int i = 0; i < appWidgetIds.length; i++) {
            context.getContentResolver().update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[]{String.valueOf(appWidgetIds[i])});
        }
    }

    /**
     * 根据小部件ID查询对应的笔记信息。
     *
     * @param context  上下文
     * @param widgetId 小部件ID
     * @return 返回查询到的Cursor对象，包含笔记的摘要、背景ID等信息。
     */
    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        return context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[]{String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);
    }

    /**
     * 更新小部件显示内容的通用方法。
     *
     * @param context          上下文
     * @param appWidgetManager AppWidget管理器
     * @param appWidgetIds     小部件ID数组
     */
    protected void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, false);
    }

    /**
     * 根据是否隐私模式更新小部件显示内容。
     *
     * @param context          上下文
     * @param appWidgetManager AppWidget管理器
     * @param appWidgetIds     小部件ID数组
     * @param privacyMode      是否为隐私模式
     */
    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
                        boolean privacyMode) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            if (appWidgetIds[i] != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int bgId = ResourceParser.getDefaultBgId(context);
                String snippet = "";
                Intent intent = new Intent(context, NoteEditActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_ID, appWidgetIds[i]);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());

                Cursor c = getNoteWidgetInfo(context, appWidgetIds[i]);
                if (c != null && c.moveToFirst()) {
                    if (c.getCount() > 1) {
                        Log.e(TAG, "Multiple message with same widget id:" + appWidgetIds[i]);
                        c.close();
                        return;
                    }
                    snippet = c.getString(COLUMN_SNIPPET);
                    bgId = c.getInt(COLUMN_BG_COLOR_ID);
                    intent.putExtra(Intent.EXTRA_UID, c.getLong(COLUMN_ID));
                    intent.setAction(Intent.ACTION_VIEW);
                } else {
                    snippet = context.getResources().getString(R.string.widget_havenot_content);
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                }

                if (c != null) {
                    c.close();
                }

                RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
                rv.setImageViewResource(R.id.widget_bg_image, getBgResourceId(bgId));
                intent.putExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, bgId);

                // 为小部件的点击事件设置PendingIntent
                PendingIntent pendingIntent = null;
                if (privacyMode) {
                    rv.setTextViewText(R.id.widget_text,
                            context.getString(R.string.widget_under_visit_mode));
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], new Intent(
                            context, NotesListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                } else {
                    rv.setTextViewText(R.id.widget_text, snippet);
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }

                rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    /**
     * 获取背景资源的ID。
     *
     * @param bgId 背景ID
     * @return 返回对应的资源ID
     */
    protected abstract int getBgResourceId(int bgId);

    /**
     * 获取小部件布局的ID。
     *
     * @return 返回布局的资源ID
     */
    protected abstract int getLayoutId();

    /**
     * 获取小部件的类型。
     *
     * @return 返回小部件的类型
     */
    protected abstract int getWidgetType();
}

