/*
 * FoldersListAdapter 类
 * 用于管理和适配文件夹列表的适配器，继承自 CursorAdapter。
 */

package net.micode.notes.ui;

// 导入相关类

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;


public class FoldersListAdapter extends CursorAdapter {
    // 查询时使用的列名数组
    public static final String[] PROJECTION = {
            NoteColumns.ID,
            NoteColumns.SNIPPET
    };

    // 列名数组中的索引常量
    public static final int ID_COLUMN = 0;
    public static final int NAME_COLUMN = 1;

    /*
     * 构造函数
     * @param context 上下文对象，通常指Activity或Application对象。
     * @param c 数据源游标Cursor。
     */
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    /*
     * 创建新的列表项View
     * @param context 上下文对象。
     * @param cursor 当前数据项的游标。
     * @param parent 视图的父容器。
     * @return 返回新创建的列表项View。
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new FolderListItem(context);
    }

    /*
     * 绑定数据到已有的View上
     * @param view 要绑定数据的视图。
     * @param context 上下文对象。
     * @param cursor 当前数据项的游标。
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof FolderListItem) {
            // 根据文件夹ID判断是根文件夹还是普通文件夹，并设置文件夹名称
            String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                    .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
            ((FolderListItem) view).bind(folderName);
        }
    }

    /*
     * 根据位置获取文件夹名称
     * @param context 上下文对象。
     * @param position 列表中的位置。
     * @return 返回该位置上文件夹的名称。
     */
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position);
        return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) ? context
                .getString(R.string.menu_move_parent_folder) : cursor.getString(NAME_COLUMN);
    }

    /*
     * FolderListItem 内部类
     * 用于表示文件夹列表中的一个项的视图类。
     */
    private class FolderListItem extends LinearLayout {
        private TextView mName; // 文件夹名称的文本视图

        /*
         * 构造函数
         * @param context 上下文对象。
         */
        public FolderListItem(Context context) {
            super(context);
            // 加载布局文件，并将自己作为根视图
            inflate(context, R.layout.folder_list_item, this);
            mName = (TextView) findViewById(R.id.tv_folder_name); // 获取文件夹名称的视图
        }

        /*
         * 绑定数据到视图上
         * @param name 要显示的文件夹名称。
         */
        public void bind(String name) {
            mName.setText(name);
        }
    }

}
