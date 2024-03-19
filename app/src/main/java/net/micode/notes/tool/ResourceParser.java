/*
 * ResourceParser 类用于管理与应用资源相关的各种静态方法和常量。
 */

package net.micode.notes.tool;

import android.content.Context;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;

public class ResourceParser {

    // 定义笔记背景颜色的常量
    public static final int YELLOW = 0;
    public static final int BLUE = 1;
    public static final int WHITE = 2;
    public static final int GREEN = 3;
    public static final int RED = 4;

    // 默认背景颜色
    public static final int BG_DEFAULT_COLOR = YELLOW;

    // 定义文本大小的常量
    public static final int TEXT_SMALL = 0;
    public static final int TEXT_MEDIUM = 1;
    public static final int TEXT_LARGE = 2;
    public static final int TEXT_SUPER = 3;

    // 默认字体大小
    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    /**
     * 笔记背景资源类，提供获取不同背景资源的方法。
     */
    public static class NoteBgResources {
        // 编辑状态下的背景资源数组
        private final static int[] BG_EDIT_RESOURCES = new int[]{
                R.drawable.edit_yellow,
                R.drawable.edit_blue,
                R.drawable.edit_white,
                R.drawable.edit_green,
                R.drawable.edit_red
        };

        // 编辑状态下的标题背景资源数组
        private final static int[] BG_EDIT_TITLE_RESOURCES = new int[]{
                R.drawable.edit_title_yellow,
                R.drawable.edit_title_blue,
                R.drawable.edit_title_white,
                R.drawable.edit_title_green,
                R.drawable.edit_title_red
        };

        // 根据id获取编辑状态下的背景资源
        public static int getNoteBgResource(int id) {
            return BG_EDIT_RESOURCES[id];
        }

        // 根据id获取编辑状态下的标题背景资源
        public static int getNoteTitleBgResource(int id) {
            return BG_EDIT_TITLE_RESOURCES[id];
        }
    }

    /**
     * 获取默认笔记背景id。
     *
     * @param context 上下文对象，用于访问SharedPreferences。
     * @return 如果用户设置了背景颜色，则返回一个随机背景颜色id；否则返回默认背景颜色id。
     */
    public static int getDefaultBgId(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            return (int) (Math.random() * NoteBgResources.BG_EDIT_RESOURCES.length);
        } else {
            return BG_DEFAULT_COLOR;
        }
    }

    /**
     * 笔记列表项背景资源类，提供获取不同背景资源的方法。
     */
    public static class NoteItemBgResources {
        // 第一个列表项的背景资源数组
        private final static int[] BG_FIRST_RESOURCES = new int[]{
                R.drawable.list_yellow_up,
                R.drawable.list_blue_up,
                R.drawable.list_white_up,
                R.drawable.list_green_up,
                R.drawable.list_red_up
        };

        // 普通列表项的背景资源数组
        private final static int[] BG_NORMAL_RESOURCES = new int[]{
                R.drawable.list_yellow_middle,
                R.drawable.list_blue_middle,
                R.drawable.list_white_middle,
                R.drawable.list_green_middle,
                R.drawable.list_red_middle
        };

        // 最后一个列表项的背景资源数组
        private final static int[] BG_LAST_RESOURCES = new int[]{
                R.drawable.list_yellow_down,
                R.drawable.list_blue_down,
                R.drawable.list_white_down,
                R.drawable.list_green_down,
                R.drawable.list_red_down,
        };

        // 单个列表项的背景资源数组
        private final static int[] BG_SINGLE_RESOURCES = new int[]{
                R.drawable.list_yellow_single,
                R.drawable.list_blue_single,
                R.drawable.list_white_single,
                R.drawable.list_green_single,
                R.drawable.list_red_single
        };

        // 获取第一个列表项的背景资源
        public static int getNoteBgFirstRes(int id) {
            return BG_FIRST_RESOURCES[id];
        }

        // 获取最后一个列表项的背景资源
        public static int getNoteBgLastRes(int id) {
            return BG_LAST_RESOURCES[id];
        }

        // 获取单个列表项的背景资源
        public static int getNoteBgSingleRes(int id) {
            return BG_SINGLE_RESOURCES[id];
        }

        // 获取普通列表项的背景资源
        public static int getNoteBgNormalRes(int id) {
            return BG_NORMAL_RESOURCES[id];
        }

        // 获取文件夹背景资源
        public static int getFolderBgRes() {
            return R.drawable.list_folder;
        }
    }

    /**
     * 小部件背景资源类，提供获取小部件背景资源的方法。
     */
    public static class WidgetBgResources {
        // 2x 小部件背景资源数组
        private final static int[] BG_2X_RESOURCES = new int[]{
                R.drawable.widget_2x_yellow,
                R.drawable.widget_2x_blue,
                R.drawable.widget_2x_white,
                R.drawable.widget_2x_green,
                R.drawable.widget_2x_red,
        };

        // 根据id获取2x小部件的背景资源
        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        // 4x 小部件背景资源数组
        private final static int[] BG_4X_RESOURCES = new int[]{
                R.drawable.widget_4x_yellow,
                R.drawable.widget_4x_blue,
                R.drawable.widget_4x_white,
                R.drawable.widget_4x_green,
                R.drawable.widget_4x_red
        };

        // 根据id获取4x小部件的背景资源
        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    /**
     * 文本外观资源类，提供获取不同文本外观资源的方法。
     */
    public static class TextAppearanceResources {
        // 文本外观资源数组
        private final static int[] TEXTAPPEARANCE_RESOURCES = new int[]{
                R.style.TextAppearanceNormal,
                R.style.TextAppearanceMedium,
                R.style.TextAppearanceLarge,
                R.style.TextAppearanceSuper
        };

        // 根据id获取文本外观资源
        public static int getTexAppearanceResource(int id) {
            // 如果id超出资源数组范围，返回默认字体大小
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        // 获取文本外观资源的数量
        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}

