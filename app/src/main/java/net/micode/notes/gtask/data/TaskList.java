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

import android.database.Cursor;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * 任务列表类，继承自Node类。用于管理一组任务（Task）对象。
 */
public class TaskList extends Node {
    // 日志标签
    private static final String TAG = TaskList.class.getSimpleName();

    // 列表中任务的索引
    private int mIndex;

    // 存储子任务的列表
    private ArrayList<Task> mChildren;

    /**
     * 构造函数，初始化任务列表。
     */
    public TaskList() {
        super();
        mChildren = new ArrayList<Task>();
        mIndex = 1;
    }

    /**
     * 生成创建任务列表的动作JSON对象。
     *
     * @param actionId 动作标识符
     * @return 包含创建任务列表动作的JSON对象
     * @throws ActionFailureException 如果生成JSON对象失败，则抛出异常
     */
    public JSONObject getCreateAction(int actionId) throws ActionFailureException {
        JSONObject js = new JSONObject();

        try {
            // 设置动作类型为创建
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);

            // 设置动作标识符
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // 设置索引
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mIndex);

            // 设置实体变化信息
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate tasklist-create jsonobject");
        }

        return js;
    }

    /**
     * 生成更新任务列表的动作JSON对象。
     *
     * @param actionId 动作标识符
     * @return 包含更新任务列表动作的JSON对象
     * @throws ActionFailureException 如果生成JSON对象失败，则抛出异常
     */
    public JSONObject getUpdateAction(int actionId) throws ActionFailureException {
        JSONObject js = new JSONObject();

        try {
            // 设置动作类型为更新
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE);

            // 设置动作标识符
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);

            // 设置任务列表ID
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid());

            // 设置实体变化信息
            JSONObject entity = new JSONObject();
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate tasklist-update jsonobject");
        }

        return js;
    }

    /**
     * 根据远程JSON对象设置任务列表的内容。
     *
     * @param js 远程获取的JSON对象
     * @throws ActionFailureException 如果从JSON对象中获取内容失败，则抛出异常
     */
    public void setContentByRemoteJSON(JSONObject js) throws ActionFailureException {
        if (js != null) {
            try {
                // 设置ID
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID));
                }

                // 设置最后修改时间
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED));
                }

                // 设置名称
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME));
                }

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
                throw new ActionFailureException("fail to get tasklist content from jsonobject");
            }
        }
    }

    /**
     * 根据本地JSON对象设置任务列表的内容。
     *
     * @param js 本地获取的JSON对象
     * @throws ActionFailureException 如果从JSON对象中获取内容失败，则抛出异常
     */
    public void setContentByLocalJSON(JSONObject js) throws ActionFailureException {
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)) {
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable");
            return;
        }

        try {
            JSONObject folder = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);

            // 根据类型设置任务列表名称
            if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                String name = folder.getString(NoteColumns.SNIPPET);
                setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + name);
            } else if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                if (folder.getLong(NoteColumns.ID) == Notes.ID_ROOT_FOLDER)
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT);
                else if (folder.getLong(NoteColumns.ID) == Notes.ID_CALL_RECORD_FOLDER)
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                            + GTaskStringUtils.FOLDER_CALL_NOTE);
                else
                    Log.e(TAG, "invalid system folder");
            } else {
                Log.e(TAG, "error type");
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to set tasklist content from local json object");
        }
    }

    /**
     * 从任务列表内容生成本地JSON对象。
     *
     * @return 本地JSON对象代表的任务列表内容
     */
    public JSONObject getLocalJSONFromContent() {
        try {
            JSONObject js = new JSONObject();
            JSONObject folder = new JSONObject();

            // 设置任务列表名称
            String folderName = getName();
            if (getName().startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX))
                folderName = folderName.substring(GTaskStringUtils.MIUI_FOLDER_PREFFIX.length(),
                        folderName.length());
            folder.put(NoteColumns.SNIPPET, folderName);
            // 根据名称判断类型
            if (folderName.equals(GTaskStringUtils.FOLDER_DEFAULT)
                    || folderName.equals(GTaskStringUtils.FOLDER_CALL_NOTE))
                folder.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
            else
                folder.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);

            js.put(GTaskStringUtils.META_HEAD_NOTE, folder);

            return js;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据本地数据库游标确定同步动作。
     *
     * @param c 数据库游标，指向当前任务列表的行
     * @return 同步动作的类型
     */
    public int getSyncAction(Cursor c) {
        try {
            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // 无本地更新
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 双方均无更新
                    return SYNC_ACTION_NONE;
                } else {
                    // 应用远程更新到本地
                    return SYNC_ACTION_UPDATE_LOCAL;
                }
            } else {
                // 验证GTask ID是否匹配
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    Log.e(TAG, "gtask id doesn't match");
                    return SYNC_ACTION_ERROR;
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 仅本地有修改
                    return SYNC_ACTION_UPDATE_REMOTE;
                } else {
                    // 对于文件夹冲突，仅应用本地修改
                    return SYNC_ACTION_UPDATE_REMOTE;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

        return SYNC_ACTION_ERROR;
    }

    /**
     * 获取子任务数量。
     *
     * @return 子任务数量
     */
    public int getChildTaskCount() {
        return mChildren.size();
    }

    /**
     * 添加一个子任务到列表中。
     *
     * @param task 要添加的子任务
     * @return 如果添加成功返回true，否则返回false
     */
    public boolean addChildTask(Task task) {
        boolean ret = false;
        if (task != null && !mChildren.contains(task)) {
            ret = mChildren.add(task);
            if (ret) {
                // 设置前置兄弟节点和父节点
                task.setPriorSibling(mChildren.isEmpty() ? null : mChildren
                        .get(mChildren.size() - 1));
                task.setParent(this);
            }
        }
        return ret;
    }

    /**
     * 在指定索引位置添加一个子任务。
     *
     * @param task  要添加的子任务
     * @param index 子任务要插入的索引位置
     * @return 如果添加成功返回true，否则返回false
     */
    public boolean addChildTask(Task task, int index) {
        if (index < 0 || index > mChildren.size()) {
            Log.e(TAG, "add child task: invalid index");
            return false;
        }

        int pos = mChildren.indexOf(task);
        if (task != null && pos == -1) {
            mChildren.add(index, task);

            // 更新任务列表
            Task preTask = null;
            Task afterTask = null;
            if (index != 0)
                preTask = mChildren.get(index - 1);
            if (index != mChildren.size() - 1)
                afterTask = mChildren.get(index + 1);

            task.setPriorSibling(preTask);
            if (afterTask != null)
                afterTask.setPriorSibling(task);
        }

        return true;
    }

    /**
     * 从列表中移除一个子任务。
     *
     * @param task 要移除的子任务
     * @return 如果移除成功返回true，否则返回false
     */
    public boolean removeChildTask(Task task) {
        boolean ret = false;
        int index = mChildren.indexOf(task);
        if (index != -1) {
            ret = mChildren.remove(task);

            if (ret) {
                // 重置前置兄弟节点和父节点
                task.setPriorSibling(null);
                task.setParent(null);

                // 更新任务列表
                if (index != mChildren.size()) {
                    mChildren.get(index).setPriorSibling(
                            index == 0 ? null : mChildren.get(index - 1));
                }
            }
        }
        return ret;
    }

    /**
     * 移动子任务到指定索引位置。
     *
     * @param task  要移动的子任务
     * @param index 子任务要移动到的索引位置
     * @return 如果移动成功返回true，否则返回false
     */
    public boolean moveChildTask(Task task, int index) {
        if (index < 0 || index >= mChildren.size()) {
            Log.e(TAG, "move child task: invalid index");
            return false;
        }

        int pos = mChildren.indexOf(task);
        if (pos == -1) {
            Log.e(TAG, "move child task: the task should in the list");
            return false;
        }

        if (pos == index)
            return true;
        return (removeChildTask(task) && addChildTask(task, index));
    }

    /**
     * 根据全局标识符(gid)查找子任务。
     *
     * @param gid 要查找的子任务的全局标识符
     * @return 如果找到匹配的子任务，则返回该任务对象；否则返回null。
     */
    public Task findChildTaskByGid(String gid) {
        // 遍历子任务列表，查找gid匹配的子任务
        for (int i = 0; i < mChildren.size(); i++) {
            Task t = mChildren.get(i);
            if (t.getGid().equals(gid)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 获取指定子任务在列表中的索引位置。
     *
     * @param task 要查找索引的子任务对象
     * @return 子任务在列表中的索引位置；如果未找到该任务，则返回-1。
     */
    public int getChildTaskIndex(Task task) {
        // 返回任务在子任务列表中的索引
        return mChildren.indexOf(task);
    }

    /**
     * 根据索引获取子任务。
     *
     * @param index 子任务的索引位置
     * @return 如果索引有效，则返回对应位置的子任务对象；否则返回null。
     */
    public Task getChildTaskByIndex(int index) {
        // 检查索引是否有效，然后返回对应位置的子任务
        if (index < 0 || index >= mChildren.size()) {
            Log.e(TAG, "getTaskByIndex: invalid index");
            return null;
        }
        return mChildren.get(index);
    }

    /**
     * 通过遍历子任务列表，查找并返回匹配指定gid的子任务。
     *
     * @param gid 要查找的子任务的全局标识符
     * @return 如果找到匹配的子任务，则返回该任务对象；否则返回null。
     */
    public Task getChilTaskByGid(String gid) {
        // 遍历子任务列表，查找gid匹配的子任务
        for (Task task : mChildren) {
            if (task.getGid().equals(gid))
                return task;
        }
        return null;
    }

    /**
     * 获取所有子任务的列表。
     *
     * @return 子任务列表，作为一个ArrayList<Task>返回。
     */
    public ArrayList<Task> getChildTaskList() {
        // 返回存储子任务的列表
        return this.mChildren;
    }

    /**
     * 设置当前任务的索引。
     *
     * @param index 要设置的索引值。
     */
    public void setIndex(int index) {
        this.mIndex = index;
    }

    /**
     * 获取当前任务的索引。
     *
     * @return 当前任务的索引值。
     */
    public int getIndex() {
        return this.mIndex;
    }
}

