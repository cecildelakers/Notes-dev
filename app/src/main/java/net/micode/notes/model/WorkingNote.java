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

package net.micode.notes.model;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.tool.ResourceParser.NoteBgResources;

// WorkingNote类用于管理笔记的相关信息
public class WorkingNote {
    // 笔记对象，包含笔记的详细信息
    private Note mNote;
    // 笔记的唯一标识符
    private long mNoteId;
    // 笔记的内容
    private String mContent;
    // 笔记的模式，例如普通、草稿等
    private int mMode;

    // 设置提醒日期的时间戳
    private long mAlertDate;
    // 笔记最后修改日期的时间戳
    private long mModifiedDate;
    // 笔记背景颜色的资源ID
    private int mBgColorId;
    // 小部件的ID
    private int mWidgetId;
    // 小部件的类型
    private int mWidgetType;
    // 笔记所属文件夹的ID
    private long mFolderId;
    // 上下文对象，用于访问应用的环境信息
    private Context mContext;

    // 日志标签，用于Log输出
    private static final String TAG = "WorkingNote";

    // 标记笔记是否被删除
    private boolean mIsDeleted;

    // 笔记设置变化监听器
    private NoteSettingChangedListener mNoteSettingStatusListener;

    // 定义一个静态数组，用于在查询时投影数据列
    public static final String[] DATA_PROJECTION = new String[]{
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };


    // 定义查询Note表时需要投影的列
    public static final String[] NOTE_PROJECTION = new String[]{
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE
    };

    // 数据ID列的索引
    private static final int DATA_ID_COLUMN = 0;

    // 数据内容列的索引
    private static final int DATA_CONTENT_COLUMN = 1;

    // 数据MIME类型列的索引
    private static final int DATA_MIME_TYPE_COLUMN = 2;

    // 数据模式列的索引
    private static final int DATA_MODE_COLUMN = 3;

    // Note表中父ID列的索引
    private static final int NOTE_PARENT_ID_COLUMN = 0;

    // Note表中提醒日期列的索引
    private static final int NOTE_ALERTED_DATE_COLUMN = 1;

    // Note表中背景颜色ID列的索引
    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;

    // Note表中Widget ID列的索引
    private static final int NOTE_WIDGET_ID_COLUMN = 3;

    // Note表中Widget类型列的索引
    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;

    // Note表中修改日期列的索引
    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    /**
     * 新建笔记的构造函数
     *
     * @param context  上下文对象，用于访问应用全局功能
     * @param folderId 文件夹ID，表示该笔记所属的文件夹
     */
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        mAlertDate = 0;
        mModifiedDate = System.currentTimeMillis();
        mFolderId = folderId;
        mNote = new Note();
        mNoteId = 0;
        mIsDeleted = false;
        mMode = 0;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
    }

    /**
     * 已存在笔记的构造函数
     *
     * @param context  上下文对象，用于访问应用全局功能
     * @param noteId   笔记ID，表示该笔记的唯一标识
     * @param folderId 文件夹ID，表示该笔记所属的文件夹
     */
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        mIsDeleted = false;
        mNote = new Note();
        loadNote();
    }


    /**
     * 加载指定笔记的信息。
     * 从数据库中查询指定ID的笔记的详细信息，并更新当前实例的状态。
     * 注意：此方法不处理查询失败或笔记不存在的情况。
     */
    private void loadNote() {
        // 查询指定ID的笔记信息
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                null, null);

        if (cursor != null) {
            // 如果查询结果不为空，尝试读取数据
            if (cursor.moveToFirst()) {
                // 从查询结果中获取笔记的各个属性
                mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
            }
            // 关闭查询结果集
            cursor.close();
        } else {
            // 如果查询结果为空，记录错误并抛出异常
            Log.e(TAG, "No note with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
        }
        // 加载笔记的附加数据，如内容、设置等
        loadNoteData();
    }

    /**
     * 加载笔记的附加数据。
     * 从数据库中查询指定ID笔记的附加信息（例如内容、设置等），并更新当前实例的状态。
     * 注意：此方法不处理查询失败或笔记数据不存在的情况。
     */
    private void loadNoteData() {
        // 查询指定笔记ID的附加数据
        Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[]{
                        String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            // 如果查询结果不为空，尝试读取数据
            if (cursor.moveToFirst()) {
                do {
                    // 根据数据类型处理不同的笔记内容
                    String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                    if (DataConstants.NOTE.equals(type)) {
                        // 处理普通笔记内容
                        mContent = cursor.getString(DATA_CONTENT_COLUMN);
                        mMode = cursor.getInt(DATA_MODE_COLUMN);
                        mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else if (DataConstants.CALL_NOTE.equals(type)) {
                        // 处理通话笔记内容
                        mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else {
                        // 记录错误的笔记类型
                        Log.d(TAG, "Wrong note type with type:" + type);
                    }
                } while (cursor.moveToNext());
            }
            // 关闭查询结果集
            cursor.close();
        } else {
            // 如果查询结果为空，记录错误并抛出异常
            Log.e(TAG, "No data with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note's data with id " + mNoteId);
        }
    }

    /**
     * 创建一个新的空笔记。
     *
     * @param context          上下文对象，用于访问应用资源和内容提供者。
     * @param folderId         笔记所属文件夹的ID。
     * @param widgetId         与笔记关联的小部件ID。
     * @param widgetType       与笔记关联的小部件类型。
     * @param defaultBgColorId 笔记的默认背景颜色ID。
     * @return 返回一个初始化好的空笔记对象。
     */
    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
                                              int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        note.setBgColorId(defaultBgColorId);
        note.setWidgetId(widgetId);
        note.setWidgetType(widgetType);
        return note;
    }

    /**
     * 根据笔记ID加载笔记。
     *
     * @param context 上下文对象，用于访问应用资源和内容提供者。
     * @param id      要加载的笔记的ID。
     * @return 返回一个根据指定ID加载的笔记对象。
     */
    public static WorkingNote load(Context context, long id) {
        return new WorkingNote(context, id, 0);
    }


    /**
     * 保存笔记到数据库。
     * 如果笔记值得保存（即内容非空且未被标记为删除），且笔记不存在于数据库中或已存在于数据库但本地有修改，则进行保存操作。
     * 如果笔记存在对应的小部件，会更新小部件内容。
     *
     * @return 如果保存成功返回true，否则返回false。
     */
    public synchronized boolean saveNote() {
        // 判断是否值得保存该笔记
        if (isWorthSaving()) {
            // 检查数据库中是否已存在该笔记
            if (!existInDatabase()) {
                // 为笔记生成新的ID
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    Log.e(TAG, "Create new note fail with id:" + mNoteId);
                    return false;
                }
            }

            mNote.syncNote(mContext, mNoteId);  // 同步笔记到数据库

            // 如果存在对应的小部件，更新小部件内容
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查笔记是否已存在于数据库中。
     *
     * @return 如果笔记ID大于0，表示已存在于数据库，返回true；否则返回false。
     */
    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    /**
     * 判断笔记是否值得被保存。
     * 笔记不值得保存的情况包括：已被标记为删除、不存在于数据库中且内容为空、存在于数据库但未本地修改。
     *
     * @return 如果笔记值得保存返回true，否则返回false。
     */
    private boolean isWorthSaving() {
        // 判断笔记是否值得保存
        if (mIsDeleted || (!existInDatabase() && TextUtils.isEmpty(mContent))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 设置笔记设置状态监听器。
     *
     * @param l 笔记设置状态监听器对象。
     */
    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }

    /**
     * 设置提醒日期，并根据需要触发状态监听器。
     *
     * @param date 设置的提醒日期。
     * @param set  是否设置提醒。
     */
    public void setAlertDate(long date, boolean set) {
        // 更新提醒日期并触发监听器
        if (date != mAlertDate) {
            mAlertDate = date;
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
    }

    /**
     * 标记笔记为已删除，并根据需要触发小部件变更监听器。
     *
     * @param mark 是否标记为已删除。
     */
    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        // 如果存在对应的小部件，触发小部件变更监听器
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onWidgetChanged();
        }
    }

    /**
     * 设置笔记背景颜色ID，并根据需要触发监听器。
     *
     * @param id 背景颜色的资源ID。
     */
    public void setBgColorId(int id) {
        // 更新背景颜色ID并触发监听器
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }


    /**
     * 设置勾选列表模式
     *
     * @param mode 模式值
     */
    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            // 当前模式与新模式不同时，通知监听器模式发生变化
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            // 更新笔记中的模式值
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }

    /**
     * 设置小部件类型
     *
     * @param type 小部件类型值
     */
    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            // 更新笔记中小部件类型的值
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }

    /**
     * 设置小部件ID
     *
     * @param id 小部件ID
     */
    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            // 更新笔记中小部件ID的值
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }

    /**
     * 设置工作文本
     *
     * @param text 工作文本内容
     */
    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            // 更新笔记中的文本内容
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    /**
     * 转换为通话笔记
     *
     * @param phoneNumber 电话号码
     * @param callDate    通话日期
     */
    public void convertToCallNote(String phoneNumber, long callDate) {
        // 设置通话日期和电话号码，并关联至通话记录文件夹
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
    }

    /**
     * 检查是否有定时提醒
     *
     * @return true表示设置了定时提醒，false表示未设置
     */
    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    /**
     * 获取内容文本
     *
     * @return 笔记内容
     */
    public String getContent() {
        return mContent;
    }

    /**
     * 获取提醒日期
     *
     * @return 提醒日期时间戳
     */
    public long getAlertDate() {
        return mAlertDate;
    }

    /**
     * 获取最后修改日期
     *
     * @return 最后修改日期时间戳
     */
    public long getModifiedDate() {
        return mModifiedDate;
    }

    /**
     * 获取背景颜色资源ID
     *
     * @return 背景颜色资源ID
     */
    public int getBgColorResId() {
        return NoteBgResources.getNoteBgResource(mBgColorId);
    }

    /**
     * 获取背景颜色ID
     *
     * @return 背景颜色ID
     */
    public int getBgColorId() {
        return mBgColorId;
    }

    /**
     * 获取标题背景资源ID
     *
     * @return 标题背景资源ID
     */
    public int getTitleBgResId() {
        return NoteBgResources.getNoteTitleBgResource(mBgColorId);
    }

    /**
     * 获取当前勾选列表模式
     *
     * @return 勾选列表模式值
     */
    public int getCheckListMode() {
        return mMode;
    }

    /**
     * 获取笔记ID
     *
     * @return 笔记ID
     */
    public long getNoteId() {
        return mNoteId;
    }

    /**
     * 获取文件夹ID
     *
     * @return 文件夹ID
     */
    public long getFolderId() {
        return mFolderId;
    }

    /**
     * 获取小部件ID
     *
     * @return 小部件ID
     */
    public int getWidgetId() {
        return mWidgetId;
    }

    /**
     * 获取小部件类型
     *
     * @return 小部件类型
     */
    public int getWidgetType() {
        return mWidgetType;
    }

    /**
     * 笔记设置变化监听器接口
     */
    public interface NoteSettingChangedListener {
        /**
         * 当前笔记的背景颜色发生变化时调用
         */
        void onBackgroundColorChanged();

        /**
         * 用户设置定时提醒时调用
         *
         * @param date 提醒日期
         * @param set  是否设置提醒
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * 用户从小部件创建笔记时调用
         */
        void onWidgetChanged();

        /**
         * 切换勾选列表模式和普通模式时调用
         *
         * @param oldMode 切换前的模式
         * @param newMode 切换后的模式
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }

}
