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

package net.micode.notes.data;

import android.net.Uri;

// Notes类定义了与笔记和文件夹相关的常量和数据列接口
public class Notes {
    public static final String AUTHORITY = "micode_notes"; // 用于标识内容提供者的授权名称
    public static final String TAG = "Notes"; // 日志标签
    public static final int TYPE_NOTE = 0; // 笔记类型
    public static final int TYPE_FOLDER = 1; // 文件夹类型
    public static final int TYPE_SYSTEM = 2; // 系统类型

    /**
     * 下面的ID是系统文件夹的标识符
     * {@link Notes#ID_ROOT_FOLDER} 是默认文件夹
     * {@link Notes#ID_TEMPARAY_FOLDER} 是属于没有文件夹的笔记
     * {@link Notes#ID_CALL_RECORD_FOLDER} 是用于存储通话记录的
     */
    public static final int ID_ROOT_FOLDER = 0; // 根文件夹ID
    public static final int ID_TEMPARAY_FOLDER = -1; // 临时文件夹ID，用于存放不属于任何文件夹的笔记
    public static final int ID_CALL_RECORD_FOLDER = -2; // 通话记录文件夹ID
    public static final int ID_TRASH_FOLER = -3; // 垃圾箱文件夹ID

    public static final String INTENT_EXTRA_ALERT_DATE = "net.micode.notes.alert_date"; // 用于Intent的提醒日期额外数据
    public static final String INTENT_EXTRA_BACKGROUND_ID = "net.micode.notes.background_color_id"; // 笔记背景色ID
    public static final String INTENT_EXTRA_WIDGET_ID = "net.micode.notes.widget_id"; // 小部件ID
    public static final String INTENT_EXTRA_WIDGET_TYPE = "net.micode.notes.widget_type"; // 小部件类型
    public static final String INTENT_EXTRA_FOLDER_ID = "net.micode.notes.folder_id"; // 文件夹ID
    public static final String INTENT_EXTRA_CALL_DATE = "net.micode.notes.call_date"; // 通话日期

    public static final int TYPE_WIDGET_INVALIDE = -1; // 无效的小部件类型
    public static final int TYPE_WIDGET_2X = 0; // 2x小部件类型
    public static final int TYPE_WIDGET_4X = 1; // 4x小部件类型

    public static class DataConstants {
        public static final String NOTE = TextNote.CONTENT_ITEM_TYPE; // 笔记的内容项类型
        public static final String CALL_NOTE = CallNote.CONTENT_ITEM_TYPE; // 通话记录的内容项类型
    }

    /**
     * 查询所有笔记和文件夹的Uri
     */
    public static final Uri CONTENT_NOTE_URI = Uri.parse("content://" + AUTHORITY + "/note");

    /**
     * 查询数据的Uri
     */
    public static final Uri CONTENT_DATA_URI = Uri.parse("content://" + AUTHORITY + "/data");

    // 笔记和文件夹的公共列接口
    public interface NoteColumns {
        /**
         * 行的唯一ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String ID = "_id";

        /**
         * 笔记或文件夹的父ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String PARENT_ID = "parent_id";

        /**
         * 创建日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * 最后修改日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * 提醒日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String ALERTED_DATE = "alert_date";

        /**
         * 笔记或文件夹的摘要信息
         * <P>类型: TEXT</P>
         */
        public static final String SNIPPET = "snippet";

        /**
         * 笔记的小部件ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String WIDGET_ID = "widget_id";

        /**
         * 笔记的小部件类型
         * <P>类型: INTEGER (long)</P>
         */
        public static final String WIDGET_TYPE = "widget_type";

        /**
         * 笔记的背景色ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String BG_COLOR_ID = "bg_color_id";

        /**
         * 笔记是否有附件
         * <P>类型: INTEGER</P>
         */
        public static final String HAS_ATTACHMENT = "has_attachment";

        /**
         * 笔记数量
         * <P>类型: INTEGER (long)</P>
         */
        public static final String NOTES_COUNT = "notes_count";

        /**
         * 文件夹类型：0-笔记，1-文件夹
         * <P>类型: INTEGER</P>
         */
        public static final String TYPE = "type";

        /**
         * 最后同步ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String SYNC_ID = "sync_id";

        /**
         * 标记本地是否已修改
         * <P>类型: INTEGER</P>
         */
        public static final String LOCAL_MODIFIED = "local_modified";

        /**
         * 移入临时文件夹前的原始父ID
         * <P>类型: INTEGER</P>
         */
        public static final String ORIGIN_PARENT_ID = "origin_parent_id";

        /**
         * Google任务ID
         * <P>类型: TEXT</P>
         */
        public static final String GTASK_ID = "gtask_id";

        /**
         * 版本号
         * <P>类型: INTEGER (long)</P>
         */
        public static final String VERSION = "version";
    }

    // 数据列接口
    public interface DataColumns {
        /**
         * 行的唯一ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String ID = "_id";

        /**
         * 该项的MIME类型。
         * <P>类型: TEXT</P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * 属于的笔记的引用ID
         * <P>类型: INTEGER (long)</P>
         */
        public static final String NOTE_ID = "note_id";

        /**
         * 创建日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String CREATED_DATE = "created_date";

        /**
         * 最后修改日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String MODIFIED_DATE = "modified_date";

        /**
         * 数据内容
         * <P>类型: TEXT</P>
         */
        public static final String CONTENT = "content";

        /**
         * 通用数据列，具体含义由{@link #MIME_TYPE}决定，用于存储整型数据
         * <P>类型: INTEGER</P>
         */
        public static final String DATA1 = "data1";

        /**
         * 通用数据列，具体含义由{@link #MIME_TYPE}决定，用于存储整型数据
         * <P>类型: INTEGER</P>
         */
        public static final String DATA2 = "data2";

        /**
         * 通用数据列，具体含义由{@link #MIME_TYPE}决定，用于存储文本数据
         * <P>类型: TEXT</P>
         */
        public static final String DATA3 = "data3";

        /**
         * 通用数据列，具体含义由{@link #MIME_TYPE}决定，用于存储文本数据
         * <P>类型: TEXT</P>
         */
        public static final String DATA4 = "data4";

        /**
         * 通用数据列，具体含义由{@link #MIME_TYPE}决定，用于存储文本数据
         * <P>类型: TEXT</P>
         */
        public static final String DATA5 = "data5";
    }

    // 文本笔记类，实现了DataColumns接口
    public static final class TextNote implements DataColumns {
        /**
         * 模式，指示文本是否在检查列表模式中
         * <P>类型: INTEGER 1:检查列表模式 0: 正常模式</P>
         */
        public static final String MODE = DATA1;

        public static final int MODE_CHECK_LIST = 1;

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/text_note"; // MIME类型定义
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/text_note"; // 单项MIME类型定义
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/text_note"); // 内容URI定义
    }

    // 通话记录笔记类，实现了DataColumns接口
    public static final class CallNote implements DataColumns {
        /**
         * 通话日期
         * <P>类型: INTEGER (long)</P>
         */
        public static final String CALL_DATE = DATA1;

        /**
         * 电话号码
         * <P>类型: TEXT</P>
         */
        public static final String PHONE_NUMBER = DATA3;

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/call_note"; // MIME类型定义
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/call_note"; // 单项MIME类型定义
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/call_note"); // 内容URI定义
    }
}
