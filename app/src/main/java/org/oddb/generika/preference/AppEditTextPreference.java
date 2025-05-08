package org.oddb.generika.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import org.oddb.generika.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppEditTextPreference extends EditTextPreference {
    private Pattern validationRegex = null;
    private String validationErrorMessage = null;
    public AppEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.validationErrorMessage = context.getString(R.string.preference_invalid_value);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppEditTextPreference);
        try {
            String validationMessage = a.getString(R.styleable.AppEditTextPreference_validationMessage);
            if (validationMessage != null) {
                this.validationErrorMessage = validationMessage;
            }
            String format = a.getString(R.styleable.AppEditTextPreference_validationFormat);
            if (format != null) {
                validationRegex = Pattern.compile(format);
            }
        } finally {
            a.recycle();
        }
        initOnChangeListener();
    }

    public AppEditTextPreference(Context context) {
        super(context);

        initOnChangeListener();
    }

    private void initOnChangeListener() {
        // set current value as summary of preference
        AppEditTextPreference _this = this;
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                    Preference preference, Object newValue) {

                if (!(newValue instanceof String)) {
                    return false;
                }
                if (((String)newValue).isEmpty()) {
                    preference.setSummary((String)newValue);
                    return true;
                }
                if (validationRegex != null) {
                    Matcher m = validationRegex.matcher((String)newValue);
                    if (m.matches()) {
                        preference.setSummary((String)newValue);
                        return true;
                    }
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.error)
                            .setMessage(_this.validationErrorMessage)
                            // A null listener allows the button to dismiss the dialog and take no further action.
                            .setPositiveButton(android.R.string.ok, null)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .show();
                    return false;
                }
                preference.setSummary((String)newValue);
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return super.getText();
    }
}
