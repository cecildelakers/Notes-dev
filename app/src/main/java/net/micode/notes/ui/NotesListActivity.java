/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.tool.BackupUtils;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

public class NotesListActivity extends Activity implements OnClickListener, OnItemLongClickListener {
    // 定义文件夹中笔记列表查询的标记
    private static final int FOLDER_NOTE_LIST_QUERY_TOKEN = 0;

    // 定义文件夹列表查询的标记
    private static final int FOLDER_LIST_QUERY_TOKEN = 1;

    // 菜单中删除文件夹的选项
    private static final int MENU_FOLDER_DELETE = 0;

    // 菜单中查看文件夹的选项
    private static final int MENU_FOLDER_VIEW = 1;

    // 菜单中更改文件夹名称的选项
    private static final int MENU_FOLDER_CHANGE_NAME = 2;

    // 首次使用应用时，添加介绍信息的偏好设置键
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    // 列表编辑状态的枚举，包括笔记列表、子文件夹和通话记录文件夹
    private enum ListEditState {
        NOTE_LIST, SUB_FOLDER, CALL_RECORD_FOLDER
    }

    ;

    // 当前编辑状态
    private ListEditState mState;

    // 后台查询处理器
    private BackgroundQueryHandler mBackgroundQueryHandler;

    // 笔记列表的适配器
    private NotesListAdapter mNotesListAdapter;

    // 笔记列表视图
    private ListView mNotesListView;

    // 添加新笔记的按钮
    private Button mAddNewNote;

    // 是否分发事件的标志
    private boolean mDispatch;

    // 触摸点的原始Y坐标
    private int mOriginY;

    // 分发事件时的Y坐标
    private int mDispatchY;

    // 标题栏文本视图
    private TextView mTitleBar;

    // 当前文件夹的ID
    private long mCurrentFolderId;

    // 内容解析器
    private ContentResolver mContentResolver;

    // 模式回调接口
    private ModeCallback mModeCallBack;

    // 日志标签
    private static final String TAG = "NotesListActivity";

    // 笔记列表视图滚动速率
    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;

    // 聚焦的笔记数据项
    private NoteItemData mFocusNoteDataItem;

    // 普通文件夹选择条件
    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?";

    // 根文件夹选择条件
    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?)" + " OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)";

    // 打开节点请求代码
    private final static int REQUEST_CODE_OPEN_NODE = 102;
    // 新建节点请求代码
    private final static int REQUEST_CODE_NEW_NODE = 103;

    /**
     * 在活动创建时调用，用于初始化资源和设置应用信息。
     *
     * @param savedInstanceState 如果活动之前被销毁，这参数包含之前的状态。如果活动没被销毁之前，这参数是null。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list);
        initResources();

        // 用户首次使用时插入介绍信息
        setAppInfoFromRawRes();
    }

    /**
     * 处理从其他活动返回的结果。
     *
     * @param requestCode 启动其他活动时传入的请求代码。
     * @param resultCode  其他活动返回的结果代码。
     * @param data        其他活动返回的数据。
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 如果返回结果为OK且请求代码为打开节点或新建节点，则刷新列表
        if (resultCode == RESULT_OK
                && (requestCode == REQUEST_CODE_OPEN_NODE || requestCode == REQUEST_CODE_NEW_NODE)) {
            mNotesListAdapter.changeCursor(null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    /**
     * 从原始资源中设置应用信息。此方法会读取R.raw.introduction中的内容，
     * 并且只有当之前未添加介绍信息时，才将读取到的内容保存为一个工作笔记。
     */
    private void setAppInfoFromRawRes() {
        // 获取SharedPreferences实例
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // 检查是否已经添加了介绍信息
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {
            StringBuilder sb = new StringBuilder();
            InputStream in = null;
            try {
                // 从资源中打开introduction文件
                in = getResources().openRawResource(R.raw.introduction);
                if (in != null) {
                    // 读取文件内容到StringBuilder
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char[] buf = new char[1024];
                    int len = 0;
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);
                    }
                } else {
                    // 打印错误日志，如果无法打开文件
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                // 确保InputStream被关闭
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // 创建一个新的工作笔记并设置其内容
            WorkingNote note = WorkingNote.createEmptyNote(this, Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID, Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            note.setWorkingText(sb.toString());
            // 保存工作笔记并标记已添加介绍信息
            if (note.saveNote()) {
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();
            } else {
                // 打印错误日志，如果保存工作笔记失败
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    /**
     * Activity启动时调用，开始异步查询笔记列表。
     */
    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();
    }

    /**
     * 初始化资源，包括ListView、适配器和其他UI组件。
     */
    private void initResources() {
        // 获取ContentResolver实例
        mContentResolver = this.getContentResolver();
        // 创建后台查询处理器
        mBackgroundQueryHandler = new BackgroundQueryHandler(this.getContentResolver());
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        // 初始化ListView和相关监听器
        mNotesListView = (ListView) findViewById(R.id.notes_list);
        mNotesListView.addFooterView(LayoutInflater.from(this).inflate(R.layout.note_list_footer, null),
                null, false);
        mNotesListView.setOnItemClickListener(new OnListItemClickListener());
        mNotesListView.setOnItemLongClickListener(this);
        // 初始化并设置笔记列表适配器
        mNotesListAdapter = new NotesListAdapter(this);
        mNotesListView.setAdapter(mNotesListAdapter);
        // 初始化新建笔记按钮并设置点击监听器
        mAddNewNote = (Button) findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mAddNewNote.setOnTouchListener(new NewNoteOnTouchListener());
        // 初始化状态变量和触摸相关的变量
        mDispatch = false;
        mDispatchY = 0;
        mOriginY = 0;
        // 初始化标题栏和其他状态变量
        mTitleBar = (TextView) findViewById(R.id.tv_title_bar);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
    }


    /**
     * 用于处理列表的多选择模式和菜单点击事件的回调类。
     */
    private class ModeCallback implements ListView.MultiChoiceModeListener, OnMenuItemClickListener {
        private DropdownMenu mDropDownMenu; // 下拉菜单
        private ActionMode mActionMode; // 动作模式
        private MenuItem mMoveMenu; // 移动菜单项

        /**
         * 创建动作模式时的回调方法。
         *
         * @param mode 动作模式实例。
         * @param menu 菜单实例。
         * @return 如果成功创建动作模式，返回true；否则返回false。
         */
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // 加载菜单项
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            // 设置删除项的点击监听器
            menu.findItem(R.id.delete).setOnMenuItemClickListener(this);
            mMoveMenu = menu.findItem(R.id.move);
            // 根据条件决定是否显示移动菜单项
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || DataUtils.getUserFolderCount(mContentResolver) == 0) {
                mMoveMenu.setVisible(false);
            } else {
                mMoveMenu.setVisible(true);
                mMoveMenu.setOnMenuItemClickListener(this);
            }
            // 初始化动作模式和列表选择模式
            mActionMode = mode;
            mNotesListAdapter.setChoiceMode(true);
            mNotesListView.setLongClickable(false);
            mAddNewNote.setVisibility(View.GONE);

            // 设置自定义视图并初始化下拉菜单
            View customView = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.note_list_dropdown_menu, null);
            mode.setCustomView(customView);
            mDropDownMenu = new DropdownMenu(NotesListActivity.this,
                    (Button) customView.findViewById(R.id.selection_menu),
                    R.menu.note_list_dropdown);
            // 设置下拉菜单项点击监听器
            mDropDownMenu.setOnDropdownMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    updateMenu();
                    return true;
                }

            });
            return true;
        }

        /**
         * 更新动作模式下的菜单项。
         */
        private void updateMenu() {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            // 更新下拉菜单标题
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mDropDownMenu.setTitle(format);
            // 更新“选择全部”菜单项的状态
            MenuItem item = mDropDownMenu.findItem(R.id.action_select_all);
            if (item != null) {
                if (mNotesListAdapter.isAllSelected()) {
                    item.setChecked(true);
                    item.setTitle(R.string.menu_deselect_all);
                } else {
                    item.setChecked(false);
                    item.setTitle(R.string.menu_select_all);
                }
            }
        }

        /**
         * 准备动作模式时的回调方法。
         *
         * @param mode 动作模式实例。
         * @param menu 菜单实例。
         * @return 返回false，表示未进行任何操作。
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 点击动作模式中的菜单项时的回调方法。
         *
         * @param mode 动作模式实例。
         * @param item 被点击的菜单项。
         * @return 返回false，表示未进行任何操作。
         */
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 销毁动作模式时的回调方法。
         *
         * @param mode 动作模式实例。
         */
        public void onDestroyActionMode(ActionMode mode) {
            // 还原列表选择模式和设置
            mNotesListAdapter.setChoiceMode(false);
            mNotesListView.setLongClickable(true);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        public void finishActionMode() {
            mActionMode.finish();
        }

        /**
         * 处理列表项选择状态变化的回调方法。
         *
         * @param mode     动作模式实例。
         * @param position 列表中被改变选择状态的项的位置。
         * @param id       项的ID。
         * @param checked  项的新选择状态。
         */
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            // 更新列表项的选择状态并更新菜单
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        /**
         * 处理菜单项点击事件的回调方法。
         *
         * @param item 被点击的菜单项。
         * @return 如果已处理点击事件，返回true；否则返回false。
         */
        public boolean onMenuItemClick(MenuItem item) {
            // 若未选择任何项，则显示提示
            if (mNotesListAdapter.getSelectedCount() == 0) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            // 根据菜单项ID执行相应操作
            switch (item.getItemId()) {
                case R.id.delete:
                    // 显示删除确认对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(getString(R.string.alert_title_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                            mNotesListAdapter.getSelectedCount()));
                    builder.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    batchDelete();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                    break;
                case R.id.move:
                    // 启动查询目标文件夹的操作
                    startQueryDestinationFolders();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }


    /**
     * 为“新建笔记”按钮添加触摸监听器的内部类，实现点击和拖动事件的处理。
     */
    private class NewNoteOnTouchListener implements OnTouchListener {

        /**
         * 处理触摸事件。
         *
         * @param v     触摸的视图。
         * @param event 触摸事件。
         * @return 如果事件被处理则返回true，否则返回false。
         */
        public boolean onTouch(View v, MotionEvent event) {
            // 根据触摸事件的动作进行不同的处理
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    // 获取屏幕高度和“新建笔记”视图的高度
                    Display display = getWindowManager().getDefaultDisplay();
                    int screenHeight = display.getHeight();
                    int newNoteViewHeight = mAddNewNote.getHeight();
                    int start = screenHeight - newNoteViewHeight;
                    int eventY = start + (int) event.getY();
                    // 如果当前状态为子文件夹编辑状态，需减去标题栏的高度
                    if (mState == ListEditState.SUB_FOLDER) {
                        eventY -= mTitleBar.getHeight();
                        start -= mTitleBar.getHeight();
                    }
                    // 当点击到“新建笔记”按钮透明部分时，将事件分发给背后的列表视图
                    // 这里使用了一种硬编码的方式处理透明部分的点击，依赖于当前的背景公式
                    if (event.getY() < (event.getX() * (-0.12) + 94)) {
                        View view = mNotesListView.getChildAt(mNotesListView.getChildCount() - 1
                                - mNotesListView.getFooterViewsCount());
                        if (view != null && view.getBottom() > start
                                && (view.getTop() < (start + 94))) {
                            mOriginY = (int) event.getY();
                            mDispatchY = eventY;
                            event.setLocation(event.getX(), mDispatchY);
                            mDispatch = true;
                            return mNotesListView.dispatchTouchEvent(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // 如果正在分发触摸事件，则更新事件的位置并继续分发
                    if (mDispatch) {
                        mDispatchY += (int) event.getY() - mOriginY;
                        event.setLocation(event.getX(), mDispatchY);
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
                default: {
                    // 当触摸动作结束或取消时，停止分发事件
                    if (mDispatch) {
                        event.setLocation(event.getX(), mDispatchY);
                        mDispatch = false;
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
            }
            // 如果事件未被分发，则返回false
            return false;
        }

    }

    ;


    /**
     * 异步查询笔记列表。
     * 根据当前文件夹ID选择不同的查询条件，启动一个后台查询处理该查询。
     */
    private void startAsyncNotesListQuery() {
        // 根据当前文件夹ID选择查询条件
        String selection = (mCurrentFolderId == Notes.ID_ROOT_FOLDER) ? ROOT_FOLDER_SELECTION
                : NORMAL_SELECTION;
        // 启动查询，排序方式为类型降序，修改日期降序
        mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
                Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, new String[]{
                        String.valueOf(mCurrentFolderId)
                }, NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
    }

    /**
     * 处理后台查询的类。
     * 继承自AsyncQueryHandler，用于处理异步查询完成后的操作。
     */
    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        /**
         * 查询完成时的处理逻辑。
         * 根据查询标记的不同，执行不同的操作，如更新笔记列表或显示文件夹列表。
         *
         * @param token  查询标记，用于区分不同的查询。
         * @param cookie 查询时传入的附加对象。
         * @param cursor 查询结果的游标。
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case FOLDER_NOTE_LIST_QUERY_TOKEN:
                    // 更新笔记列表适配器的数据源
                    mNotesListAdapter.changeCursor(cursor);
                    break;
                case FOLDER_LIST_QUERY_TOKEN:
                    // 根据查询结果展示或记录错误
                    if (cursor != null && cursor.getCount() > 0) {
                        showFolderListMenu(cursor);
                    } else {
                        Log.e(TAG, "Query folder failed");
                    }
                    break;
                default:
                    // 对未知标记不做处理
                    return;
            }
        }
    }

    /**
     * 显示文件夹列表的菜单。
     * 使用查询结果构建一个对话框，让用户选择一个文件夹。
     *
     * @param cursor 查询结果的游标，包含文件夹信息。
     */
    private void showFolderListMenu(Cursor cursor) {
        // 构建文件夹列表选择的对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(R.string.menu_title_select_folder);
        final FoldersListAdapter adapter = new FoldersListAdapter(this, cursor);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            /**
             * 用户选择文件夹时的处理逻辑。
             * 将选中的笔记移动到用户选择的文件夹中，并给出反馈。
             *
             * @param dialog 对话框实例。
             * @param which  用户选择的项的索引。
             */
            public void onClick(DialogInterface dialog, int which) {
                // 批量移动选中的笔记到目标文件夹
                DataUtils.batchMoveToFolder(mContentResolver,
                        mNotesListAdapter.getSelectedItemIds(), adapter.getItemId(which));
                // 显示移动操作的反馈信息
                Toast.makeText(
                        NotesListActivity.this,
                        getString(R.string.format_move_notes_to_folder,
                                mNotesListAdapter.getSelectedCount(),
                                adapter.getFolderName(NotesListActivity.this, which)),
                        Toast.LENGTH_SHORT).show();
                // 结束当前的操作模式
                mModeCallBack.finishActionMode();
            }
        });
        builder.show();
    }

    /**
     * 创建新的笔记。
     * 启动一个活动用于编辑新笔记或编辑现有笔记。
     */
    private void createNewNote() {
        // 构建意图并指定动作为插入或编辑，以及初始文件夹ID
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
        // 启动该意图并期待返回结果
        this.startActivityForResult(intent, REQUEST_CODE_NEW_NODE);
    }


    /**
     * 批量删除笔记的函数。根据当前是否处于同步模式，采取不同的删除策略：如果不处于同步模式，则直接删除笔记；如果处于同步模式，则将笔记移动到回收站文件夹。
     * 执行删除操作后，会更新相应的widgets。
     */
    private void batchDelete() {
        new AsyncTask<Void, Void, HashSet<AppWidgetAttribute>>() {
            // 在后台执行任务，获取选中的widgets并执行删除操作
            protected HashSet<AppWidgetAttribute> doInBackground(Void... unused) {
                // 获取当前选中的widgets
                HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
                if (!isSyncMode()) {
                    // 如果当前不处于同步模式，直接删除笔记
                    if (DataUtils.batchDeleteNotes(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds())) {
                        // 删除成功无需额外操作
                    } else {
                        // 删除失败，记录错误
                        Log.e(TAG, "Delete notes error, should not happens");
                    }
                } else {
                    // 如果处于同步模式，将笔记移动到回收站文件夹
                    if (!DataUtils.batchMoveToFolder(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds(), Notes.ID_TRASH_FOLER)) {
                        // 移动失败，记录错误
                        Log.e(TAG, "Move notes to trash folder error, should not happens");
                    }
                }
                return widgets;
            }

            // 删除操作完成后，在UI线程执行后续操作
            @Override
            protected void onPostExecute(HashSet<AppWidgetAttribute> widgets) {
                // 遍历所有受影响的widgets，对有效的widgets进行更新
                if (widgets != null) {
                    for (AppWidgetAttribute widget : widgets) {
                        if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                            // 更新有效的widget
                            updateWidget(widget.widgetId, widget.widgetType);
                        }
                    }
                }
                // 结束动作模式
                mModeCallBack.finishActionMode();
            }
        }.execute();
    }


    /**
     * 删除指定的文件夹。
     * 如果是在同步模式下，文件夹会被移动到回收站，否则直接删除。
     * 同时，也会更新与该文件夹相关的所有小部件。
     *
     * @param folderId 要删除的文件夹ID。
     */
    private void deleteFolder(long folderId) {
        // 根据ID判断是否为根文件夹，根文件夹不能被删除
        if (folderId == Notes.ID_ROOT_FOLDER) {
            Log.e(TAG, "Wrong folder id, should not happen " + folderId);
            return;
        }

        HashSet<Long> ids = new HashSet<Long>();
        ids.add(folderId);

        // 获取与文件夹相关联的小部件信息
        HashSet<AppWidgetAttribute> widgets = DataUtils.getFolderNoteWidget(mContentResolver,
                folderId);
        if (!isSyncMode()) {
            // 非同步模式下直接删除文件夹
            DataUtils.batchDeleteNotes(mContentResolver, ids);
        } else {
            // 同步模式下将文件夹移动到回收站
            DataUtils.batchMoveToFolder(mContentResolver, ids, Notes.ID_TRASH_FOLER);
        }

        // 更新相关小部件
        if (widgets != null) {
            for (AppWidgetAttribute widget : widgets) {
                // 有效的小部件才进行更新
                if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                        && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                    updateWidget(widget.widgetId, widget.widgetType);
                }
            }
        }
    }

    /**
     * 打开指定的笔记节点进行编辑。
     *
     * @param data 包含要打开的笔记节点信息的对象。
     */
    private void openNode(NoteItemData data) {
        // 构造Intent并设置动作和额外数据，然后启动Activity
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, data.getId());
        this.startActivityForResult(intent, REQUEST_CODE_OPEN_NODE);
    }

    /**
     * 打开指定的文件夹，并加载其笔记列表。
     * 根据文件夹ID的不同，更新UI状态，包括标题和新增笔记按钮的可见性。
     *
     * @param data 包含要打开的文件夹信息的对象。
     */
    private void openFolder(NoteItemData data) {
        // 设置当前文件夹ID并启动异步查询
        mCurrentFolderId = data.getId();
        startAsyncNotesListQuery();

        // 根据文件夹ID更新UI状态
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mState = ListEditState.CALL_RECORD_FOLDER;
            mAddNewNote.setVisibility(View.GONE);
        } else {
            mState = ListEditState.SUB_FOLDER;
        }

        // 更新标题栏显示
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mTitleBar.setText(R.string.call_record_folder_name);
        } else {
            mTitleBar.setText(data.getSnippet());
        }
        mTitleBar.setVisibility(View.VISIBLE);
    }

    /**
     * 点击事件的处理方法。
     * 目前仅处理新建笔记按钮的点击事件。
     *
     * @param v 被点击的视图对象。
     */
    public void onClick(View v) {
        // 根据视图ID执行相应的操作
        switch (v.getId()) {
            case R.id.btn_new_note:
                createNewNote();
                break;
            default:
                break;
        }
    }


    /**
     * 显示软键盘。
     */
    private void showSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /**
     * 隐藏软键盘。
     *
     * @param view 触发隐藏软键盘的视图。
     */
    private void hideSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 显示创建或修改文件夹的对话框。
     *
     * @param create 如果为true，则为创建文件夹；如果为false，则为修改文件夹。
     */
    private void showCreateOrModifyFolderDialog(final boolean create) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        showSoftInput(); // 显示软键盘

        if (!create) {
            // 如果是修改文件夹
            if (mFocusNoteDataItem != null) {
                etName.setText(mFocusNoteDataItem.getSnippet()); // 设置当前文件夹名称
                builder.setTitle(getString(R.string.menu_folder_change_name)); // 设置对话框标题
            } else {
                Log.e(TAG, "The long click data item is null"); // 日志记录，长按的数据项为null
                return;
            }
        } else {
            // 如果是创建文件夹
            etName.setText(""); // 清空输入框内容
            builder.setTitle(this.getString(R.string.menu_create_folder)); // 设置对话框标题
        }

        // 设置对话框的确定和取消按钮
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                hideSoftInput(etName); // 点击取消时隐藏软键盘
            }
        });

        final Dialog dialog = builder.setView(view).show(); // 显示对话框
        final Button positive = (Button) dialog.findViewById(android.R.id.button1); // 获取确定按钮
        positive.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                hideSoftInput(etName); // 隐藏软键盘
                String name = etName.getText().toString(); // 获取输入的文件夹名称
                if (DataUtils.checkVisibleFolderName(mContentResolver, name)) { // 检查文件夹名称是否已存在
                    Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                            Toast.LENGTH_LONG).show(); // 显示文件夹已存在的提示
                    etName.setSelection(0, etName.length()); // 选中输入框中的所有文本
                    return;
                }
                if (!create) {
                    // 如果是修改文件夹
                    if (!TextUtils.isEmpty(name)) { // 验证输入的文件夹名称不为空
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SNIPPET, name); // 设置新的文件夹名称
                        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER); // 设置类型为文件夹
                        values.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记为已修改
                        mContentResolver.update(Notes.CONTENT_NOTE_URI, values, NoteColumns.ID
                                + "=?", new String[]{
                                String.valueOf(mFocusNoteDataItem.getId())
                        }); // 更新数据库中的文件夹信息
                    }
                } else if (!TextUtils.isEmpty(name)) { // 如果是创建文件夹
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.SNIPPET, name); // 设置文件夹名称
                    values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER); // 设置类型为文件夹
                    mContentResolver.insert(Notes.CONTENT_NOTE_URI, values); // 在数据库中插入新的文件夹信息
                }
                dialog.dismiss(); // 关闭对话框
            }
        });

        // 初始状态下，如果输入框为空，则禁用确定按钮
        if (TextUtils.isEmpty(etName.getText())) {
            positive.setEnabled(false);
        }

        // 监听输入框文本变化，以动态启用或禁用确定按钮
        etName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 空实现
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(etName.getText())) { // 当输入框为空时，禁用确定按钮
                    positive.setEnabled(false);
                } else { // 当输入框不为空时，启用确定按钮
                    positive.setEnabled(true);
                }
            }

            public void afterTextChanged(Editable s) {
                // 空实现
            }
        });
    }


    /**
     * 当用户按下返回键时调用的方法，根据当前状态执行不同的操作。
     * 在子文件夹状态下，返回根文件夹并显示笔记列表；
     * 在通话记录文件夹状态下，也返回根文件夹但显示添加新笔记按钮；
     * 在笔记列表状态下，执行父类的onBackPressed方法，通常是退出或返回上一级。
     */
    @Override
    public void onBackPressed() {
        switch (mState) {
            case SUB_FOLDER:
                // 从子文件夹状态返回到根文件夹的笔记列表状态
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                startAsyncNotesListQuery();
                mTitleBar.setVisibility(View.GONE);
                break;
            case CALL_RECORD_FOLDER:
                // 从通话记录文件夹状态返回到根文件夹的笔记列表状态，并显示添加新笔记按钮
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                mAddNewNote.setVisibility(View.VISIBLE);
                mTitleBar.setVisibility(View.GONE);
                startAsyncNotesListQuery();
                break;
            case NOTE_LIST:
                // 在笔记列表状态下，执行父类的返回操作
                super.onBackPressed();
                break;
            default:
                // 对于其他状态，不执行任何操作
                break;
        }
    }

    /**
     * 更新小部件显示。
     * 根据传入的小部件类型，设置对应的Provider并发送更新广播。
     *
     * @param appWidgetId   小部件ID
     * @param appWidgetType 小部件类型
     */
    private void updateWidget(int appWidgetId, int appWidgetType) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // 根据小部件类型设置Provider
        if (appWidgetType == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (appWidgetType == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unspported widget type");
            return;
        }

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{
                appWidgetId
        });

        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }

    /**
     * 文件夹列表的上下文菜单创建监听器。
     * 在焦点笔记项不为空时，添加查看、删除和重命名菜单项。
     */
    private final OnCreateContextMenuListener mFolderOnCreateContextMenuListener = new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (mFocusNoteDataItem != null) {
                menu.setHeaderTitle(mFocusNoteDataItem.getSnippet());
                menu.add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
                menu.add(0, MENU_FOLDER_DELETE, 0, R.string.menu_folder_delete);
                menu.add(0, MENU_FOLDER_CHANGE_NAME, 0, R.string.menu_folder_change_name);
            }
        }
    };

    /**
     * 上下文菜单关闭时的回调方法。
     * 在列表视图中取消上下文菜单的监听器。
     *
     * @param menu 被关闭的菜单对象
     */
    @Override
    public void onContextMenuClosed(Menu menu) {
        if (mNotesListView != null) {
            mNotesListView.setOnCreateContextMenuListener(null);
        }
        super.onContextMenuClosed(menu);
    }


    /**
     * 当上下文菜单中的项目被选择时调用。
     *
     * @param item 被选择的菜单项。
     * @return 如果事件已成功处理，则返回true；否则如果事件未处理，则返回false。
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mFocusNoteDataItem == null) {
            Log.e(TAG, "The long click data item is null");
            return false;
        }
        switch (item.getItemId()) {
            case MENU_FOLDER_VIEW:
                openFolder(mFocusNoteDataItem); // 打开指定的文件夹
                break;
            case MENU_FOLDER_DELETE:
                // 显示删除文件夹的确认对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_folder));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFolder(mFocusNoteDataItem.getId()); // 确认后删除文件夹
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case MENU_FOLDER_CHANGE_NAME:
                showCreateOrModifyFolderDialog(false); // 显示修改文件夹名称的对话框
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * 准备选项菜单。
     *
     * @param menu 菜单对象。
     * @return 总是返回true。
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear(); // 清除之前的菜单项
        // 根据当前状态加载不同的菜单布局
        if (mState == ListEditState.NOTE_LIST) {
            getMenuInflater().inflate(R.menu.note_list, menu);
            // 设置同步或取消同步菜单项的标题
            menu.findItem(R.id.menu_sync).setTitle(
                    GTaskSyncService.isSyncing() ? R.string.menu_sync_cancel : R.string.menu_sync);
        } else if (mState == ListEditState.SUB_FOLDER) {
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            Log.e(TAG, "Wrong state:" + mState);
        }
        return true;
    }

    /**
     * 处理选项菜单项的选择。
     *
     * @param item 被选择的菜单项。
     * @return 如果事件已成功处理，则返回true；否则如果事件未处理，则返回false。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                showCreateOrModifyFolderDialog(true); // 显示创建新文件夹的对话框
                break;
            }
            case R.id.menu_export_text: {
                exportNoteToText(); // 导出笔记为文本
                break;
            }
            case R.id.menu_sync: {
                // 处理同步菜单项的点击事件
                if (isSyncMode()) {
                    if (TextUtils.equals(item.getTitle(), getString(R.string.menu_sync))) {
                        GTaskSyncService.startSync(this);
                    } else {
                        GTaskSyncService.cancelSync(this);
                    }
                } else {
                    startPreferenceActivity();
                }
                break;
            }
            case R.id.menu_setting: {
                startPreferenceActivity(); // 打开设置界面
                break;
            }
            case R.id.menu_new_note: {
                createNewNote(); // 创建新笔记
                break;
            }
            case R.id.menu_search:
                onSearchRequested(); // 触发搜索请求
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 处理搜索请求。
     *
     * @return 总是返回true。
     */
    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /* appData */, false);
        return true;
    }


    /**
     * 将笔记导出为文本文件。
     * 在后台任务中执行导出操作，并根据操作结果展示不同的对话框。
     */
    private void exportNoteToText() {
        final BackupUtils backup = BackupUtils.getInstance(NotesListActivity.this);
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... unused) {
                // 执行导出操作
                return backup.exportToText();
            }

            @Override
            protected void onPostExecute(Integer result) {
                // 根据导出结果展示不同的对话框
                if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
                    showExportFailedDialog(NotesListActivity.this.getString(R.string.failed_sdcard_export),
                            NotesListActivity.this.getString(R.string.error_sdcard_unmounted));
                } else if (result == BackupUtils.STATE_SUCCESS) {
                    showExportSuccessDialog(NotesListActivity.this.getString(R.string.success_sdcard_export),
                            backup.getExportedTextFileName(), backup.getExportedTextFileDir());
                } else if (result == BackupUtils.STATE_SYSTEM_ERROR) {
                    showExportFailedDialog(NotesListActivity.this.getString(R.string.failed_sdcard_export),
                            NotesListActivity.this.getString(R.string.error_sdcard_export));
                }
            }

        }.execute();
    }

    /**
     * 检查当前是否为同步模式。
     *
     * @return 如果已配置同步账户名则返回true，否则返回false。
     */
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    /**
     * 启动设置活动。
     * 用于打开设置界面。
     */
    private void startPreferenceActivity() {
        Activity from = getParent() != null ? getParent() : this;
        Intent intent = new Intent(from, NotesPreferenceActivity.class);
        from.startActivityIfNeeded(intent, -1);
    }

    /**
     * 列表项点击监听器。
     * 处理列表项的点击事件，根据不同的状态和项类型执行相应的操作。
     */
    private class OnListItemClickListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (view instanceof NotesListItem) {
                NoteItemData item = ((NotesListItem) view).getItemData();
                if (mNotesListAdapter.isInChoiceMode()) {
                    // 在选择模式下处理项的点击事件
                    if (item.getType() == Notes.TYPE_NOTE) {
                        position = position - mNotesListView.getHeaderViewsCount();
                        mModeCallBack.onItemCheckedStateChanged(null, position, id,
                                !mNotesListAdapter.isSelectedItem(position));
                    }
                    return;
                }

                // 根据当前状态处理项的点击事件
                switch (mState) {
                    case NOTE_LIST:
                        if (item.getType() == Notes.TYPE_FOLDER
                                || item.getType() == Notes.TYPE_SYSTEM) {
                            openFolder(item);
                        } else if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in NOTE_LIST");
                        }
                        break;
                    case SUB_FOLDER:
                    case CALL_RECORD_FOLDER:
                        if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in SUB_FOLDER");
                        }
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /**
     * 启动查询目标文件夹。
     * 根据当前状态查询并显示文件夹列表。
     */
    private void startQueryDestinationFolders() {
        String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND " + NoteColumns.ID + "<>?";
        selection = (mState == ListEditState.NOTE_LIST) ? selection :
                "(" + selection + ") OR (" + NoteColumns.ID + "=" + Notes.ID_ROOT_FOLDER + ")";

        mBackgroundQueryHandler.startQuery(FOLDER_LIST_QUERY_TOKEN,
                null,
                Notes.CONTENT_NOTE_URI,
                FoldersListAdapter.PROJECTION,
                selection,
                new String[]{
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(mCurrentFolderId)
                },
                NoteColumns.MODIFIED_DATE + " DESC");
    }

    /**
     * 长按列表项时的处理。
     * 根据不同的项类型启动选择模式或显示上下文菜单。
     *
     * @return 总是返回false。
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view instanceof NotesListItem) {
            mFocusNoteDataItem = ((NotesListItem) view).getItemData();
            if (mFocusNoteDataItem.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
                // 长按笔记项时启动选择模式
                if (mNotesListView.startActionMode(mModeCallBack) != null) {
                    mModeCallBack.onItemCheckedStateChanged(null, position, id, true);
                    mNotesListView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } else {
                    Log.e(TAG, "startActionMode fails");
                }
            } else if (mFocusNoteDataItem.getType() == Notes.TYPE_FOLDER) {
                // 长按文件夹项时设置上下文菜单监听器
                mNotesListView.setOnCreateContextMenuListener(mFolderOnCreateContextMenuListener);
            }
        }
        return false;
    }

    /**
     * 显示导出失败的对话框。
     *
     * @param title   对话框标题
     * @param message 对话框消息内容
     */
    private void showExportFailedDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    /**
     * 显示导出成功的对话框。
     *
     * @param title    对话框标题
     * @param fileName 导出文件的名称
     * @param fileDir  导出文件的目录
     */
    private void showExportSuccessDialog(String title, String fileName, String fileDir) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(title);
        builder.setMessage(NotesListActivity.this.getString(R.string.format_exported_file_location, fileName, fileDir));
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

}
