/*
 * Copyright (C) 2014 TeamCanjica https://github.com/TeamCanjica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teamcanjica.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MasterSeekBarDialogPreference extends DialogPreference implements OnPreferenceChangeListener
{
    private static final int DEFAULT_MIN_PROGRESS = 0;
    private static final int DEFAULT_MAX_PROGRESS = 100;
    private static final int DEFAULT_PROGRESS = 0;

    private int mMinProgress;
    private int mMaxProgress;
    private int mProgress;
    private int stepSize = 0;
    private CharSequence mProgressTextSuffix;
    private TextView mProgressText;
    private SeekBar mSeekBar;
    private boolean isFloat = false;

    private static final String FILE_READAHEADKB = "/sys/block/mmcblk0/queue/read_ahead_kb";
    private static final String FILE_CPU_VOLTAGE = "/sys/kernel/liveopp/arm_step";
    private static final String FILE_CYCLE_CHARGING = "/sys/kernel/abb-fg/fg_cyc";
    private static final String FILE_GPU_VOLTAGE = "/sys/kernel/mali/mali_dvfs_config";
    private static final int defaultGPUVoltValues[] = {0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x29, 0x2a, 0x2b, 
    	0x2c, 0x2d, 0x2f, 0x30, 0x32, 0x33, 0x34, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f};
    private static final int defaultCPUVoltValues[] = {0x18, 0x1a, 0x20, 0x24, 0x2f, 0x32, 0x3f, 0x3f, 0x3f, 0x3f};
    private static final int voltSteps[] = {0, -12, -24, -36, -48, -72, -60, -84, -96};

    public MasterSeekBarDialogPreference(Context context) {
        this(context, null);
    }

    public MasterSeekBarDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);

        // Get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MasterSeekBarDialogPreference, 0, 0);
        try {
            setMinProgress(a.getInteger(R.styleable.MasterSeekBarDialogPreference_min, DEFAULT_MIN_PROGRESS));
            setMaxProgress(a.getInteger(R.styleable.MasterSeekBarDialogPreference_android_max, DEFAULT_MAX_PROGRESS));
            setProgressTextSuffix(a.getString(R.styleable.MasterSeekBarDialogPreference_progressTextSuffix));
            stepSize = a.getInteger(R.styleable.MasterSeekBarDialogPreference_stepSize, 1);
            isFloat = a.getBoolean(R.styleable.MasterSeekBarDialogPreference_isFloat, false);
        }
        finally {
            a.recycle();
        }

        // Set layout
        setDialogLayoutResource(R.layout.preference_seek_bar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        setProgress(restore ? getPersistedInt(DEFAULT_PROGRESS) : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_PROGRESS);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView dialogMessageText = (TextView) view.findViewById(R.id.text_dialog_message);
        dialogMessageText.setText(getDialogMessage());

        mProgressText = (TextView) view.findViewById(R.id.text_progress);

        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update text that displays the current SeekBar progress value
                // Note: This does not persist the progress value. that is only ever done in setProgress()
            	String progressStr;
            	int mStepSize = stepSize;
            	if (isFloat) {
            		mStepSize = stepSize / 10;
            	}
            	if (mStepSize >= 1) {
            		progressStr = String.valueOf(Math.round((progress + mMinProgress) / mStepSize) * mStepSize);
        		} else {
        			progressStr = String.valueOf(progress + mMinProgress);
        		}
            	mProgressText.setText(mProgressTextSuffix == null ? progressStr : progressStr.concat(mProgressTextSuffix.toString()));
            }
        });
        mSeekBar.setMax(mMaxProgress - mMinProgress);
        mSeekBar.setProgress(mProgress - mMinProgress);
       // mSeekBar.setKeyProgressIncrement(stepSize);
    }

    public int getMinProgress() {
        return mMinProgress;
    }

    public void setMinProgress(int minProgress) {
        mMinProgress = minProgress;
        setProgress(Math.max(mProgress, mMinProgress));
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
        setProgress(Math.min(mProgress, mMaxProgress));
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        progress = Math.max(Math.min(progress, mMaxProgress), mMinProgress);
        int mStepSize = stepSize;
        if (isFloat) {
        	mStepSize = stepSize / 10;
        }
        if (progress != mProgress) {
        	if (mStepSize >= 1) {
        		progress = Math.round(progress / mStepSize) * mStepSize;
        	}
        		mProgress = progress;
        		persistInt(progress);
        		notifyChanged();
        }
    }

    public CharSequence getProgressTextSuffix() {
        return mProgressTextSuffix;
    }

    public void setProgressTextSuffix(CharSequence progressTextSuffix) {
        mProgressTextSuffix = progressTextSuffix;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
 
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            int seekBarProgress = mSeekBar.getProgress() + mMinProgress;
            if (callChangeListener(seekBarProgress)) {
                setProgress(seekBarProgress);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        // Save the instance state so that it will survive screen orientation changes and other events that may temporarily destroy it
        final Parcelable superState = super.onSaveInstanceState();
 
        // Set the state's value with the class member that holds current setting value
        final SavedState myState = new SavedState(superState);
        myState.minProgress = getMinProgress();
        myState.maxProgress = getMaxProgress();
        myState.progress = getProgress();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState()
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the state
        SavedState myState = (SavedState) state;
        setMinProgress(myState.minProgress);
        setMaxProgress(myState.maxProgress);
        setProgress(myState.progress);

        super.onRestoreInstanceState(myState.getSuperState());
    }

    private static class SavedState extends BaseSavedState {
        int minProgress;
        int maxProgress;
        int progress;
 
        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
 
            minProgress = source.readInt();
            maxProgress = source.readInt();
            progress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
 
            dest.writeInt(minProgress);
            dest.writeInt(maxProgress);
            dest.writeInt(progress);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
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

    // PREFERENCE STUFF STARTS HERE
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		String key = preference.getKey();

		if (key.equals(DeviceSettings.KEY_READAHEADKB)) {
			Utils.writeValue(FILE_READAHEADKB, String.valueOf((Integer) newValue));
		} else if (key.equals(DeviceSettings.KEY_DISCHARGING_THRESHOLD)) {
			Utils.writeValue(FILE_CYCLE_CHARGING, "dischar=" + String.valueOf((Integer) newValue));
		} else if (key.equals(DeviceSettings.KEY_RECHARGING_THRESHOLD)) {
			Utils.writeValue(FILE_CYCLE_CHARGING, "rechar=" + String.valueOf((Integer) newValue));
		} else if (key.equals(DeviceSettings.KEY_CPU_VOLTAGE)) {
			int i;
			for (i = 0; voltSteps[i] != (Integer) (Math.round((Integer) newValue / 12) * 12); ++i) {
			}
			for (int j = 0; j <= defaultCPUVoltValues.length - 1; ++j) {
			    Utils.writeValue(FILE_CPU_VOLTAGE + String.valueOf(j), "varm=0x" + Integer.toHexString(defaultCPUVoltValues[j] - i));
			}
		} else if (key.equals(DeviceSettings.KEY_GPU_VOLTAGE)) {
			int i;
			for (i = 0; voltSteps[i] != (Integer) (Math.round((Integer) newValue / 12) * 12); ++i) {
			}
			for (int j = 0; j <= defaultGPUVoltValues.length - 1; ++j) {
			    Utils.writeValue(FILE_GPU_VOLTAGE, j + " vape=0x" + Integer.toHexString(defaultGPUVoltValues[j] - i));
			}
		}

		return true;
	}

	public static void restore(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		Utils.writeValue(FILE_CYCLE_CHARGING, 
				"dischar=" + sharedPrefs.
						getInt(DeviceSettings.KEY_DISCHARGING_THRESHOLD, 100));

		Utils.writeValue(FILE_CYCLE_CHARGING,
				"rechar=" + sharedPrefs.
						getInt(DeviceSettings.KEY_RECHARGING_THRESHOLD, 100));

		Utils.writeValue(FILE_READAHEADKB,
				String.valueOf(sharedPrefs.
						getInt(DeviceSettings.KEY_READAHEADKB, 512)));

		// CPU VOLTAGE
		int i;
		for (i = 0; voltSteps[i] != sharedPrefs.
				getInt(DeviceSettings.KEY_CPU_VOLTAGE, voltSteps[0]); ++i) {
		}
		for (int j = 0; j <= defaultCPUVoltValues.length - 1; ++j) {
		    Utils.writeValue(FILE_CPU_VOLTAGE + String.valueOf(j), "varm=0x" + Integer.toHexString(defaultCPUVoltValues[j] - i));
		}

		// GPU VOLTAGE
		for (i = 0; voltSteps[i] != sharedPrefs.
				getInt(DeviceSettings.KEY_GPU_VOLTAGE, voltSteps[0]); ++i) {
		}
		for (int j = 0; j <= defaultGPUVoltValues.length - 1; ++j) {
		    Utils.writeValue(FILE_GPU_VOLTAGE, j + " vape=0x" + Integer.toHexString(defaultGPUVoltValues[j] - i));
		}
	}
}
