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

package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.data.MetaData;
import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.SqlNote;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


public class GTaskManager {
    // GTaskManager类的标签，用于日志输出等。
    private static final String TAG = GTaskManager.class.getSimpleName();

    // 任务状态：成功。
    public static final int STATE_SUCCESS = 0;

    // 任务状态：网络错误。
    public static final int STATE_NETWORK_ERROR = 1;

    // 任务状态：内部错误。
    public static final int STATE_INTERNAL_ERROR = 2;

    // 任务状态：同步进行中。
    public static final int STATE_SYNC_IN_PROGRESS = 3;

    // 任务状态：同步已取消。
    public static final int STATE_SYNC_CANCELLED = 4;

    // GTaskManager的单例实例。
    private static GTaskManager mInstance = null;

    // 关联的Activity对象。
    private Activity mActivity;

    // 上下文对象。
    private Context mContext;

    // 内容解析器。
    private ContentResolver mContentResolver;

    // 标记是否正在同步。
    private boolean mSyncing;

    // 标记是否已取消同步。
    private boolean mCancelled;

    // 保存任务列表的HashMap，键为列表ID，值为任务列表对象。
    private HashMap<String, TaskList> mGTaskListHashMap;

    // 保存任务的HashMap，键为任务ID，值为任务对象。
    private HashMap<String, Node> mGTaskHashMap;

    // 保存元数据的HashMap，键为元数据ID，值为元数据对象。
    private HashMap<String, MetaData> mMetaHashMap;

    // 元数据列表。
    private TaskList mMetaList;

    // 本地删除任务ID的集合。
    private HashSet<Long> mLocalDeleteIdMap;

    // 保存任务全局ID到本地ID的映射的HashMap。
    private HashMap<String, Long> mGidToNid;

    // 保存本地ID到任务全局ID的映射的HashMap。
    private HashMap<Long, String> mNidToGid;

    // GTaskManager的私有构造函数，初始化各种状态和映射。
    private GTaskManager() {
        mSyncing = false;
        mCancelled = false;
        mGTaskListHashMap = new HashMap<String, TaskList>();
        mGTaskHashMap = new HashMap<String, Node>();
        mMetaHashMap = new HashMap<String, MetaData>();
        mMetaList = null;
        mLocalDeleteIdMap = new HashSet<Long>();
        mGidToNid = new HashMap<String, Long>();
        mNidToGid = new HashMap<Long, String>();
    }


    /**
     * 获取 GTaskManager 的单例对象。
     * 采用单例模式确保全局仅有一个 GTaskManager 实例。
     *
     * @return GTaskManager 的单例对象。
     */
    public static synchronized GTaskManager getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskManager();
        }
        return mInstance;
    }

    /**
     * 设置活动上下文。
     * 用于获取授权令牌。
     *
     * @param activity 当前活动对象。
     */
    public synchronized void setActivityContext(Activity activity) {
        mActivity = activity;
    }

    /**
     * 同步任务数据。
     * 会尝试与Google任务进行登录和数据同步，如果过程中发生错误或取消，则返回对应的状态码。
     *
     * @param context   上下文对象，用于执行同步操作。
     * @param asyncTask 异步任务对象，用于在同步过程中更新进度。
     * @return 同步操作的状态码，可以是正在同步、网络错误、内部错误或同步取消。
     */
    public int sync(Context context, GTaskASyncTask asyncTask) {
        if (mSyncing) {
            Log.d(TAG, "Sync is in progress");
            return STATE_SYNC_IN_PROGRESS;
        }
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mSyncing = true;
        mCancelled = false;
        // 清理同步相关的数据结构
        mGTaskListHashMap.clear();
        mGTaskHashMap.clear();
        mMetaHashMap.clear();
        mLocalDeleteIdMap.clear();
        mGidToNid.clear();
        mNidToGid.clear();

        try {
            GTaskClient client = GTaskClient.getInstance();
            client.resetUpdateArray();

            // 尝试登录 Google 任务服务
            if (!mCancelled) {
                if (!client.login(mActivity)) {
                    throw new NetworkFailureException("login google task failed");
                }
            }

            // 初始化 Google 任务列表
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_init_list));
            initGTaskList();

            // 执行内容同步工作
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_syncing));
            syncContent();
        } catch (NetworkFailureException e) {
            Log.e(TAG, e.toString());
            return STATE_NETWORK_ERROR;
        } catch (ActionFailureException e) {
            Log.e(TAG, e.toString());
            return STATE_INTERNAL_ERROR;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return STATE_INTERNAL_ERROR;
        } finally {
            // 无论成功或失败，最后都清理数据结构
            mGTaskListHashMap.clear();
            mGTaskHashMap.clear();
            mMetaHashMap.clear();
            mLocalDeleteIdMap.clear();
            mGidToNid.clear();
            mNidToGid.clear();
            mSyncing = false;
        }

        return mCancelled ? STATE_SYNC_CANCELLED : STATE_SUCCESS;
    }

    /**
     * 初始化 GTask 列表。
     * 该方法首先检查操作是否已被取消，然后从 GTaskClient 获取任务列表信息，并初始化元数据列表和任务列表。
     * 如果过程中发生网络错误，可能会抛出 NetworkFailureException 异常。
     *
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常。
     */
    private void initGTaskList() throws NetworkFailureException {
        if (mCancelled) // 检查是否取消了操作
            return;

        GTaskClient client = GTaskClient.getInstance(); // 获取 GTask 客户端实例

        try {
            JSONArray jsTaskLists = client.getTaskLists(); // 从客户端获取任务列表数组

            // 初始化元数据列表
            mMetaList = null;
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // 寻找并初始化元数据列表
                if (name
                        .equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    mMetaList = new TaskList();
                    mMetaList.setContentByRemoteJSON(object);

                    // 加载元数据
                    JSONArray jsMetas = client.getTaskList(gid);
                    for (int j = 0; j < jsMetas.length(); j++) {
                        object = (JSONObject) jsMetas.getJSONObject(j);
                        MetaData metaData = new MetaData();
                        metaData.setContentByRemoteJSON(object);
                        if (metaData.isWorthSaving()) {
                            mMetaList.addChildTask(metaData);
                            if (metaData.getGid() != null) {
                                mMetaHashMap.put(metaData.getRelatedGid(), metaData);
                            }
                        }
                    }
                }
            }

            // 如果元数据列表不存在，则创建新的元数据列表
            if (mMetaList == null) {
                mMetaList = new TaskList();
                mMetaList.setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                        + GTaskStringUtils.FOLDER_META);
                GTaskClient.getInstance().createTaskList(mMetaList);
            }

            // 初始化任务列表
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // 创建并初始化除元数据之外的其他任务列表
                if (name.startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX)
                        && !name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                        + GTaskStringUtils.FOLDER_META)) {
                    TaskList tasklist = new TaskList();
                    tasklist.setContentByRemoteJSON(object);
                    mGTaskListHashMap.put(gid, tasklist);
                    mGTaskHashMap.put(gid, tasklist);

                    // 加载任务
                    JSONArray jsTasks = client.getTaskList(gid);
                    for (int j = 0; j < jsTasks.length(); j++) {
                        object = (JSONObject) jsTasks.getJSONObject(j);
                        gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                        Task task = new Task();
                        task.setContentByRemoteJSON(object);
                        if (task.isWorthSaving()) {
                            task.setMetaInfo(mMetaHashMap.get(gid));
                            tasklist.addChildTask(task);
                            mGTaskHashMap.put(gid, task);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            // 处理 JSON 解析异常
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("initGTaskList: handling JSONObject failed");
        }
    }


    /**
     * 同步内容数据。
     * 该方法首先处理本地已删除的笔记，然后同步文件夹信息，接着处理数据库中存在的笔记，
     * 最后处理剩余的项目，并更新本地同步ID。如果在过程中检测到网络失败，则抛出网络失败异常。
     *
     * @throws NetworkFailureException 如果在网络通信过程中发生失败
     */
    private void syncContent() throws NetworkFailureException {
        int syncType;
        Cursor c = null;
        String gid;
        Node node;

        mLocalDeleteIdMap.clear();  // 清除本地删除映射表

        if (mCancelled) {
            return;  // 如果操作已被取消，则直接返回
        }

        // 处理本地删除的笔记
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id=?)", new String[]{
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, null);
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从映射表中移除
                        doContentSync(Node.SYNC_ACTION_DEL_REMOTE, node, c); // 执行内容同步
                    }

                    mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 添加到本地删除映射表
                }
            } else {
                Log.w(TAG, "failed to query trash folder");
            }
        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // 首先同步文件夹信息
        syncFolder();

        // 处理数据库中存在的笔记
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // 从映射表中移除
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // 更新ID映射
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid);
                        syncType = node.getSyncAction(c); // 获取同步动作
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // 如果没有GTask ID，则视为本地新增
                            syncType = Node.SYNC_ACTION_ADD_REMOTE;
                        } else {
                            // 如果有GTask ID但本地不存在，则视为远程删除
                            syncType = Node.SYNC_ACTION_DEL_LOCAL;
                        }
                    }
                    doContentSync(syncType, node, c); // 执行内容同步
                }
            } else {
                Log.w(TAG, "failed to query existing note in database");
            }

        } finally {
            if (c != null) {
                c.close(); // 关闭游标
                c = null;
            }
        }

        // 处理剩余项目
        Iterator<Map.Entry<String, Node>> iter = mGTaskHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Node> entry = iter.next();
            node = entry.getValue();
            doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // 将剩余项目作为本地新增处理
        }

        // 检查是否取消操作，清理本地删除表，并更新本地同步ID
        if (!mCancelled) {
            // 批量删除本地已删除的笔记，如果失败则抛出异常
            if (!DataUtils.batchDeleteNotes(mContentResolver, mLocalDeleteIdMap)) {
                throw new ActionFailureException("failed to batch-delete local deleted notes");
            }
        }

        // 刷新本地同步ID
        if (!mCancelled) {
            GTaskClient.getInstance().commitUpdate(); // 提交更新
            refreshLocalSyncId(); // 刷新本地同步ID
        }

    }

    /**
     * 同步文件夹数据。
     * 该方法负责同步根文件夹、通话记录文件夹以及本地和远程存在的文件夹。
     * 它会根据文件夹的当前状态（是否存在、是否已同步）采取相应的同步操作，如添加、更新或删除。
     *
     * @throws NetworkFailureException 如果网络操作失败
     */
    private void syncFolder() throws NetworkFailureException {
        Cursor c = null;
        String gid;
        Node node;
        int syncType;

        if (mCancelled) {
            return;
        }

        // 同步根文件夹
        try {
            c = mContentResolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                    Notes.ID_ROOT_FOLDER), SqlNote.PROJECTION_NOTE, null, null, null);
            if (c != null) {
                c.moveToNext();
                gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                node = mGTaskHashMap.get(gid);
                // 判断节点是否为空，不为空则更新，为空则添加
                if (node != null) {
                    mGTaskHashMap.remove(gid);
                    mGidToNid.put(gid, (long) Notes.ID_ROOT_FOLDER);
                    mNidToGid.put((long) Notes.ID_ROOT_FOLDER, gid);
                    // 仅当系统文件夹的名称需要更新时执行内容同步
                    if (!node.getName().equals(
                            GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT))
                        doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                } else {
                    doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                }
            } else {
                Log.w(TAG, "failed to query root folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 同步通话记录文件夹
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE, "(_id=?)",
                    new String[]{
                            String.valueOf(Notes.ID_CALL_RECORD_FOLDER)
                    }, null);
            if (c != null) {
                if (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    // 判断节点是否为空，不为空则更新，为空则添加
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, (long) Notes.ID_CALL_RECORD_FOLDER);
                        mNidToGid.put((long) Notes.ID_CALL_RECORD_FOLDER, gid);
                        // 仅当系统文件夹的名称需要更新时执行内容同步
                        if (!node.getName().equals(
                                GTaskStringUtils.MIUI_FOLDER_PREFFIX
                                        + GTaskStringUtils.FOLDER_CALL_NOTE))
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                    } else {
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                    }
                }
            } else {
                Log.w(TAG, "failed to query call note folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 同步本地已存在的文件夹
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    // 判断节点是否为空，不为空则更新，为空则根据情况添加或删除
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN));
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid);
                        syncType = node.getSyncAction(c);
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // 本地添加
                            syncType = Node.SYNC_ACTION_ADD_REMOTE;
                        } else {
                            // 远程删除
                            syncType = Node.SYNC_ACTION_DEL_LOCAL;
                        }
                    }
                    doContentSync(syncType, node, c);
                }
            } else {
                Log.w(TAG, "failed to query existing folder");
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 同步远程添加的文件夹
        Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TaskList> entry = iter.next();
            gid = entry.getKey();
            node = entry.getValue();
            if (mGTaskHashMap.containsKey(gid)) {
                mGTaskHashMap.remove(gid);
                doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null);
            }
        }

        if (!mCancelled)
            GTaskClient.getInstance().commitUpdate();
    }

    /**
     * 根据指定的同步类型，对节点内容进行同步操作。
     *
     * @param syncType 同步操作的类型，决定是添加、删除、还是更新节点。
     * @param node     要进行同步操作的节点。
     * @param c        游标，用于在本地数据库操作时获取额外信息（在删除操作中使用）。
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常。
     */
    private void doContentSync(int syncType, Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) { // 检查是否已取消同步操作
            return;
        }

        MetaData meta;
        switch (syncType) {
            case Node.SYNC_ACTION_ADD_LOCAL: // 添加本地节点
                addLocalNode(node);
                break;
            case Node.SYNC_ACTION_ADD_REMOTE: // 添加远程节点
                addRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_DEL_LOCAL: // 删除本地节点
                meta = mMetaHashMap.get(c.getString(SqlNote.GTASK_ID_COLUMN));
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 从服务器删除节点
                }
                mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // 记录已删除的本地节点ID
                break;
            case Node.SYNC_ACTION_DEL_REMOTE: // 删除远程节点
                meta = mMetaHashMap.get(node.getGid());
                if (meta != null) {
                    GTaskClient.getInstance().deleteNode(meta); // 从服务器删除节点
                }
                GTaskClient.getInstance().deleteNode(node); // 直接从本地数据库删除节点
                break;
            case Node.SYNC_ACTION_UPDATE_LOCAL: // 更新本地节点
                updateLocalNode(node, c);
                break;
            case Node.SYNC_ACTION_UPDATE_REMOTE: // 更新远程节点
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_UPDATE_CONFLICT: // 处理更新冲突
                // 目前简单地采用本地更新，未来可能需要合并双方修改
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_NONE: // 无操作
                break;
            case Node.SYNC_ACTION_ERROR: // 默认错误处理
            default:
                throw new ActionFailureException("unkown sync action type"); // 抛出未知同步操作类型的异常
        }
    }


    /**
     * 将本地节点添加到数据库中。
     * 该方法首先检查操作是否已被取消，然后根据节点类型（任务列表或任务）创建相应的 SqlNote 对象。
     * 对于任务列表节点，会根据节点名称（默认文件夹或通话记录文件夹）设置特殊的 SqlNote 属性；
     * 对于任务节点，会从节点内容中创建 JSON 对象，并根据需要调整其中的 ID 字段（如果这些 ID 在数据库中已存在）。
     * 最后，该方法会将 SqlNote 对象提交到数据库，并更新相关的 ID 映射关系。
     *
     * @param node 要添加的本地节点，不应为 null。
     * @throws NetworkFailureException 如果添加节点过程中检测到网络失败。
     */
    private void addLocalNode(Node node) throws NetworkFailureException {
        if (mCancelled) {
            return; // 如果操作已取消，则直接返回，不执行添加操作
        }

        SqlNote sqlNote;
        if (node instanceof TaskList) {
            // 处理任务列表节点，根据节点名称设置特殊的 SqlNote 属性
            if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT)) {
                sqlNote = new SqlNote(mContext, Notes.ID_ROOT_FOLDER);
            } else if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE)) {
                sqlNote = new SqlNote(mContext, Notes.ID_CALL_RECORD_FOLDER);
            } else {
                sqlNote = new SqlNote(mContext);
                sqlNote.setContent(node.getLocalJSONFromContent());
                sqlNote.setParentId(Notes.ID_ROOT_FOLDER);
            }
        } else {
            // 处理任务节点，从节点内容创建 JSON 对象，并根据需要调整 ID 字段
            sqlNote = new SqlNote(mContext);
            JSONObject js = node.getLocalJSONFromContent();
            try {
                // 检查并处理 JSON 对象中的 Note 和 Data ID 字段
                if (js.has(GTaskStringUtils.META_HEAD_NOTE)) {
                    JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                    if (note.has(NoteColumns.ID)) {
                        long id = note.getLong(NoteColumns.ID);
                        if (DataUtils.existInNoteDatabase(mContentResolver, id)) {
                            // 如果笔记 ID 已存在，则移除该 ID
                            note.remove(NoteColumns.ID);
                        }
                    }
                }

                if (js.has(GTaskStringUtils.META_HEAD_DATA)) {
                    JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject data = dataArray.getJSONObject(i);
                        if (data.has(DataColumns.ID)) {
                            long dataId = data.getLong(DataColumns.ID);
                            if (DataUtils.existInDataDatabase(mContentResolver, dataId)) {
                                // 如果数据 ID 已存在，则移除该 ID
                                data.remove(DataColumns.ID);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                e.printStackTrace();
            }
            sqlNote.setContent(js);

            // 设置节点的父 ID
            Long parentId = mGidToNid.get(((Task) node).getParent().getGid());
            if (parentId == null) {
                Log.e(TAG, "cannot find task's parent id locally");
                throw new ActionFailureException("cannot add local node");
            }
            sqlNote.setParentId(parentId.longValue());
        }

        // 提交 SqlNote 到数据库，并更新 ID 映射关系
        sqlNote.setGtaskId(node.getGid());
        sqlNote.commit(false);

        mGidToNid.put(node.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), node.getGid());

        // 更新远程元数据
        updateRemoteMeta(node.getGid(), sqlNote);
    }


    /**
     * 更新本地节点信息
     *
     * @param node 需要更新的节点
     * @param c    数据库游标，用于操作数据库
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常
     */
    private void updateLocalNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote;
        // 根据节点内容更新本地数据库中的笔记
        sqlNote = new SqlNote(mContext, c);
        sqlNote.setContent(node.getLocalJSONFromContent());

        // 确定父节点ID，任务则查找父任务ID，否则默认为根文件夹ID
        Long parentId = (node instanceof Task) ? mGidToNid.get(((Task) node).getParent().getGid())
                : new Long(Notes.ID_ROOT_FOLDER);
        if (parentId == null) {
            Log.e(TAG, "cannot find task's parent id locally");
            throw new ActionFailureException("cannot update local node");
        }
        sqlNote.setParentId(parentId.longValue());
        sqlNote.commit(true);

        // 更新远程元数据
        updateRemoteMeta(node.getGid(), sqlNote);
    }

    /**
     * 在远程添加节点信息
     *
     * @param node 需要添加的节点
     * @param c    数据库游标，用于操作数据库
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常
     */
    private void addRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c);
        Node n;

        // 如果是任务类型，则远程创建任务；否则，根据条件判断是否需要创建新的任务列表
        if (sqlNote.isNoteType()) {
            Task task = new Task();
            task.setContentByLocalJSON(sqlNote.getContent());

            // 查找任务所属的任务列表ID
            String parentGid = mNidToGid.get(sqlNote.getParentId());
            if (parentGid == null) {
                Log.e(TAG, "cannot find task's parent tasklist");
                throw new ActionFailureException("cannot add remote task");
            }
            mGTaskListHashMap.get(parentGid).addChildTask(task);

            GTaskClient.getInstance().createTask(task);
            n = (Node) task;

            // 更新远程元数据
            updateRemoteMeta(task.getGid(), sqlNote);
        } else {
            TaskList tasklist = null;

            // 判断是否需要创建新的任务列表
            String folderName = GTaskStringUtils.MIUI_FOLDER_PREFFIX;
            if (sqlNote.getId() == Notes.ID_ROOT_FOLDER)
                folderName += GTaskStringUtils.FOLDER_DEFAULT;
            else if (sqlNote.getId() == Notes.ID_CALL_RECORD_FOLDER)
                folderName += GTaskStringUtils.FOLDER_CALL_NOTE;
            else
                folderName += sqlNote.getSnippet();

            // 在已有的任务列表中查找匹配的条目
            Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, TaskList> entry = iter.next();
                String gid = entry.getKey();
                TaskList list = entry.getValue();

                if (list.getName().equals(folderName)) {
                    tasklist = list;
                    if (mGTaskHashMap.containsKey(gid)) {
                        mGTaskHashMap.remove(gid);
                    }
                    break;
                }
            }

            // 如果没有找到匹配的任务列表，则创建新的任务列表
            if (tasklist == null) {
                tasklist = new TaskList();
                tasklist.setContentByLocalJSON(sqlNote.getContent());
                GTaskClient.getInstance().createTaskList(tasklist);
                mGTaskListHashMap.put(tasklist.getGid(), tasklist);
            }
            n = (Node) tasklist;
        }

        // 更新本地数据库中的笔记信息
        sqlNote.setGtaskId(n.getGid());
        sqlNote.commit(false);
        sqlNote.resetLocalModified();
        sqlNote.commit(true);

        // 更新GID和ID的映射关系
        mGidToNid.put(n.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), n.getGid());
    }


    /**
     * 更新远程节点信息。
     *
     * @param node 需要更新的节点
     * @param c    数据库游标，用于获取节点的详细信息
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常
     */
    private void updateRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        if (mCancelled) { // 检查是否已取消操作
            return;
        }

        SqlNote sqlNote = new SqlNote(mContext, c); // 从数据库游标中创建 SqlNote 对象

        // 使用本地 JSON 格式更新远程节点内容
        node.setContentByLocalJSON(sqlNote.getContent());
        GTaskClient.getInstance().addUpdateNode(node); // 将节点添加到更新队列

        // 更新元数据
        updateRemoteMeta(node.getGid(), sqlNote);

        // 如果是笔记类型，检查并移动任务
        if (sqlNote.isNoteType()) {
            Task task = (Task) node;
            TaskList preParentList = task.getParent(); // 获取任务的当前父任务列表

            String curParentGid = mNidToGid.get(sqlNote.getParentId()); // 获取当前父任务列表的 GID
            if (curParentGid == null) {
                Log.e(TAG, "cannot find task's parent tasklist");
                throw new ActionFailureException("cannot update remote task");
            }
            TaskList curParentList = mGTaskListHashMap.get(curParentGid); // 获取当前父任务列表对象

            // 如果任务的父任务列表发生变化，则移动任务
            if (preParentList != curParentList) {
                preParentList.removeChildTask(task);
                curParentList.addChildTask(task);
                GTaskClient.getInstance().moveTask(task, preParentList, curParentList);
            }
        }

        // 重置本地修改标志，并提交更改
        sqlNote.resetLocalModified();
        sqlNote.commit(true);
    }

    /**
     * 更新远程元数据。
     *
     * @param gid     元数据的全局标识符
     * @param sqlNote 包含元数据内容的 SqlNote 对象
     * @throws NetworkFailureException 如果网络操作失败，则抛出此异常
     */
    private void updateRemoteMeta(String gid, SqlNote sqlNote) throws NetworkFailureException {
        if (sqlNote != null && sqlNote.isNoteType()) { // 确保是笔记类型
            MetaData metaData = mMetaHashMap.get(gid); // 尝试获取现有的元数据对象
            if (metaData != null) {
                // 更新元数据内容并加入更新队列
                metaData.setMeta(gid, sqlNote.getContent());
                GTaskClient.getInstance().addUpdateNode(metaData);
            } else {
                // 如果元数据不存在，则创建新的元数据对象并添加到远程
                metaData = new MetaData();
                metaData.setMeta(gid, sqlNote.getContent());
                mMetaList.addChildTask(metaData);
                mMetaHashMap.put(gid, metaData);
                GTaskClient.getInstance().createTask(metaData);
            }
        }
    }


    /**
     * 刷新本地同步ID。
     * 该方法首先获取最新的gtask列表，然后通过查询本地笔记内容来更新这些笔记的同步ID。
     * 如果在查询过程中发现有本地项目在同步后没有对应的gtask ID，则抛出ActionFailureException异常。
     *
     * @throws NetworkFailureException 如果网络操作失败。
     * @throws ActionFailureException  如果在同步后发现有本地项目没有对应的gtask ID。
     */
    private void refreshLocalSyncId() throws NetworkFailureException {
        if (mCancelled) {
            return;
        }

        // 清空现有的gtask列表和元数据，准备获取最新的数据
        mGTaskHashMap.clear();
        mGTaskListHashMap.clear();
        mMetaHashMap.clear();
        initGTaskList();

        Cursor c = null;
        try {
            // 查询本地笔记内容，准备更新同步ID
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SYNC_ID, node.getLastModified());
                        mContentResolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                                c.getLong(SqlNote.ID_COLUMN)), values, null, null);
                    } else {
                        // 如果查询到的笔记没有对应的gtask ID，则抛出异常
                        Log.e(TAG, "something is missed");
                        throw new ActionFailureException(
                                "some local items don't have gid after sync");
                    }
                }
            } else {
                // 如果查询操作失败，记录警告信息
                Log.w(TAG, "failed to query local note to refresh sync id");
            }
        } finally {
            // 释放Cursor资源
            if (c != null) {
                c.close();
                c = null;
            }
        }
    }

    /**
     * 获取同步账户的名称。
     *
     * @return 同步账户的名称。
     */
    public String getSyncAccount() {
        return GTaskClient.getInstance().getSyncAccount().name;
    }

    /**
     * 取消同步操作。
     * 设置取消标志，终止正在进行的同步操作。
     */
    public void cancelSync() {
        mCancelled = true;
    }

}
