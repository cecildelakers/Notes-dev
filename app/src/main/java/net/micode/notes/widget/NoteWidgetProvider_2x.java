/**
 * 该类主要负责管理2x大小的便签小部件在界面更新时的相关逻辑，包括布局资源的获取、背景资源的设置以及小部件类型的标识。
 * 更新逻辑：
 * 覆盖了 onUpdate 方法，调用了父类的 update 方法，用于处理小部件在界面更新时的逻辑。
 * 布局资源：
 * 实现了 getLayoutId 方法，用于获取小部件的布局资源ID，这里返回的是 R.layout.widget_2x，表示2x大小的便签小部件所使用的布局。
 * 背景资源：
 * 覆盖了 getBgResourceId 方法，用于根据给定的背景资源索引ID获取对应的背景资源ID。
 * 小部件类型：
 * 实现了 getWidgetType 方法，用于获取当前小部件的类型，这里返回的是 Notes.TYPE_WIDGET_2X，表示2x大小的便签小部件的类型。
 */

package net.micode.notes.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

/**
 * 2x版本的NoteWidgetProvider，负责处理2x大小的小部件更新和其他操作。
 */
public class NoteWidgetProvider_2x extends NoteWidgetProvider {
    /**
     * 当小部件需要更新时调用此方法。
     *
     * @param context          上下文环境，用于访问应用全局功能。
     * @param appWidgetManager 管理当前应用中所有小部件的AppWidgetManager实例。
     * @param appWidgetIds     当前需要更新的小部件ID数组。
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.update(context, appWidgetManager, appWidgetIds);
    }

    /**
     * 获取小部件布局的ID。
     *
     * @return 布局资源ID。
     */
    @Override
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    /**
     * 根据背景ID获取对应的背景资源ID。
     *
     * @param bgId 背景资源的索引ID。
     * @return 背景资源的ID。
     */
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    /**
     * 获取小部件的类型。
     *
     * @return 小部件类型的常量。
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}

