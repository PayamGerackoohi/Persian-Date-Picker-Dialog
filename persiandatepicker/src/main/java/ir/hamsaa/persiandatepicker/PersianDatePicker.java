package ir.hamsaa.persiandatepicker;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Date;

import cn.carbswang.android.numberpickerview.library.NumberPickerView;
import ir.hamsaa.persiandatepicker.util.PersianCalendar;
import ir.hamsaa.persiandatepicker.util.PersianCalendarConstants;
import ir.hamsaa.persiandatepicker.util.PersianCalendarUtils;
import ir.hamsaa.persiandatepicker.util.PersianHelper;

class PersianDatePicker extends LinearLayout {
    private static final String TAG = "PersianDatePicker";

    private final PersianCalendar pCalendar;
    private int selectedMonth = 7;
    private int selectedYear = 1370;
    private int selectedDay = 22;
    private boolean displayMonthNames;
    private OnDateChangedListener mListener;
    private NumberPickerView yearPicker;
    private NumberPickerView monthPicker;
    private NumberPickerView dayPicker;

    private int minYear;
    private int maxYear;

    private boolean displayDescription;
    private TextView descriptionTextView;
    private Typeface typeFace;
    private int dividerColor;
    private int yearRange;
    private String[] yearValues;
    private String[] monthValues;
    private String[] dayValues;

    public PersianDatePicker(Context context) {
        this(context, null, -1);
    }

    public PersianDatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    private void updateVariablesFromXml(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PersianDatePicker, 0, 0);
        yearRange = a.getInteger(R.styleable.PersianDatePicker_yearRange, 10);
        /*
         * Initializing yearNumberPicker min and max values If minYear and
         * maxYear attributes are not set, use (current year - 10) as min and
         * (current year + 10) as max.
         */
        minYear = a.getInt(R.styleable.PersianDatePicker_minYear, pCalendar.getPersianYear() - yearRange);
        maxYear = a.getInt(R.styleable.PersianDatePicker_maxYear, pCalendar.getPersianYear() + yearRange);
        displayMonthNames = a.getBoolean(R.styleable.PersianDatePicker_displayMonthNames, false);
        /*
         * displayDescription
         */
        displayDescription = a.getBoolean(R.styleable.PersianDatePicker_displayDescription, false);
        selectedDay = a.getInteger(R.styleable.PersianDatePicker_selectedDay, pCalendar.getPersianDay());
        selectedYear = a.getInt(R.styleable.PersianDatePicker_selectedYear, pCalendar.getPersianYear());
        selectedMonth = a.getInteger(R.styleable.PersianDatePicker_selectedMonth, pCalendar.getPersianMonth());

        // if you pass selected year before min year, then we need to push min year to before that
        if (minYear > selectedYear) {
            minYear = selectedYear - yearRange;
        }

        if (maxYear < selectedYear) {
            maxYear = selectedYear + yearRange;
        }
        a.recycle();
    }

    public PersianDatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // get layout inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // inflate views
        View view = inflater.inflate(R.layout.sl_persian_date_picker, this);

        // get views
        yearPicker = view.findViewById(R.id.yearPicker);
        monthPicker = view.findViewById(R.id.monthPicker);
        dayPicker = view.findViewById(R.id.dayPicker);
        descriptionTextView = view.findViewById(R.id.descriptionTextView);

        // init calendar
        pCalendar = new PersianCalendar();

        // update variables from xml
        updateVariablesFromXml(context, attrs);

        Log.e(TAG, "PersianDatePicker: is called.");
        // update view
        updateViewData();
    }

    public void setMaxYear(int maxYear) {
        Log.e(TAG, "setMaxYear: is called");
        this.maxYear = maxYear;
        updateViewData();
    }

    public void setMinYear(int minYear) {
        this.minYear = minYear;
        updateViewData();
    }

    public void setTypeFace(Typeface typeFace) {
        this.typeFace = typeFace;
        updateViewData();
    }

    public void setDividerColor(@ColorInt int color) {
        this.dividerColor = color;
        updateViewData();
    }

    private void setDividerColor(NumberPicker picker, int color) {

        java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (java.lang.reflect.Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    pf.set(picker, colorDrawable);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Resources.NotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private void updateViewData() {
        Log.e(TAG, "testtest: updateViewData: is called.");
        if (typeFace != null) {
            yearPicker.setContentTextTypeface(typeFace);
            monthPicker.setContentTextTypeface(typeFace);
            dayPicker.setContentTextTypeface(typeFace);
        }

        if (dividerColor > 0) {
            yearPicker.setDividerColor(dividerColor);
            monthPicker.setDividerColor(dividerColor);
            dayPicker.setDividerColor(dividerColor);
        }

        setupYearPicker(minYear, maxYear);
        setupMonthPicker();
        setupDayPicker(31);

        if (displayDescription) {
            descriptionTextView.setVisibility(View.VISIBLE);
//            descriptionTextView.setText(getDisplayPersianDate().getPersianLongDate());
        }
        updateTexts();
    }

    private void calculateSelectedDay() {
        if (selectedDay > 31 || selectedDay < 1) {
            throw new IllegalArgumentException(String.format("Selected day (%d) must be between 1 and 31", selectedDay));
        }
        if (selectedMonth > 6 && selectedMonth < 12 && selectedDay == 31) {
            selectedDay = 30;
        } else {
            boolean isLeapYear = PersianCalendarUtils.isPersianLeapYear(selectedYear);
            if (isLeapYear && selectedDay == 31) {
                selectedDay = 30;
            } else if (selectedDay > 29) {
                selectedDay = 29;
            }
        }
    }

    private void setupDayPicker(int max) {
        calculateSelectedDay();
        dayValues = generateLinearArray(1, max);
        setupNumberPickerView(dayPicker, 1, max, dayValues);
        dayPicker.setValue(selectedDay);
        dayPicker.setOnValueChangeListenerInScrolling(new NumberPickerView.OnValueChangeListenerInScrolling() {
            @Override
            public void onValueChangeInScrolling(NumberPickerView picker, int oldVal, int newVal) {
                updateDate(null, null, newVal);
            }
        });
    }

    private void setupMonthPicker() {
        monthValues = PersianCalendarConstants.persianMonthNames;
        setupNumberPickerView(monthPicker, 1, 12, monthValues);
        if (selectedMonth < 1 || selectedMonth > 12) {
            throw new IllegalArgumentException(String.format("Selected month (%d) must be between 1 and 12", selectedMonth));
        }
        monthPicker.setValue(selectedMonth);
        monthPicker.setOnValueChangeListenerInScrolling(new NumberPickerView.OnValueChangeListenerInScrolling() {
            @Override
            public void onValueChangeInScrolling(NumberPickerView picker, int oldVal, int newVal) {
                updateDate(null, newVal, null);
            }
        });
    }

    private String[] generateLinearArray(int from, int to) {
        String[] values;
        int c = 0;
        if (from > to) {
            values = new String[from - to + 1];
            for (int i = from; i >= to; i--) {
                values[c++] = PersianHelper.toPersianNumber(i);
            }
        } else {
            values = new String[to - from + 1];
            for (int i = from; i <= to; i++) {
                values[c++] = PersianHelper.toPersianNumber(i);
            }
        }
        return values;
    }

    private void setupYearPicker(int min, int max) {
        yearValues = generateLinearArray(min, max);
        setupNumberPickerView(yearPicker, min, max, yearValues);

        calculateSelectedYear();
        yearPicker.setOnValueChangeListenerInScrolling(new NumberPickerView.OnValueChangeListenerInScrolling() {
            @Override
            public void onValueChangeInScrolling(NumberPickerView picker, int oldVal, int newVal) {
                updateDate(newVal, null, null);
            }
        });
    }

    private void calculateSelectedYear() {
        if (selectedYear > maxYear) {
            selectedYear = maxYear;
        }
        if (selectedYear < minYear) {
            selectedYear = minYear;
        }
        yearPicker.setValue(selectedYear);
    }

    private void setupNumberPickerView(NumberPickerView numberPicker, int min, int max, String[] values) {
        Log.e(TAG, "setupNumberPickerView: min<" + min + "> max<" + max + ">");
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setMinValue(min);
        boolean increasing = values.length >= numberPicker.getDisplayedValues().length;
        if (increasing) {
            numberPicker.setDisplayedValues(values);
            numberPicker.setMaxValue(max);
        } else {
            numberPicker.setMaxValue(max);
            numberPicker.setDisplayedValues(values);
        }
        numberPicker.setValue((min + max) / 2);
    }

    private void updateDate(Integer yearIndex, Integer monthIndex, Integer dayIndex) {
        if (dayIndex == null) {
            int year = yearPicker.getValue();
            int month = monthPicker.getValue();

            if (yearIndex != null) {
                year = Integer.parseInt(yearValues[yearIndex]);
            }
            if (monthIndex != null) {
                month = monthIndex;
            }
            boolean isLeapYear = PersianCalendarUtils.isPersianLeapYear(year);

            if (month < 7) {
                setupDayPicker(31);
            } else if (month < 12) {
                setupDayPicker(30);
            } else if (month == 12) {
                if (isLeapYear) {
                    setupDayPicker(30);
                } else {
                    setupDayPicker(29);
                }
            }
        }
        updateTexts();
    }

    private void updateTexts() {
        Log.e(TAG, "testtest: updateTexts: year<" + yearPicker.getValue() + "> month<" + monthPicker.getValue() + "> day<" + dayPicker.getValue() + ">");
        if (displayDescription) {
            descriptionTextView.setText(getDisplayPersianDate().getPersianLongDate());
        }

        if (mListener != null) {
            mListener.onDateChanged(yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());
        }
    }

    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        mListener = onDateChangedListener;
    }

    /**
     * The callback used to indicate the user changed the date.
     * A class that wants to be notified when the date of PersianDatePicker
     * changes should implement this interface and register itself as the
     * listener of date change events using the PersianDataPicker's
     * setOnDateChangedListener method.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param newYear  The year that was set.
         * @param newMonth The month that was set (1-12)
         * @param newDay   The day of the month that was set.
         */
        void onDateChanged(int newYear, int newMonth, int newDay);
    }

    public Date getDisplayDate() {
        return getPersianCalendar().getTime();
    }

    @NonNull
    private PersianCalendar getPersianCalendar() {
        PersianCalendar displayPersianDate = new PersianCalendar();
        Log.e(TAG, "testtest: getPersianCalendar: year<" + yearPicker.getValue() + "> month<" + monthPicker.getValue() + "> day<" + dayPicker.getValue() + ">");
        displayPersianDate.setPersianDate(yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());
        return displayPersianDate;
    }

    public void setDisplayDate(Date displayDate) {
        setDisplayPersianDate(new PersianCalendar(displayDate.getTime()));
    }

    public PersianCalendar getDisplayPersianDate() {
        return getPersianCalendar();
    }

    public void setDisplayPersianDate(PersianCalendar displayPersianDate) {
        Log.e(TAG, "testtest: setDisplayPersianDate: year<" + displayPersianDate.getPersianYear() + ">");
        int year = displayPersianDate.getPersianYear();
        int month = displayPersianDate.getPersianMonth();
        int day = displayPersianDate.getPersianDay();
        if (month > 6 && month < 12 && day == 31) {
            day = 30;
        } else {
            boolean isLeapYear = PersianCalendarUtils.isPersianLeapYear(year);
            if (isLeapYear && day == 31) {
                day = 30;
            } else if (day > 29) {
                day = 29;
            }
        }

        selectedYear = year;
        selectedMonth = month;
        selectedDay = day;

        // if you pass selected year before min year, then we need to push min year to before that
        if (minYear > selectedYear) {
            minYear = selectedYear - yearRange;
        }

        // if you pass selected year after max year, then we need to push max year to after that
        if (maxYear < selectedYear) {
            maxYear = selectedYear + yearRange;
        }

        setupYearPicker(minYear, maxYear);
        yearPicker.setValue(year);
        monthPicker.setValue(month);
        dayPicker.setValue(day);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        // end

        ss.datetime = this.getDisplayDate().getTime();
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        // end

        setDisplayDate(new Date(ss.datetime));
    }

    static class SavedState extends BaseSavedState {
        long datetime;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.datetime = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(this.datetime);
        }

        // required field that makes Parcelables from a Parcel
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
