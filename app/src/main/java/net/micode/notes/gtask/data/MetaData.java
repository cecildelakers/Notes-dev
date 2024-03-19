/**
 * MetaData类，继承自Task类，用于处理与任务相关的元数据。
 */
package net.micode.notes.gtask.data;

import android.database.Cursor;
import android.util.Log;

import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class MetaData extends Task {
    private final static String TAG = MetaData.class.getSimpleName(); // 日志标签

    private String mRelatedGid = null; // 与任务相关的全局ID

    /**
     * 设置元数据。
     *
     * @param gid      任务的全局ID。
     * @param metaInfo 元信息的JSON对象。
     */
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid); // 将任务的全局ID添加到元信息中
        } catch (JSONException e) {
            Log.e(TAG, "failed to put related gid");
        }
        setNotes(metaInfo.toString()); // 将元信息设置为任务的笔记
        setName(GTaskStringUtils.META_NOTE_NAME); // 设置任务的名称为特定的元数据标志名称
    }

    /**
     * 获取与任务相关的全局ID。
     *
     * @return 相关的全局ID字符串。
     */
    public String getRelatedGid() {
        return mRelatedGid;
    }

    /**
     * 判断任务是否值得保存。
     *
     * @return 如果任务的笔记字段不为空，则返回true，表示值得保存。
     */
    @Override
    public boolean isWorthSaving() {
        return getNotes() != null;
    }

    /**
     * 通过远程JSON对象设置内容。
     *
     * @param js JSON对象，包含远程任务的内容。
     */
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        super.setContentByRemoteJSON(js);
        if (getNotes() != null) {
            try {
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID); // 从笔记中提取相关的全局ID
            } catch (JSONException e) {
                Log.w(TAG, "failed to get related gid");
                mRelatedGid = null; // 提取失败时，设置相关ID为null
            }
        }
    }

    /**
     * 通过本地JSON对象设置内容。此方法不应被调用。
     *
     * @param js 本地JSON对象。
     */
    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // this function should not be called
        throw new IllegalAccessError("MetaData:setContentByLocalJSON should not be called");
    }

    /**
     * 从内容生成本地JSON对象。此方法不应被调用。
     *
     * @return 生成的JSON对象。
     */
    @Override
    public JSONObject getLocalJSONFromContent() {
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent should not be called");
    }

    /**
     * 获取同步操作类型。此方法不应被调用。
     *
     * @param c 数据库游标，指向当前任务。
     * @return 同步操作的类型。
     */
    @Override
    public int getSyncAction(Cursor c) {
        throw new IllegalAccessError("MetaData:getSyncAction should not be called");
    }

}
