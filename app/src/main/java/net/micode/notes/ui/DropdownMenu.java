/*
 * DropdownMenu类用于创建和管理一个下拉菜单。
 * 该类封装了一个Button和一个PopupMenu，通过点击Button来显示下拉菜单。
 */
package net.micode.notes.ui;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

public class DropdownMenu {
    private Button mButton; // 弹出下拉菜单的按钮
    private PopupMenu mPopupMenu; // 弹出的下拉菜单
    private Menu mMenu; // 下拉菜单的项目集合

    /**
     * DropdownMenu的构造函数。
     *
     * @param context 上下文对象，通常是指Activity。
     * @param button  用于触发下拉菜单显示的按钮。
     * @param menuId  菜单资源ID，用于加载下拉菜单的项目。
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button;
        mButton.setBackgroundResource(R.drawable.dropdown_icon); // 设置按钮背景为下拉图标
        mPopupMenu = new PopupMenu(context, mButton); // 创建PopupMenu实例
        mMenu = mPopupMenu.getMenu(); // 获取菜单项的集合
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu); // 加载菜单项
        // 设置按钮点击事件，点击后显示下拉菜单
        mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mPopupMenu.show();
            }
        });
    }

    /**
     * 设置下拉菜单项的点击事件监听器。
     *
     * @param listener PopupMenu的OnMenuItemClickListener，用于监听菜单项的点击事件。
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * 根据ID查找菜单项。
     *
     * @param id 菜单项的ID。
     * @return 返回找到的MenuItem对象，如果未找到则返回null。
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    /**
     * 设置按钮的标题。
     *
     * @param title 按钮要显示的标题。
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
