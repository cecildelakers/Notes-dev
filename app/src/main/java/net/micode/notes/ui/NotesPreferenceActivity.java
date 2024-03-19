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

package net.micode.notes.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;


public class NotesPreferenceActivity extends PreferenceActivity {
    // 常量定义部分：主要用于设置和同步相关的偏好设置键
    public static final String PREFERENCE_NAME = "notes_preferences"; // 偏好设置的名称
    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name"; // 同步账户名称的键
    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time"; // 上次同步时间的键
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear"; // 设置背景颜色的键
    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key"; // 同步账户的键
    private static final String AUTHORITIES_FILTER_KEY = "authorities"; // 权限过滤键

    // 类成员变量定义部分：主要用于账户同步和UI更新
    private PreferenceCategory mAccountCategory; // 账户分类偏好项
    private GTaskReceiver mReceiver; // 接收同步任务的广播接收器
    private Account[] mOriAccounts; // 原始账户数组
    private boolean mHasAddedAccount; // 标记是否已添加新账户

    /**
     * 当设置Activity创建时调用。
     * 主要进行界面初始化和设置账户同步。
     *
     * @param icicle 保存Activity状态的Bundle，用于恢复状态。
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 设置返回按钮
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 从XML加载偏好设置
        addPreferencesFromResource(R.xml.preferences);
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);
        mReceiver = new GTaskReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);
        registerReceiver(mReceiver, filter); // 注册广播接收器以监听同步服务

        mOriAccounts = null;
        // 添加设置头部视图
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        getListView().addHeaderView(header, null, true);
    }

    /**
     * 当设置Activity恢复到前台时调用。
     * 主要用于检查并自动设置新添加的账户进行同步。
     */
    @Override
    protected void onResume() {
        super.onResume();

        // 自动设置新添加的账户进行同步
        if (mHasAddedAccount) {
            Account[] accounts = getGoogleAccounts();
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                for (Account accountNew : accounts) {
                    boolean found = false;
                    for (Account accountOld : mOriAccounts) {
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        setSyncAccount(accountNew.name); // 设置新账户进行同步
                        break;
                    }
                }
            }
        }

        // 刷新UI
        refreshUI();
    }


    /**
     * 当Activity即将被销毁时调用，用于注销广播接收器。
     */
    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver); // 注销广播接收器，避免内存泄漏
        }
        super.onDestroy();
    }

    /**
     * 加载账户偏好设置，展示当前同步账户信息及操作。
     */
    private void loadAccountPreference() {
        mAccountCategory.removeAll(); // 清空账户分类下的所有条目

        // 创建并配置账户偏好项
        Preference accountPref = new Preference(this);
        final String defaultAccount = getSyncAccountName(this); // 获取默认同步账户名称
        accountPref.setTitle(getString(R.string.preferences_account_title)); // 设置标题
        accountPref.setSummary(getString(R.string.preferences_account_summary)); // 设置摘要
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                // 处理账户点击事件
                if (!GTaskSyncService.isSyncing()) {
                    if (TextUtils.isEmpty(defaultAccount)) {
                        // 如果尚未设置账户，则展示选择账户对话框
                        showSelectAccountAlertDialog();
                    } else {
                        // 如果已经设置账户，则展示更改账户确认对话框
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    // 如果正在同步中，则展示无法更改账户的提示
                    Toast.makeText(NotesPreferenceActivity.this,
                                    R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
        });

        mAccountCategory.addPreference(accountPref); // 将账户偏好项添加到账户分类下
    }

    /**
     * 加载同步按钮，并根据同步状态设置其文本和点击事件。
     */
    private void loadSyncButton() {
        Button syncButton = (Button) findViewById(R.id.preference_sync_button); // 获取同步按钮
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview); // 获取上次同步时间视图

        // 根据同步状态设置按钮文本和点击事件
        if (GTaskSyncService.isSyncing()) {
            syncButton.setText(getString(R.string.preferences_button_sync_cancel)); // 设置为取消同步文本
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this); // 设置点击事件为取消同步
                }
            });
        } else {
            syncButton.setText(getString(R.string.preferences_button_sync_immediately)); // 设置为立即同步文本
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    GTaskSyncService.startSync(NotesPreferenceActivity.this); // 设置点击事件为开始同步
                }
            });
        }
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this))); // 只有在设置了同步账户时才使能同步按钮

        // 根据同步状态设置上次同步时间的显示
        if (GTaskSyncService.isSyncing()) {
            lastSyncTimeView.setText(GTaskSyncService.getProgressString()); // 如果正在同步，显示进度信息
            lastSyncTimeView.setVisibility(View.VISIBLE); // 显示上次同步时间视图
        } else {
            long lastSyncTime = getLastSyncTime(this); // 获取上次同步时间
            if (lastSyncTime != 0) {
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime))); // 格式化并显示上次同步时间
                lastSyncTimeView.setVisibility(View.VISIBLE); // 显示上次同步时间视图
            } else {
                lastSyncTimeView.setVisibility(View.GONE); // 如果未同步过，则隐藏上次同步时间视图
            }
        }
    }

    /**
     * 刷新用户界面，加载账户偏好设置和同步按钮。
     */
    private void refreshUI() {
        loadAccountPreference(); // 加载账户偏好设置
        loadSyncButton(); // 加载同步按钮
    }

    /**
     * 显示选择账户的对话框。
     * 该对话框列出了已连接的Google账户，并允许用户选择一个账户用于同步。
     * 如果没有账户，对话框将提供添加账户的选项。
     */
    private void showSelectAccountAlertDialog() {
        // 创建对话框构建器并设置自定义标题
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips));

        dialogBuilder.setCustomTitle(titleView);
        dialogBuilder.setPositiveButton(null, null); // 移除默认的确定按钮

        // 获取当前设备上的Google账户
        Account[] accounts = getGoogleAccounts();
        String defAccount = getSyncAccountName(this); // 获取当前同步的账户名称

        mOriAccounts = accounts; // 保存原始账户列表
        mHasAddedAccount = false; // 标记是否已添加新账户

        if (accounts.length > 0) {
            // 创建账户选项并设置选中项
            CharSequence[] items = new CharSequence[accounts.length];
            final CharSequence[] itemMapping = items;
            int checkedItem = -1; // 记录默认选中的账户
            int index = 0;
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, defAccount)) {
                    checkedItem = index;
                }
                items[index++] = account.name;
            }
            // 设置单选列表，并为选中的账户执行同步操作
            dialogBuilder.setSingleChoiceItems(items, checkedItem,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setSyncAccount(itemMapping[which].toString());
                            dialog.dismiss();
                            refreshUI();
                        }
                    });
        }

        // 添加“添加账户”选项
        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null);
        dialogBuilder.setView(addAccountView);

        final AlertDialog dialog = dialogBuilder.show();
        // 点击“添加账户”执行添加账户操作
        addAccountView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHasAddedAccount = true;
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[]{
                        "gmail-ls"
                });
                startActivityForResult(intent, -1);
                dialog.dismiss();
            }
        });
    }

    /**
     * 显示更改账户确认对话框。
     * 提供用户更改当前同步账户或取消更改的选择。
     */
    private void showChangeAccountConfirmAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 设置自定义标题，包含当前同步账户名称
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this)));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg));
        dialogBuilder.setCustomTitle(titleView);

        // 创建菜单项并设置点击事件
        CharSequence[] menuItemArray = new CharSequence[]{
                getString(R.string.preferences_menu_change_account),
                getString(R.string.preferences_menu_remove_account),
                getString(R.string.preferences_menu_cancel)
        };
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // 选择更改账户，显示账户选择对话框
                    showSelectAccountAlertDialog();
                } else if (which == 1) {
                    // 选择移除账户，执行移除操作并刷新UI
                    removeSyncAccount();
                    refreshUI();
                }
            }
        });
        dialogBuilder.show();
    }

    /**
     * 获取设备上的Google账户列表。
     *
     * @return Account[] 返回设备上所有类型为“com.google”的账户数组。
     */
    private Account[] getGoogleAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getAccountsByType("com.google");
    }


    /**
     * 设置同步账户信息。
     * 如果当前账户与传入账户不一致，则更新SharedPreferences中的账户信息，并清理本地相关的gtask信息。
     *
     * @param account 需要设置的账户名
     */
    private void setSyncAccount(String account) {
        // 检查当前账户是否与传入账户名一致，不一致则更新账户信息
        if (!getSyncAccountName(this).equals(account)) {
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            // 如果账户名非空，则保存账户名，否则清除账户名
            if (account != null) {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account);
            } else {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
            }
            editor.commit();

            // 清理上次同步时间
            setLastSyncTime(this, 0);

            // 清理本地相关的gtask信息
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.GTASK_ID, "");
                    values.put(NoteColumns.SYNC_ID, 0);
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start();

            // 显示设置成功的提示信息
            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 移除同步账户信息。
     * 清除SharedPreferences中的账户信息和上次同步时间，并清理本地相关的gtask信息。
     */
    private void removeSyncAccount() {
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        // 如果存在账户信息，则移除
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME);
        }
        // 如果存在上次同步时间信息，则移除
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            editor.remove(PREFERENCE_LAST_SYNC_TIME);
        }
        editor.commit();

        // 清理本地相关的gtask信息
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.GTASK_ID, "");
                values.put(NoteColumns.SYNC_ID, 0);
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start();
    }

    /**
     * 获取当前同步账户名。
     * 从SharedPreferences中获取存储的账户名，默认为空字符串。
     *
     * @param context 上下文
     * @return 同步账户名
     */
    public static String getSyncAccountName(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
    }

    /**
     * 设置上次同步的时间。
     * 将指定的时间保存到SharedPreferences中。
     *
     * @param context 上下文
     * @param time    上次同步的时间戳
     */
    public static void setLastSyncTime(Context context, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time);
        editor.commit();
    }

    /**
     * 获取上次同步的时间。
     * 从SharedPreferences中获取上次同步的时间戳，默认为0。
     *
     * @param context 上下文
     * @return 上次同步的时间戳
     */
    public static long getLastSyncTime(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0);
    }

    /**
     * 广播接收器类，用于接收gtask同步相关的广播消息，并据此刷新UI。
     */
    private class GTaskReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUI();
            // 如果广播消息表明正在同步，则更新UI显示的同步状态信息
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG));
            }

        }
    }

    /**
     * 处理选项菜单项的选择事件。
     *
     * @param item 选中的菜单项
     * @return 如果事件已处理，则返回true；否则返回false。
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 当选择返回按钮时，启动NotesListActivity并清除当前活动栈顶以上的所有活动
                Intent intent = new Intent(this, NotesListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
