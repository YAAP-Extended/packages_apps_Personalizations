/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.custom.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SearchIndexable
public class LockScreenWidgets extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "LockScreenWidgets";

    private static final String EXTRA_WIDGET_1_KEY = "custom_widgets1";
    private static final String EXTRA_WIDGET_2_KEY = "custom_widgets2";
    private static final String EXTRA_WIDGET_3_KEY = "custom_widgets3";
    private static final String EXTRA_WIDGET_4_KEY = "custom_widgets4";

    private static final String LOCKSCREEN_WIDGETS_EXTRAS_KEY = "lockscreen_widgets_extras";
    private static final String LOCKSCREEN_WIDGETS_ENABLED_KEY = "lockscreen_widgets_enabled";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Preference mExtraWidget1;
    private Preference mExtraWidget2;
    private Preference mExtraWidget3;
    private Preference mExtraWidget4;

    private SwitchPreferenceCompat mLockScreenWidgetsEnabledPref;
    private List<Preference> mWidgetPreferences;

    private final Map<Preference, String> widgetKeysMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.custom_settings_lockscreen_widgets);

        initializePreferences();
        setupListeners();

        boolean isLsWidgetsEnabled = Settings.System.getIntForUser(
                getActivity().getContentResolver(),
                LOCKSCREEN_WIDGETS_ENABLED_KEY,
                0,
                UserHandle.USER_CURRENT) != 0;

        mLockScreenWidgetsEnabledPref.setChecked(isLsWidgetsEnabled);
        showWidgetPreferences(isLsWidgetsEnabled);

        loadInitialPreferences();
    }

    private void initializePreferences() {
        mExtraWidget1 = findPreference(EXTRA_WIDGET_1_KEY);
        mExtraWidget2 = findPreference(EXTRA_WIDGET_2_KEY);
        mExtraWidget3 = findPreference(EXTRA_WIDGET_3_KEY);
        mExtraWidget4 = findPreference(EXTRA_WIDGET_4_KEY);

        mWidgetPreferences = Arrays.asList(
                mExtraWidget1,
                mExtraWidget2,
                mExtraWidget3,
                mExtraWidget4
        );

        mLockScreenWidgetsEnabledPref = findPreference(LOCKSCREEN_WIDGETS_ENABLED_KEY);
    }

    private void setupListeners() {
        for (Preference widgetPref : mWidgetPreferences) {
            widgetPref.setOnPreferenceChangeListener(this);
            widgetKeysMap.put(widgetPref, "");
        }
        mLockScreenWidgetsEnabledPref.setOnPreferenceChangeListener(this);
    }

    private void showWidgetPreferences(boolean isEnabled) {
        for (Preference widgetPref : mWidgetPreferences) {
            widgetPref.setVisible(isEnabled);
        }
    }

    private void loadInitialPreferences() {
        ContentResolver resolver = getActivity().getContentResolver();
        String extraWidgets = Settings.System.getString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY);
        setWidgetAndPreferenceValues(extraWidgets, mExtraWidget1, mExtraWidget2, mExtraWidget3, mExtraWidget4);
    }

    private void setWidgetAndPreferenceValues(String widgets, Preference... preferences) {
        if (widgets == null) {
            return;
        }
        List<String> widgetList = Arrays.asList(widgets.split(","));
        for (int i = 0; i < preferences.length && i < widgetList.size(); i++) {
            String value = widgetList.get(i).trim();
            Preference pref = preferences[i];
            widgetKeysMap.put(pref, value);
            if (pref instanceof ListPreference) {
                ((ListPreference) pref).setValue(value);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (widgetKeysMap.containsKey(preference)) {
            widgetKeysMap.put(preference, String.valueOf(newValue));
            updateWidgetPreferences();
            return true;
        } else if (preference == mLockScreenWidgetsEnabledPref) {
            boolean isEnabled = (boolean) newValue;
            showWidgetPreferences(isEnabled);
            mLockScreenWidgetsEnabledPref.setChecked(isEnabled);

            ContentResolver resolver = getContext().getContentResolver();
            int userId = UserHandle.USER_CURRENT;

            // hacky way to update the clock size but works until i figure out why SystemSettings flow update is ignored
            Settings.System.putIntForUser(
                    resolver,
                    LOCKSCREEN_WIDGETS_ENABLED_KEY,
                    isEnabled ? 1 : 0,
                    userId
            );

            int defaultDoubleLineClock = getContext().getResources().getInteger(
                    com.android.internal.R.integer.config_doublelineClockDefault
            );
            int currentValue = Settings.Secure.getIntForUser(
                    resolver,
                    Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                    defaultDoubleLineClock,
                    userId
            );
            Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                    currentValue == 1 ? 0 : 1,
                    userId
            );
            mHandler.postDelayed(() -> {
                Settings.Secure.putIntForUser(
                        resolver,
                        Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                        currentValue,
                        userId
                );
            }, 100);

            return true;
        }
        return false;
    }

    private void updateWidgetPreferences() {
        List<String> extraWidgetsList = Arrays.asList(
                widgetKeysMap.get(mExtraWidget1),
                widgetKeysMap.get(mExtraWidget2),
                widgetKeysMap.get(mExtraWidget3),
                widgetKeysMap.get(mExtraWidget4)
        );

        extraWidgetsList = replaceEmptyWithNone(extraWidgetsList);

        String extraWidgets = TextUtils.join(",", extraWidgetsList);

        ContentResolver resolver = getActivity().getContentResolver();
        Settings.System.putString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY, extraWidgets);
    }

    private List<String> replaceEmptyWithNone(List<String> inputList) {
        return inputList.stream()
                .map(s -> TextUtils.isEmpty(s) ? "none" : s)
                .collect(Collectors.toList());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.custom_settings_lockscreen_widgets) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return super.getNonIndexableKeys(context);
                }
            };
}
