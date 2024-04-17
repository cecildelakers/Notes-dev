/**
 * 这段代码定义了一个名为 NoteWidgetProvider_4x 的类，它是一个4x大小的便签小部件提供者，继承自 NoteWidgetProvider 类。以下是其主要功能总结：
 * 更新逻辑：
 * 覆盖了 onUpdate 方法，调用了父类的 update 方法，用于处理小部件在界面更新时的逻辑。
 * 布局资源：
 * 实现了 getLayoutId 方法，用于获取小部件的布局资源ID，这里返回的是 R.layout.widget_4x，表示4x大小的便签小部件所使用的布局。
 * 背景资源：
 * 覆盖了 getBgResourceId 方法，用于获取小部件背景资源的ID。调用了 ResourceParser.WidgetBgResources.getWidget4xBgResource(bgId) 方法，根据给定的背景资源索引ID获取对应的背景资源ID。
 * 小部件类型：
 * 实现了 getWidgetType 方法，用于获取当前小部件的类型。这里返回的是 Notes.TYPE_WIDGET_4X，表示4x大小的便签小部件的类型。
 */

package net.micode.notes.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

/**
 * 4x大小的便签小部件提供者类，继承自NoteWidgetProvider。
 * 负责处理小部件在界面更新时的逻辑。
 */
public class NoteWidgetProvider_4x extends NoteWidgetProvider {
    /**
     * 当小部件需要更新时调用此方法。
     *
     * @param context          上下文环境，通常为应用程序环境。
     * @param appWidgetManager 小部件管理器，用于管理已安装的小部件。
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
    protected int getLayoutId() {
        return R.layout.widget_4x;
    }

    /**
     * 获取小部件背景资源的ID。
     *
     * @param bgId 背景资源的索引ID。
     * @return 背景资源的ID。
     */
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget4xBgResource(bgId);
    }

    /**
     * 获取当前小部件的类型。
     *
     * @return 小部件类型的整型标识。
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_4X;
    }
}

