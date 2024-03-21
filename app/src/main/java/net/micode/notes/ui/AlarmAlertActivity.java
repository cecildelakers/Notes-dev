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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;


/*
 * AlarmAlertActivity 类用于处理提醒通知的界面和声音播放。
 * 当一个笔记的提醒时间到达时，这个活动会被启动，显示提醒信息并播放提醒声音。
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    private long mNoteId;      //文本在数据库存储中的ID号
    private String mSnippet;   //闹钟提示时出现的文本片段
    private static final int SNIPPET_PREW_MAX_LEN = 60;
    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Bundle类型的数据与Map类型的数据相似，都是以key-value的形式存储数据的
        //onsaveInstanceState方法是用来保存Activity的状态的
        //能从onCreate的参数savedInsanceState中获得状态数据
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //界面显示——无标题

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    //保持窗体点亮
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    //将窗体点亮
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    //允许窗体点亮时锁屏
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }//在手机锁屏后如果到了闹钟提示时间，点亮屏幕

        Intent intent = getIntent();

        try {
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            //根据ID从数据库中获取标签的内容；
            //getContentResolver（）是实现数据共享，实例存储。
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                    SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
            //判断标签片段是否达到符合长度
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
        /*
        try
        {
        	// 代码区
        }
        catch(Exception e)
        {
        	// 异常处理
        }
                     代码区如果有错误，就会返回所写异常的处理。*/
        mPlayer = new MediaPlayer();
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog();
            //弹出对话框
            playAlarmSound();
            //闹钟提示音激发
        } else {
            finish();
            //完成闹钟动作
        }
    }

    private boolean isScreenOn() {
        //判断屏幕是否锁屏，调用系统函数判断，最后返回值是布尔类型
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    private void playAlarmSound() {
        //闹钟提示音激发
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        //调用系统的铃声管理URI，得到闹钟提示音
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        try {
            mPlayer.setDataSource(this, url);
            //方法：setDataSource(Context context, Uri uri)
            //解释：无返回值，设置多媒体数据来源【根据 Uri】
            mPlayer.prepare();
            //准备同步
            mPlayer.setLooping(true);
            //设置是否循环播放
            mPlayer.start();
            //开始播放
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //e.printStackTrace()函数功能是抛出异常， 还将显示出更深的调用信息
            //System.out.println(e)，这个方法打印出异常，并且输出在哪里出现的异常
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        //AlertDialog的构造方法全部是Protected的
        //所以不能直接通过new一个AlertDialog来创建出一个AlertDialog。
        //要创建一个AlertDialog，就要用到AlertDialog.Builder中的create()方法
        //如这里的dialog就是新建了一个AlertDialog
        dialog.setTitle(R.string.app_name);
        //为对话框设置标题
        dialog.setMessage(mSnippet);
        //为对话框设置内容
        dialog.setPositiveButton(R.string.notealert_ok, this);
        //给对话框添加"Yes"按钮
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }//对话框添加"No"按钮
        dialog.show().setOnDismissListener(this);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            //用which来选择click后下一步的操作
            case DialogInterface.BUTTON_NEGATIVE:
                //这是取消操作
                Intent intent = new Intent(this, NoteEditActivity.class);
                //实现两个类间的数据传输
                intent.setAction(Intent.ACTION_VIEW);
                //设置动作属性
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                //实现key-value对
                //EXTRA_UID为key；mNoteId为键
                startActivity(intent);
                //开始动作
                break;
            default:
                //这是确定操作
                break;
        }
    }

    public void onDismiss(DialogInterface dialog) {
        //忽略
        stopAlarmSound();
        //停止闹钟声音
        finish();
        //完成该动作
    }

    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            //停止播放
            mPlayer.release();
            //释放MediaPlayer对象
            mPlayer = null;
        }
    }
}
