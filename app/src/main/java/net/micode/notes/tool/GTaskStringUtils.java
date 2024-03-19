/*
 * GTaskStringUtils 类定义
 * 该类提供了一系列与GTask相关的字符串常量，用于在操作GTask数据时标识各种JSON属性。
 */

package net.micode.notes.tool;

public class GTaskStringUtils {

    // GTask JSON对象中各种属性的键名
    public final static String GTASK_JSON_ACTION_ID = "action_id"; // 动作ID
    public final static String GTASK_JSON_ACTION_LIST = "action_list"; // 动作列表
    public final static String GTASK_JSON_ACTION_TYPE = "action_type"; // 动作类型
    public final static String GTASK_JSON_ACTION_TYPE_CREATE = "create"; // 创建动作类型
    public final static String GTASK_JSON_ACTION_TYPE_GETALL = "get_all"; // 获取所有动作类型
    public final static String GTASK_JSON_ACTION_TYPE_MOVE = "move"; // 移动动作类型
    public final static String GTASK_JSON_ACTION_TYPE_UPDATE = "update"; // 更新动作类型
    public final static String GTASK_JSON_CREATOR_ID = "creator_id"; // 创建者ID
    public final static String GTASK_JSON_CHILD_ENTITY = "child_entity"; // 子实体
    public final static String GTASK_JSON_CLIENT_VERSION = "client_version"; // 客户端版本
    public final static String GTASK_JSON_COMPLETED = "completed"; // 完成状态
    public final static String GTASK_JSON_CURRENT_LIST_ID = "current_list_id"; // 当前列表ID
    public final static String GTASK_JSON_DEFAULT_LIST_ID = "default_list_id"; // 默认列表ID
    public final static String GTASK_JSON_DELETED = "deleted"; // 删除状态
    public final static String GTASK_JSON_DEST_LIST = "dest_list"; // 目标列表
    public final static String GTASK_JSON_DEST_PARENT = "dest_parent"; // 目标父实体
    public final static String GTASK_JSON_DEST_PARENT_TYPE = "dest_parent_type"; // 目标父实体类型
    public final static String GTASK_JSON_ENTITY_DELTA = "entity_delta"; // 实体增量
    public final static String GTASK_JSON_ENTITY_TYPE = "entity_type"; // 实体类型
    public final static String GTASK_JSON_GET_DELETED = "get_deleted"; // 获取已删除项
    public final static String GTASK_JSON_ID = "id"; // ID
    public final static String GTASK_JSON_INDEX = "index"; // 索引
    public final static String GTASK_JSON_LAST_MODIFIED = "last_modified"; // 最后修改时间
    public final static String GTASK_JSON_LATEST_SYNC_POINT = "latest_sync_point"; // 最新同步点
    public final static String GTASK_JSON_LIST_ID = "list_id"; // 列表ID
    public final static String GTASK_JSON_LISTS = "lists"; // 列表集合
    public final static String GTASK_JSON_NAME = "name"; // 名称
    public final static String GTASK_JSON_NEW_ID = "new_id"; // 新ID
    public final static String GTASK_JSON_NOTES = "notes"; // 备注
    public final static String GTASK_JSON_PARENT_ID = "parent_id"; // 父ID
    public final static String GTASK_JSON_PRIOR_SIBLING_ID = "prior_sibling_id"; // 前一个兄弟ID
    public final static String GTASK_JSON_RESULTS = "results"; // 结果
    public final static String GTASK_JSON_SOURCE_LIST = "source_list"; // 源列表
    public final static String GTASK_JSON_TASKS = "tasks"; // 任务集合
    public final static String GTASK_JSON_TYPE = "type"; // 类型
    public final static String GTASK_JSON_TYPE_GROUP = "GROUP"; // 类型：组
    public final static String GTASK_JSON_TYPE_TASK = "TASK"; // 类型：任务
    public final static String GTASK_JSON_USER = "user"; // 用户
    // MIUI笔记相关的文件夹前缀和元数据键名
    public final static String MIUI_FOLDER_PREFFIX = "[MIUI_Notes]"; // MIUI笔记文件夹前缀
    public final static String FOLDER_DEFAULT = "Default"; // 默认文件夹名
    public final static String FOLDER_CALL_NOTE = "Call_Note"; // 通话笔记文件夹名
    public final static String FOLDER_META = "METADATA"; // 元数据文件夹名
    // 元数据头部键名
    public final static String META_HEAD_GTASK_ID = "meta_gid"; // GTask ID
    public final static String META_HEAD_NOTE = "meta_note"; // 笔记内容
    public final static String META_HEAD_DATA = "meta_data"; // 元数据
    // 元数据笔记名称，不可更新和删除
    public final static String META_NOTE_NAME = "[META INFO] DON'T UPDATE AND DELETE";
}
