/*
 * DateTimePickerDialog类提供了一个日期和时间选择器对话框。
 * 用户可以选择一个日期和时间，然后通过监听器回调返回选择的值。
 */
package net.micode.notes.ui;

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    // 当前选择的日期和时间
    private Calendar mDate = Calendar.getInstance();
    // 用于指示日期时间选择器是否使用24小时制
    private boolean mIs24HourView;
    // 日期时间设置监听器，用于处理日期时间选择后的回调
    private OnDateTimeSetListener mOnDateTimeSetListener;
    // 日期时间选择器视图
    private DateTimePicker mDateTimePicker;

    /**
     * 日期时间设置监听器接口。
     * 实现此接口的类需要提供OnDateTimeSet方法来处理日期时间被设置的事件。
     */
    public interface OnDateTimeSetListener {
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    /**
     * 构造函数初始化日期时间选择器对话框。
     *
     * @param context 上下文对象，通常是指Activity。
     * @param date    初始显示的日期时间值。
     */
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        mDateTimePicker = new DateTimePicker(context);
        setView(mDateTimePicker);
        // 设置日期时间改变的监听器
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                                          int dayOfMonth, int hourOfDay, int minute) {
                // 更新内部日期时间值
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                // 更新对话框的标题显示
                updateTitle(mDate.getTimeInMillis());
            }
        });
        mDate.setTimeInMillis(date);
        mDate.set(Calendar.SECOND, 0);
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        // 设置对话框的确认和取消按钮
        setButton(context.getString(R.string.datetime_dialog_ok), this);
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener) null);
        // 根据系统设置决定是否使用24小时制
        set24HourView(DateFormat.is24HourFormat(this.getContext()));
        // 更新标题以显示当前选择的日期和时间
        updateTitle(mDate.getTimeInMillis());
    }

    /**
     * 设置日期时间选择器是否使用24小时制。
     *
     * @param is24HourView 是否使用24小时制。
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    /**
     * 设置日期时间被设置时的监听器。
     *
     * @param callBack 日期时间设置监听器对象。
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    /**
     * 更新对话框标题以显示当前选择的日期和时间。
     *
     * @param date 当前选择的日期时间值。
     */
    private void updateTitle(long date) {
        // 根据是否使用24小时制来格式化日期时间显示
        int flag =
                DateUtils.FORMAT_SHOW_YEAR |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_TIME;
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_24HOUR;
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    /**
     * 点击按钮时的处理逻辑。
     * 如果设置了日期时间设置监听器，则调用其OnDateTimeSet方法，传入当前选择的日期时间值。
     *
     * @param arg0 对话框对象。
     * @param arg1 按钮标识。
     */
    public void onClick(DialogInterface arg0, int arg1) {
        if (mOnDateTimeSetListener != null) {
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }

}
