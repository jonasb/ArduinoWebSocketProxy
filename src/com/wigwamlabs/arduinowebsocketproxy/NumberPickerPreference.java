package com.wigwamlabs.arduinowebsocketproxy;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {
    private int mValue;
    private final int mMin, mMax;
    private NumberPicker mNumberPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);

        mMax = a.getInt(R.styleable.NumberPickerPreference_max, 5);
        mMin = a.getInt(R.styleable.NumberPickerPreference_min, 0);

        a.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View view = inflater.inflate(R.layout.number_picker_dialog, null);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        mNumberPicker.setMaxValue(mMax);
        mNumberPicker.setMinValue(mMin);
        mNumberPicker.setWrapSelectorWheel(false);

        return view;
    }

    @Override
    public CharSequence getSummary() {
        return Integer.toString(mValue);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mNumberPicker.setValue(mValue);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            final int newValue = mNumberPicker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        final int defaultInt = (defaultValue != null ? (Integer) defaultValue : mMin);
        setValue(restorePersistedValue ? getPersistedInt(defaultInt) : defaultInt);
    }

    private void setValue(int value) {
        mValue = value;
        persistInt(value);
    }
}
