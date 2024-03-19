/*
 * Node类定义了一个节点的基本属性和操作，用于数据同步时的表现和转换。
 * 它是用于表示通用数据节点的抽象类，具体的数据操作和格式转换由其子类实现。
 */

package net.micode.notes.gtask.data;

import android.database.Cursor;

import org.json.JSONObject;

// 定义节点同步动作的常量
public abstract class Node {
    public static final int SYNC_ACTION_NONE = 0; // 无动作
    public static final int SYNC_ACTION_ADD_REMOTE = 1; // 添加远程节点
    public static final int SYNC_ACTION_ADD_LOCAL = 2; // 添加本地节点
    public static final int SYNC_ACTION_DEL_REMOTE = 3; // 删除远程节点
    public static final int SYNC_ACTION_DEL_LOCAL = 4; // 删除本地节点
    public static final int SYNC_ACTION_UPDATE_REMOTE = 5; // 更新远程节点
    public static final int SYNC_ACTION_UPDATE_LOCAL = 6; // 更新本地节点
    public static final int SYNC_ACTION_UPDATE_CONFLICT = 7; // 更新冲突
    public static final int SYNC_ACTION_ERROR = 8; // 同步错误

    private String mGid; // 全局唯一标识符
    private String mName; // 节点名称
    private long mLastModified; // 最后修改时间
    private boolean mDeleted; // 节点是否被删除的标志

    // 构造函数，初始化节点属性
    public Node() {
        mGid = null;
        mName = "";
        mLastModified = 0;
        mDeleted = false;
    }

    // 生成创建节点的JSON动作
    public abstract JSONObject getCreateAction(int actionId);

    // 生成更新节点的JSON动作
    public abstract JSONObject getUpdateAction(int actionId);

    // 根据远程JSON内容设置节点内容
    public abstract void setContentByRemoteJSON(JSONObject js);

    // 根据本地JSON内容设置节点内容
    public abstract void setContentByLocalJSON(JSONObject js);

    // 从内容生成本地JSON表示
    public abstract JSONObject getLocalJSONFromContent();

    // 根据Cursor获取同步动作
    public abstract int getSyncAction(Cursor c);

    // 设置节点的全局唯一标识符
    public void setGid(String gid) {
        this.mGid = gid;
    }

    // 设置节点名称
    public void setName(String name) {
        this.mName = name;
    }

    // 设置节点最后修改时间
    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    // 设置节点是否被删除
    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    // 获取节点的全局唯一标识符
    public String getGid() {
        return this.mGid;
    }

    // 获取节点名称
    public String getName() {
        return this.mName;
    }

    // 获取节点最后修改时间
    public long getLastModified() {
        return this.mLastModified;
    }

    // 获取节点是否被删除的标志
    public boolean getDeleted() {
        return this.mDeleted;
    }

}
