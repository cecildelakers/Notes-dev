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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import net.micode.notes.R;


import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

public class DateTimePicker extends FrameLayout {

    // 默认启用状态
    private static final boolean DEFAULT_ENABLE_STATE = true;

    // 半天的小时数
    private static final int HOURS_IN_HALF_DAY = 12;
    // 一整天的小时数
    private static final int HOURS_IN_ALL_DAY = 24;
    // 一周的天数
    private static final int DAYS_IN_ALL_WEEK = 7;
    // 日期选择器的最小值
    private static final int DATE_SPINNER_MIN_VAL = 0;
    // 日期选择器的最大值
    private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
    // 24小时制小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW = 0;
    // 24小时制小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW = 23;
    // 12小时制小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW = 1;
    // 12小时制小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW = 12;
    // 分钟选择器的最小值
    private static final int MINUT_SPINNER_MIN_VAL = 0;
    // 分钟选择器的最大值
    private static final int MINUT_SPINNER_MAX_VAL = 59;
    // 上下午选择器的最小值
    private static final int AMPM_SPINNER_MIN_VAL = 0;
    // 上下午选择器的最大值
    private static final int AMPM_SPINNER_MAX_VAL = 1;

    // 日期选择器
    private final NumberPicker mDateSpinner;
    // 小时选择器
    private final NumberPicker mHourSpinner;
    // 分钟选择器
    private final NumberPicker mMinuteSpinner;
    // 上下午选择器
    private final NumberPicker mAmPmSpinner;
    // 当前日期
    private Calendar mDate;

    // 用于显示日期的字符串数组
    private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK];

    // 当前是否为上午
    private boolean mIsAm;

    // 当前是否为24小时制视图
    private boolean mIs24HourView;

    // 控件是否启用
    private boolean mIsEnabled = DEFAULT_ENABLE_STATE;

    // 是否正在初始化
    private boolean mInitialising;

    // 日期时间改变监听器
    private OnDateTimeChangedListener mOnDateTimeChangedListener;

    // 日期选择器的值改变监听器
    private NumberPicker.OnValueChangeListener mOnDateChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 根据新旧值的差异更新日期
            mDate.add(Calendar.DAY_OF_YEAR, newVal - oldVal);
            updateDateControl();
            onDateTimeChanged();
        }
    };

    // 小时选择器的值改变监听器
    private NumberPicker.OnValueChangeListener mOnHourChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 根据小时的变化更新日期和上下午状态
            boolean isDateChanged = false;
            Calendar cal = Calendar.getInstance();
            if (!mIs24HourView) {
                // 处理12小时制下的日期变化和上下午切换
                if (!mIsAm && oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) {
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (mIsAm && oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
                if (oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY ||
                        oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                }
            } else {
                // 处理24小时制下的日期变化
                if (oldVal == HOURS_IN_ALL_DAY - 1 && newVal == 0) {
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (oldVal == 0 && newVal == HOURS_IN_ALL_DAY - 1) {
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
            }
            // 更新小时并触发日期时间改变事件
            int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
            mDate.set(Calendar.HOUR_OF_DAY, newHour);
            onDateTimeChanged();
            // 如果日期有变化，则更新年月日
            if (isDateChanged) {
                setCurrentYear(cal.get(Calendar.YEAR));
                setCurrentMonth(cal.get(Calendar.MONTH));
                setCurrentDay(cal.get(Calendar.DAY_OF_MONTH));
            }
        }
    };


    // 分别为分钟和AM/PM选择器监听器设置匿名内部类，实现数值变化时的处理逻辑。
    private NumberPicker.OnValueChangeListener mOnMinuteChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 计算小时的偏移量，当从最大值变为最小值或从最小值变为最大值时调整
            int minValue = mMinuteSpinner.getMinValue();
            int maxValue = mMinuteSpinner.getMaxValue();
            int offset = 0;
            if (oldVal == maxValue && newVal == minValue) {
                offset += 1;
            } else if (oldVal == minValue && newVal == maxValue) {
                offset -= 1;
            }
            // 根据偏移量更新日期和小时选择器，并检查是否需要切换AM/PM
            if (offset != 0) {
                mDate.add(Calendar.HOUR_OF_DAY, offset);
                mHourSpinner.setValue(getCurrentHour());
                updateDateControl();
                int newHour = getCurrentHourOfDay();
                if (newHour >= HOURS_IN_HALF_DAY) {
                    mIsAm = false;
                    updateAmPmControl();
                } else {
                    mIsAm = true;
                    updateAmPmControl();
                }
            }
            // 更新分钟值并触发日期变化的回调
            mDate.set(Calendar.MINUTE, newVal);
            onDateTimeChanged();
        }
    };

    private NumberPicker.OnValueChangeListener mOnAmPmChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 切换AM/PM状态，并更新日期和AM/PM选择器
            mIsAm = !mIsAm;
            if (mIsAm) {
                mDate.add(Calendar.HOUR_OF_DAY, -HOURS_IN_HALF_DAY);
            } else {
                mDate.add(Calendar.HOUR_OF_DAY, HOURS_IN_HALF_DAY);
            }
            updateAmPmControl();
            onDateTimeChanged();
        }
    };

    // 定义日期时间变化的回调接口
    public interface OnDateTimeChangedListener {
        void onDateTimeChanged(DateTimePicker view, int year, int month,
                               int dayOfMonth, int hourOfDay, int minute);
    }

    // 构造函数：初始化日期时间选择器
    public DateTimePicker(Context context) {
        this(context, System.currentTimeMillis());
    }

    // 构造函数：指定初始日期时间
    public DateTimePicker(Context context, long date) {
        this(context, date, DateFormat.is24HourFormat(context));
    }

    // 构造函数：指定是否使用24小时制视图
    public DateTimePicker(Context context, long date, boolean is24HourView) {
        super(context);
        mDate = Calendar.getInstance();
        mInitialising = true;
        mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
        inflate(context, R.layout.datetime_picker, this);

        // 初始化日期、小时、分钟和AM/PM选择器，并设置相应的监听器
        mDateSpinner = (NumberPicker) findViewById(R.id.date);
        mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
        mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
        mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);

        mHourSpinner = (NumberPicker) findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);
        mMinuteSpinner = (NumberPicker) findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(MINUT_SPINNER_MIN_VAL);
        mMinuteSpinner.setMaxValue(MINUT_SPINNER_MAX_VAL);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setOnValueChangedListener(mOnMinuteChangedListener);

        String[] stringsForAmPm = new DateFormatSymbols().getAmPmStrings();
        mAmPmSpinner = (NumberPicker) findViewById(R.id.amPm);
        mAmPmSpinner.setMinValue(AMPM_SPINNER_MIN_VAL);
        mAmPmSpinner.setMaxValue(AMPM_SPINNER_MAX_VAL);
        mAmPmSpinner.setDisplayedValues(stringsForAmPm);
        mAmPmSpinner.setOnValueChangedListener(mOnAmPmChangedListener);

        // 更新控件至初始状态
        updateDateControl();
        updateHourControl();
        updateAmPmControl();

        set24HourView(is24HourView);

        // 设置为当前时间
        setCurrentDate(date);

        setEnabled(isEnabled());

        // 设置内容描述
        mInitialising = false;
    }


    /**
     * 设置控件的启用状态。
     * 如果当前状态与传入状态相同，则不进行任何操作。
     * 启用或禁用日期和时间选择器，并更新内部启用状态。
     *
     * @param enabled 控件是否启用
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        // 同时启用或禁用日期和时间选择器
        mDateSpinner.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        mAmPmSpinner.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    /**
     * 获取控件的启用状态。
     *
     * @return 控件是否启用
     */
    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * 获取当前日期的时间戳（毫秒）。
     *
     * @return 当前日期的毫秒时间戳
     */
    public long getCurrentDateInTimeMillis() {
        return mDate.getTimeInMillis();
    }

    /**
     * 设置当前日期。
     * 根据传入的毫秒时间戳更新日期选择器的值。
     *
     * @param date The current date in millis
     */
    public void setCurrentDate(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        // 通过日历实例的详细字段设置当前日期和时间
        setCurrentDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /**
     * 设置当前日期和时间。
     * 分别设置年、月、日、时和分。
     *
     * @param year       当前年份
     * @param month      当前月份
     * @param dayOfMonth 当前日
     * @param hourOfDay  当前小时
     * @param minute     当前分钟
     */
    public void setCurrentDate(int year, int month,
                               int dayOfMonth, int hourOfDay, int minute) {
        // 分别设置年、月、日、时和分
        setCurrentYear(year);
        setCurrentMonth(month);
        setCurrentDay(dayOfMonth);
        setCurrentHour(hourOfDay);
        setCurrentMinute(minute);
    }


    /**
     * 获取当前年份
     *
     * @return 当前的年份
     */
    public int getCurrentYear() {
        return mDate.get(Calendar.YEAR);
    }

    /**
     * 设置当前年份
     *
     * @param year 当前的年份
     */
    public void setCurrentYear(int year) {
        // 如果不是初始化状态并且设置的年份与当前年份相同，则直接返回
        if (!mInitialising && year == getCurrentYear()) {
            return;
        }
        mDate.set(Calendar.YEAR, year);
        updateDateControl(); // 更新日期控件
        onDateTimeChanged(); // 触发日期时间改变事件
    }

    /**
     * 获取当前月份
     *
     * @return 当前的月份（从0开始）
     */
    public int getCurrentMonth() {
        return mDate.get(Calendar.MONTH);
    }

    /**
     * 设置当前月份
     *
     * @param month 当前的月份（从0开始）
     */
    public void setCurrentMonth(int month) {
        // 如果不是初始化状态并且设置的月份与当前月份相同，则直接返回
        if (!mInitialising && month == getCurrentMonth()) {
            return;
        }
        mDate.set(Calendar.MONTH, month);
        updateDateControl(); // 更新日期控件
        onDateTimeChanged(); // 触发日期时间改变事件
    }

    /**
     * 获取当前日期（月中的天数）
     *
     * @return 当前的日期（月中的天数）
     */
    public int getCurrentDay() {
        return mDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 设置当前日期（月中的天数）
     *
     * @param dayOfMonth 当前的日期（月中的天数）
     */
    public void setCurrentDay(int dayOfMonth) {
        // 如果不是初始化状态并且设置的日期与当前日期相同，则直接返回
        if (!mInitialising && dayOfMonth == getCurrentDay()) {
            return;
        }
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateDateControl(); // 更新日期控件
        onDateTimeChanged(); // 触发日期时间改变事件
    }

    /**
     * 获取当前小时（24小时制），范围为(0~23)
     *
     * @return 当前的小时（24小时制）
     */
    public int getCurrentHourOfDay() {
        return mDate.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取当前小时，根据是否为24小时制返回不同的值。
     * 如果是24小时制，与{@link #getCurrentHourOfDay()}返回相同结果；
     * 否则，将小时转换为12小时制，并考虑上午/下午。
     *
     * @return 当前的小时（根据视图模式可能是12小时制）
     */
    private int getCurrentHour() {
        if (mIs24HourView) {
            return getCurrentHourOfDay();
        } else {
            int hour = getCurrentHourOfDay();
            // 转换为12小时制
            if (hour > HOURS_IN_HALF_DAY) {
                return hour - HOURS_IN_HALF_DAY;
            } else {
                return hour == 0 ? HOURS_IN_HALF_DAY : hour;
            }
        }
    }


    /**
     * 设置当前小时（24小时制），范围为(0~23)
     *
     * @param hourOfDay 当前小时数
     */
    public void setCurrentHour(int hourOfDay) {
        // 如果在初始化中或者小时未改变，则直接返回
        if (!mInitialising && hourOfDay == getCurrentHourOfDay()) {
            return;
        }
        mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        // 如果不是24小时视图，则调整小时数并更新AM/PM控制
        if (!mIs24HourView) {
            if (hourOfDay >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (hourOfDay > HOURS_IN_HALF_DAY) {
                    hourOfDay -= HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (hourOfDay == 0) {
                    hourOfDay = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(hourOfDay);
        onDateTimeChanged();
    }

    /**
     * 获取当前分钟数
     *
     * @return 当前分钟数
     */
    public int getCurrentMinute() {
        return mDate.get(Calendar.MINUTE);
    }

    /**
     * 设置当前分钟数
     *
     * @param minute 当前分钟数值
     */
    public void setCurrentMinute(int minute) {
        // 如果在初始化中或者分钟数未改变，则直接返回
        if (!mInitialising && minute == getCurrentMinute()) {
            return;
        }
        mMinuteSpinner.setValue(minute);
        mDate.set(Calendar.MINUTE, minute);
        onDateTimeChanged();
    }

    /**
     * 获取当前是否为24小时视图
     *
     * @return 如果是24小时视图返回true，否则返回false
     */
    public boolean is24HourView() {
        return mIs24HourView;
    }

    /**
     * 设置当前视图为24小时制还是AM/PM制
     *
     * @param is24HourView 如果为true表示24小时制，false表示AM/PM制
     */
    public void set24HourView(boolean is24HourView) {
        // 如果视图模式未改变，则直接返回
        if (mIs24HourView == is24HourView) {
            return;
        }
        mIs24HourView = is24HourView;
        mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
        int hour = getCurrentHourOfDay();
        updateHourControl();
        setCurrentHour(hour);
        updateAmPmControl();
    }

    /**
     * 更新日期控制组件，显示正确的日期选项
     */
    private void updateDateControl() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate.getTimeInMillis());
        cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
        mDateSpinner.setDisplayedValues(null);
        // 循环设置一周内每一天的显示文本
        for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
        }
        mDateSpinner.setDisplayedValues(mDateDisplayValues);
        mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2);
        mDateSpinner.invalidate();
    }

    /**
     * 根据当前是否为24小时视图来更新AM/PM控制组件的显示和值
     */
    private void updateAmPmControl() {
        if (mIs24HourView) {
            mAmPmSpinner.setVisibility(View.GONE);
        } else {
            int index = mIsAm ? Calendar.AM : Calendar.PM;
            mAmPmSpinner.setValue(index);
            mAmPmSpinner.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 根据当前是否为24小时视图来更新小时控制组件的最小值和最大值
     */
    private void updateHourControl() {
        if (mIs24HourView) {
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW);
        } else {
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW);
        }
    }

    /**
     * 设置点击“设置”按钮时的回调接口
     *
     * @param callback 回调接口实例，如果为null则不执行任何操作
     */
    public void setOnDateTimeChangedListener(OnDateTimeChangedListener callback) {
        mOnDateTimeChangedListener = callback;
    }

    /**
     * 当日期时间被改变时调用此方法，如果设置了日期时间改变监听器，则触发监听器的回调方法
     */
    private void onDateTimeChanged() {
        if (mOnDateTimeChangedListener != null) {
            mOnDateTimeChangedListener.onDateTimeChanged(this, getCurrentYear(),
                    getCurrentMonth(), getCurrentDay(), getCurrentHourOfDay(), getCurrentMinute());
        }
    }
}
