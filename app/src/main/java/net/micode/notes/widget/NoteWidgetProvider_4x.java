/*
 * 版权声明：MiCode开源社区（www.micode.net）
 *
 * 本代码遵循Apache 2.0开源协议
 * 详细授权信息请访问：http://www.apache.org/licenses/LICENSE-2.0
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

