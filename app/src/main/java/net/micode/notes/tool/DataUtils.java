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

package net.micode.notes.tool;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.ArrayList;
import java.util.HashSet;


public class DataUtils {
    public static final String TAG = "DataUtils";

    /**
     * 批量删除笔记
     *
     * @param resolver 内容解析器
     * @param ids      要删除的笔记ID集合
     * @return 如果删除成功或集合为空或为null，则返回true，否则返回false
     */
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }

        // 构建删除操作的列表
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            if (id == Notes.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root");
                continue;
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            operationList.add(builder.build());
        }
        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            // 检查删除结果
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 将笔记移动到指定文件夹
     *
     * @param resolver    内容解析器
     * @param id          笔记ID
     * @param srcFolderId 原始文件夹ID
     * @param desFolderId 目标文件夹ID
     */
    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId, long desFolderId) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.PARENT_ID, desFolderId);
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        resolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id), values, null, null);
    }

    /**
     * 批量将笔记移动到指定文件夹
     *
     * @param resolver 内容解析器
     * @param ids      要移动的笔记ID集合
     * @param folderId 目标文件夹ID
     * @return 如果移动成功或集合为空或为null，则返回true，否则返回false
     */
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids,
                                            long folderId) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }

        // 构建更新操作的列表
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            builder.withValue(NoteColumns.PARENT_ID, folderId);
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1);
            operationList.add(builder.build());
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            // 检查移动结果
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "move notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 获取除系统文件夹外的所有用户文件夹数量
     *
     * @param resolver 内容解析器
     * @return 用户文件夹数量
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{"COUNT(*)"},
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[]{String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);

        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    count = cursor.getInt(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "get folder count failed:" + e.toString());
                } finally {
                    cursor.close();
                }
            }
        }
        return count;
    }

    /**
     * 检查指定类型的笔记在数据库中是否可见
     *
     * @param resolver 内容解析器
     * @param noteId   笔记ID
     * @param type     笔记类型
     * @return 如果可见，则返回true，否则返回false
     */
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER,
                new String[]{String.valueOf(type)},
                null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查指定的笔记ID在数据库中是否存在
     *
     * @param resolver 内容解析器
     * @param noteId   笔记ID
     * @return 如果存在，则返回true，否则返回false
     */
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查指定的数据ID在数据库中是否存在
     *
     * @param resolver 内容解析器
     * @param dataId   数据ID
     * @return 如果存在，则返回true，否则返回false
     */
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查文件夹名称是否在数据库中已存在（不包括系统文件夹）
     *
     * @param resolver 内容解析器
     * @param name     文件夹名称
     * @return 如果已存在，则返回true，否则返回false
     */
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER +
                        " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER +
                        " AND " + NoteColumns.SNIPPET + "=?",
                new String[]{name}, null);
        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 获取指定文件夹中的笔记小部件信息集合
     *
     * @param resolver 内容解析器
     * @param folderId 文件夹ID
     * @return 笔记小部件信息集合
     */
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE},
                NoteColumns.PARENT_ID + "=?",
                new String[]{String.valueOf(folderId)},
                null);

        HashSet<AppWidgetAttribute> set = null;
        if (c != null) {
            if (c.moveToFirst()) {
                set = new HashSet<AppWidgetAttribute>();
                do {
                    try {
                        AppWidgetAttribute widget = new AppWidgetAttribute();
                        widget.widgetId = c.getInt(0);
                        widget.widgetType = c.getInt(1);
                        set.add(widget);
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, e.toString());
                    }
                } while (c.moveToNext());
            }
            c.close();
        }
        return set;
    }

    /**
     * 通过笔记ID获取关联的通话号码
     *
     * @param resolver 内容解析器
     * @param noteId   笔记ID
     * @return 通话号码，如果未找到则返回空字符串
     */
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.PHONE_NUMBER},
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String[]{String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                return cursor.getString(0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Get call number fails " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    /**
     * 根据电话号码和通话日期获取对应的笔记ID
     *
     * @param resolver    内容解析器
     * @param phoneNumber 电话号码
     * @param callDate    通话日期
     * @return 笔记ID，未找到则返回0
     */
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.NOTE_ID},
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ",?)",
                new String[]{String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber},
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    return cursor.getLong(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "Get call note id fails " + e.toString());
                }
            }
            cursor.close();
        }
        return 0;
    }

    /**
     * 根据笔记ID从数据库中获取笔记的摘要。
     *
     * @param resolver 内容解析器，用于查询数据库。
     * @param noteId   笔记的ID，用于定位特定的笔记。
     * @return 笔记的摘要字符串。如果找不到对应的笔记，将抛出IllegalArgumentException。
     */
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        // 使用内容解析器查询特定ID的笔记的摘要
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.SNIPPET},
                NoteColumns.ID + "=?",
                new String[]{String.valueOf(noteId)},
                null);

        if (cursor != null) {
            String snippet = "";
            // 如果查询结果不为空，尝试获取摘要
            if (cursor.moveToFirst()) {
                snippet = cursor.getString(0);
            }
            // 关闭游标
            cursor.close();
            return snippet;
        }
        // 如果找不到指定ID的笔记，抛出异常
        throw new IllegalArgumentException("Note is not found with id: " + noteId);
    }

    /**
     * 格式化摘要字符串。
     * 主要用于去除字符串两端的空白字符，以及截取至第一个换行符之前的内容。
     *
     * @param snippet 需要格式化的摘要字符串。
     * @return 格式化后的摘要字符串。
     */
    public static String getFormattedSnippet(String snippet) {
        // 如果摘要字符串不为空，进行格式化处理
        if (snippet != null) {
            snippet = snippet.trim(); // 去除两端的空白字符
            int index = snippet.indexOf('\n'); // 查找第一个换行符的位置
            if (index != -1) {
                snippet = snippet.substring(0, index); // 截取至第一个换行符之前
            }
        }
        return snippet;
    }
}
