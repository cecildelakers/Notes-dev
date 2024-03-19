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

package net.micode.notes.gtask.data;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.tool.ResourceParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * SqlNote 类用于管理和操作数据库中的笔记数据。
 * 它提供了创建、加载和更新笔记内容的接口。
 */
public class SqlNote {
    // 日志标签
    private static final String TAG = SqlNote.class.getSimpleName();

    // 无效的ID值
    private static final int INVALID_ID = -99999;

    // 查询笔记时要选择的列
    public static final String[] PROJECTION_NOTE = new String[]{
            NoteColumns.ID, NoteColumns.ALERTED_DATE, NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE, NoteColumns.HAS_ATTACHMENT, NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT, NoteColumns.PARENT_ID, NoteColumns.SNIPPET, NoteColumns.TYPE,
            NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE, NoteColumns.SYNC_ID,
            NoteColumns.LOCAL_MODIFIED, NoteColumns.ORIGIN_PARENT_ID, NoteColumns.GTASK_ID,
            NoteColumns.VERSION
    };

    // 各查询列的索引
    public static final int ID_COLUMN = 0;
    public static final int ALERTED_DATE_COLUMN = 1;
    public static final int BG_COLOR_ID_COLUMN = 2;
    public static final int CREATED_DATE_COLUMN = 3;
    public static final int HAS_ATTACHMENT_COLUMN = 4;
    public static final int MODIFIED_DATE_COLUMN = 5;
    public static final int NOTES_COUNT_COLUMN = 6;
    public static final int PARENT_ID_COLUMN = 7;
    public static final int SNIPPET_COLUMN = 8;
    public static final int TYPE_COLUMN = 9;
    public static final int WIDGET_ID_COLUMN = 10;
    public static final int WIDGET_TYPE_COLUMN = 11;
    public static final int SYNC_ID_COLUMN = 12;
    public static final int LOCAL_MODIFIED_COLUMN = 13;
    public static final int ORIGIN_PARENT_ID_COLUMN = 14;
    public static final int GTASK_ID_COLUMN = 15;
    public static final int VERSION_COLUMN = 16;

    // 上下文和内容解析器，用于访问数据库
    private Context mContext;
    private ContentResolver mContentResolver;

    // 标记是否创建新笔记
    private boolean mIsCreate;

    // 笔记的各种属性
    private long mId;
    private long mAlertDate;
    private int mBgColorId;
    private long mCreatedDate;
    private int mHasAttachment;
    private long mModifiedDate;
    private long mParentId;
    private String mSnippet;
    private int mType;
    private int mWidgetId;
    private int mWidgetType;
    private long mOriginParent;
    private long mVersion;

    // 用于存储两次更新之间差异的数据值
    private ContentValues mDiffNoteValues;

    // 存储与笔记相关数据的列表
    private ArrayList<SqlData> mDataList;

    /**
     * 构造函数，初始化一个新的SqlNote实例。
     *
     * @param context 上下文，通常是指Activity或Application对象。
     */
    public SqlNote(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        // 初始化笔记属性为默认值
        mId = INVALID_ID;
        mAlertDate = 0;
        mBgColorId = ResourceParser.getDefaultBgId(context);
        mCreatedDate = System.currentTimeMillis();
        mHasAttachment = 0;
        mModifiedDate = System.currentTimeMillis();
        mParentId = 0;
        mSnippet = "";
        mType = Notes.TYPE_NOTE;
        mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
        mOriginParent = 0;
        mVersion = 0;
        mDiffNoteValues = new ContentValues();
        mDataList = new ArrayList<SqlData>();
    }

    /**
     * 构造函数，从数据库中加载指定ID的笔记。
     *
     * @param context 上下文，通常是指Activity或Application对象。
     * @param c       数据库查询结果的Cursor对象。
     */
    public SqlNote(Context context, Cursor c) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(c);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }

    /**
     * 构造函数，从数据库中加载指定ID的笔记。
     *
     * @param context 上下文，通常是指Activity或Application对象。
     * @param id      要加载的笔记的ID。
     */
    public SqlNote(Context context, long id) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(id);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }

    // 从数据库中加载笔记数据
    private void loadFromCursor(long id) {
        Cursor c = null;
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, PROJECTION_NOTE, "(_id=?)",
                    new String[]{
                            String.valueOf(id)
                    }, null);
            if (c != null) {
                if (c.moveToNext()) {
                    loadFromCursor(c);
                } else {
                    Log.w(TAG, "loadFromCursor: cursor = null");
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    // 从Cursor中加载笔记数据到实例属性
    private void loadFromCursor(Cursor c) {
        mId = c.getLong(ID_COLUMN);
        mAlertDate = c.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = c.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = c.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = c.getInt(HAS_ATTACHMENT_COLUMN);
        mModifiedDate = c.getLong(MODIFIED_DATE_COLUMN);
        mParentId = c.getLong(PARENT_ID_COLUMN);
        mSnippet = c.getString(SNIPPET_COLUMN);
        mType = c.getInt(TYPE_COLUMN);
        mWidgetId = c.getInt(WIDGET_ID_COLUMN);
        mWidgetType = c.getInt(WIDGET_TYPE_COLUMN);
        mVersion = c.getLong(VERSION_COLUMN);
    }

    /**
     * 加载数据内容。
     * 从数据库中查询特定note_id的数据，并将其加载到mDataList中。
     */
    private void loadDataContent() {
        Cursor c = null;
        mDataList.clear();
        try {
            // 查询指定note_id的数据
            c = mContentResolver.query(Notes.CONTENT_DATA_URI, SqlData.PROJECTION_DATA,
                    "(note_id=?)", new String[]{
                            String.valueOf(mId)
                    }, null);
            if (c != null) {
                // 如果查询结果为空，打印警告信息并返回
                if (c.getCount() == 0) {
                    Log.w(TAG, "it seems that the note has not data");
                    return;
                }
                // 遍历查询结果，并加载到mDataList中
                while (c.moveToNext()) {
                    SqlData data = new SqlData(mContext, c);
                    mDataList.add(data);
                }
            } else {
                // 如果查询结果为null，打印警告信息
                Log.w(TAG, "loadDataContent: cursor = null");
            }
        } finally {
            // 释放资源
            if (c != null)
                c.close();
        }
    }

    /**
     * 设置内容。
     * 根据传入的JSONObject，更新或创建笔记的相关内容。
     *
     * @param js 包含笔记信息的JSONObject。
     * @return 成功返回true，失败返回false。
     */
    public boolean setContent(JSONObject js) {
        try {
            // 从js中获取note信息
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            // 系统笔记不可修改
            if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                Log.w(TAG, "cannot set system folder");
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                // 文件夹类型笔记，仅更新snippet和类型
                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_NOTE) {
                // 笔记类型，更新或设置多种信息
                JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                long id = note.has(NoteColumns.ID) ? note.getLong(NoteColumns.ID) : INVALID_ID;
                if (mIsCreate || mId != id) {
                    mDiffNoteValues.put(NoteColumns.ID, id);
                }
                mId = id;

                // 更新或设置提醒日期、背景色id、创建日期、附件标志、修改日期、父id、snippet、类型、小部件id和类型等信息
                // 该部分通过条件判断，确定是否需要更新数据库字段

                // 处理数据项数组，每个数据项会被更新或创建
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    SqlData sqlData = null;
                    if (data.has(DataColumns.ID)) {
                        long dataId = data.getLong(DataColumns.ID);
                        for (SqlData temp : mDataList) {
                            if (dataId == temp.getId()) {
                                sqlData = temp;
                            }
                        }
                    }

                    if (sqlData == null) {
                        sqlData = new SqlData(mContext);
                        mDataList.add(sqlData);
                    }

                    sqlData.setContent(data);
                }
            }
        } catch (JSONException e) {
            // 处理JSON解析异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取内容。
     * 将当前笔记的内容转换为JSONObject格式。
     *
     * @return 笔记内容的JSONObject，如果无法转换成功则返回null。
     */
    public JSONObject getContent() {
        try {
            JSONObject js = new JSONObject();

            if (mIsCreate) {
                // 如果笔记尚未在数据库中创建，返回null
                Log.e(TAG, "it seems that we haven't created this in database yet");
                return null;
            }

            JSONObject note = new JSONObject();
            // 根据笔记类型，填充不同的信息到note JSONObject中
            // 该部分通过条件判断，根据mType选择需要填充的信息

            // 将note和data信息添加到js中
            js.put(GTaskStringUtils.META_HEAD_NOTE, note);
            // 处理数据项数组，将其添加到js中

            return js;
        } catch (JSONException e) {
            // 处理JSON构建异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置父id。
     *
     * @param id 父笔记的id。
     */
    public void setParentId(long id) {
        mParentId = id;
        mDiffNoteValues.put(NoteColumns.PARENT_ID, id);
    }

    /**
     * 设置Gtask id。
     *
     * @param gid Gtask的id。
     */
    public void setGtaskId(String gid) {
        mDiffNoteValues.put(NoteColumns.GTASK_ID, gid);
    }

    /**
     * 设置同步id。
     *
     * @param syncId 同步的id。
     */
    public void setSyncId(long syncId) {
        mDiffNoteValues.put(NoteColumns.SYNC_ID, syncId);
    }

    /**
     * 重置本地修改标志。
     */
    public void resetLocalModified() {
        mDiffNoteValues.put(NoteColumns.LOCAL_MODIFIED, 0);
    }

    /**
     * 获取笔记id。
     *
     * @return 笔记的id。
     */
    public long getId() {
        return mId;
    }

    /**
     * 获取父id。
     *
     * @return 父笔记的id。
     */
    public long getParentId() {
        return mParentId;
    }

    /**
     * 获取snippet。
     *
     * @return 笔记的snippet。
     */
    public String getSnippet() {
        return mSnippet;
    }

    /**
     * 判断是否为笔记类型。
     *
     * @return 是笔记类型返回true，否则返回false。
     */
    public boolean isNoteType() {
        return mType == Notes.TYPE_NOTE;
    }

    /**
     * 提交对笔记的更改或创建新的笔记。
     *
     * @param validateVersion 是否验证版本号。如果为 true，则在更新笔记时会检查版本号以避免并发更新的问题。
     *                        如果为 false，则不进行版本号检查。
     *                        这个参数主要用于处理客户端在同步过程中可能同时更新同一笔记的情况。
     */
    public void commit(boolean validateVersion) {
        if (mIsCreate) { // 处理创建新笔记的逻辑
            // 在创建新笔记时，如果ID是无效的（即未指定），且包含了ID字段，则移除该字段
            if (mId == INVALID_ID && mDiffNoteValues.containsKey(NoteColumns.ID)) {
                mDiffNoteValues.remove(NoteColumns.ID);
            }

            // 使用ContentResolver插入新的笔记数据
            Uri uri = mContentResolver.insert(Notes.CONTENT_NOTE_URI, mDiffNoteValues);
            try {
                // 从插入返回的URI中解析出新笔记的ID
                mId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Get note id error :" + e.toString());
                // 如果无法解析出ID，抛出异常
                throw new ActionFailureException("create note failed");
            }
            // 检查解析出的ID是否有效
            if (mId == 0) {
                throw new IllegalStateException("Create thread id failed");
            }

            // 如果是创建笔记类型，提交关联数据
            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, false, -1);
                }
            }
        } else { // 处理更新现有笔记的逻辑
            // 如果指定的笔记ID无效或不存在，抛出异常
            if (mId <= 0 && mId != Notes.ID_ROOT_FOLDER && mId != Notes.ID_CALL_RECORD_FOLDER) {
                Log.e(TAG, "No such note");
                throw new IllegalStateException("Try to update note with invalid id");
            }
            // 如果有差异的数据需要更新，则进行更新
            if (mDiffNoteValues.size() > 0) {
                mVersion++; // 更新版本号
                int result = 0;
                // 根据是否验证版本号，执行不同的更新逻辑
                if (!validateVersion) {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?)", new String[]{
                            String.valueOf(mId)
                    });
                } else {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                                    + NoteColumns.ID + "=?) AND (" + NoteColumns.VERSION + "<=?)",
                            new String[]{
                                    String.valueOf(mId), String.valueOf(mVersion)
                            });
                }
                // 如果更新结果为0，说明没有进行任何更新，可能是由于同步时用户同时更新了笔记
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }

            // 如果是笔记类型，提交关联数据
            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, validateVersion, mVersion);
                }
            }
        }

        // 刷新本地信息，加载最新数据
        loadFromCursor(mId);
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();

        // 清空差异数据，重置创建状态
        mDiffNoteValues.clear();
        mIsCreate = false;
    }
}
