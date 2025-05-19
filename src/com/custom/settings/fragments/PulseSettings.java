/*
 * Copyright (C) 2016-2024 crDroid Android Project
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
package com.custom.settings.fragments;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preferences.ui.AdaptiveSwitchPreference;
import com.custom.settings.preferences.colorpicker.ColorPickerPreference;
import static com.custom.settings.preferences.SecureSettingsStore.*;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = PulseSettings.class.getSimpleName();

    private static final String NAVBAR_PULSE_ENABLED_KEY = "navbar_pulse_enabled";
    private static final String LOCKSCREEN_PULSE_ENABLED_KEY = "lockscreen_pulse_enabled";
    private static final String AMBIENT_PULSE_ENABLED_KEY = "ambient_pulse_enabled";
    private static final String PULSE_SMOOTHING_KEY = "pulse_smoothing_enabled";
    private static final String PULSE_COLOR_MODE_KEY = "pulse_color_mode";
    private static final String PULSE_COLOR_MODE_CHOOSER_KEY = "pulse_color_user";
    private static final String PULSE_COLOR_MODE_LAVA_SPEED_KEY = "pulse_lavalamp_speed";
    private static final String PULSE_RENDER_CATEGORY_SOLID = "pulse_2";
    private static final String PULSE_RENDER_CATEGORY_FADING = "pulse_fading_bars_category";
    private static final String PULSE_RENDER_MODE_KEY = "pulse_render_style";
    private static final int RENDER_STYLE_FADING_BARS = 0;
    private static final int RENDER_STYLE_SOLID_LINES = 1;
    private static final int COLOR_TYPE_ACCENT = 0;
    private static final int COLOR_TYPE_USER = 1;
    private static final int COLOR_TYPE_LAVALAMP = 2;
    private static final int COLOR_TYPE_AUTO = 3;

    private static final String PULSE_SETTINGS_FOOTER = "pulse_settings_footer";

    private AdaptiveSwitchPreference mNavbarPulse;
    private AdaptiveSwitchPreference mAmbientPulse;
    private AdaptiveSwitchPreference mFadingPulse;
    private AdaptiveSwitchPreference mPulseSmoothing;
    private ListPreference mPulseColorMode;
    private ColorPickerPreference mPulseColor;
    private ListPreference mPulseRenderMode;
    private Preference mPulseLavaSpeed;
    private Preference mPulseLavaDensity;
    private Preference mPulseSolidSpeed;
    private Preference mPulseSolidDensity;
    private Preference mPulseFilledBlockSize;
    private Preference mPulseEmptyBlockSize;
    private Preference mPulseFilledFadeIn;
    private Preference mPulseFilledFadeOut;
    private Preference mPulseEmptyFadeIn;
    private Preference mPulseEmptyFadeOut;
    private AdaptiveSwitchPreference mPulseCustomDots;
    private Preference mPulseCustomDotsValue;
    private AdaptiveSwitchPreference mPulseCustomDivider;
    private Preference mPulseCustomDividerValue;
    private AdaptiveSwitchPreference mPulseShowVolume;
    private Preference mPulseShowVolumeValue;
    private AdaptiveSwitchPreference mPulseShowVolumeDots;
    private Preference mPulseShowVolumeDotsValue;
    private AdaptiveSwitchPreference mPulseShowVolumeDivider;
    private Preference mPulseShowVolumeDividerValue;
    private AdaptiveSwitchPreference mPulseShowVolumeFilled;
    private Preference mPulseShowVolumeFilledValue;
    private AdaptiveSwitchPreference mPulseShowVolumeEmpty;
    private Preference mPulseShowVolumeEmptyValue;
    private AdaptiveSwitchPreference mPulseShowVolumeFadeIn;
    private Preference mPulseShowVolumeFadeInValue;
    private AdaptiveSwitchPreference mPulseShowVolumeFadeOut;
    private Preference mPulseShowVolumeFadeOutValue;
    private AdaptiveSwitchPreference mPulseShowVolumeFilledFadeIn;
    private Preference mPulseShowVolumeFilledFadeInValue;
    private AdaptiveSwitchPreference mPulseShowVolumeFilledFadeOut;
    private Preference mPulseShowVolumeFilledFadeOutValue;
    private AdaptiveSwitchPreference mPulseShowVolumeEmptyFadeIn;
    private Preference mPulseShowVolumeEmptyFadeInValue;
    private AdaptiveSwitchPreference mPulseShowVolumeEmptyFadeOut;
    private Preference mPulseShowVolumeEmptyFadeOutValue;
    private ListPreference mPulseShowVolumeColorMode;
    private ColorPickerPreference mPulseShowVolumeColor;
    private ListPreference mPulseShowVolumeRenderMode;
    private Preference mPulseShowVolumeLavaSpeed;
    private Preference mPulseShowVolumeLavaDensity;
    private Preference mPulseShowVolumeSolidSpeed;
    private Preference mPulseShowVolumeSolidDensity;
    private Preference mPulseShowVolumeFilledBlockSize;
    private Preference mPulseShowVolumeEmptyBlockSize;
    private AdaptiveSwitchPreference mPulseShowVolumeCustomDots;
    private Preference mPulseShowVolumeCustomDotsValue;
    private AdaptiveSwitchPreference mPulseShowVolumeCustomDivider;
    private Preference mPulseShowVolumeCustomDividerValue;
    private Preference mFooterPref;

    private PreferenceCategory mFadingBarsCat;
    private PreferenceCategory mSolidBarsCat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pulse_settings);

        ContentResolver resolver = getContext().getContentResolver();

        mNavbarPulse = (AdaptiveSwitchPreference) findPreference(NAVBAR_PULSE_ENABLED_KEY);
        boolean navbarPulse = Settings.Secure.getIntForUser(resolver,
                NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mNavbarPulse.setEnabled(navbarPulse);
        mNavbarPulse.setOnPreferenceChangeListener(this);

        mAmbientPulse = (AdaptiveSwitchPreference) findPreference(AMBIENT_PULSE_ENABLED_KEY);
        boolean ambientPulse = Settings.Secure.getIntForUser(resolver,
                AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mAmbientPulse.setEnabled(ambientPulse);
        mAmbientPulse.setOnPreferenceChangeListener(this);

        mPulseColorMode = (ListPreference) findPreference(PULSE_COLOR_MODE_KEY);
        mPulseColor = (ColorPickerPreference) findPreference(PULSE_COLOR_MODE_CHOOSER_KEY);
        mPulseLavaSpeed = findPreference(PULSE_COLOR_MODE_LAVA_SPEED_KEY);
        mPulseColorMode.setOnPreferenceChangeListener(this);

        mPulseRenderMode = (ListPreference) findPreference(PULSE_RENDER_MODE_KEY);
        mPulseRenderMode.setOnPreferenceChangeListener(this);

        mFadingBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_FADING);
        mSolidBarsCat = (PreferenceCategory) findPreference(
                PULSE_RENDER_CATEGORY_SOLID);

        mPulseSmoothing = (AdaptiveSwitchPreference) findPreference(PULSE_SMOOTHING_KEY);

        mFooterPref = findPreference(PULSE_SETTINGS_FOOTER);
        mFooterPref.setTitle(R.string.pulse_help_policy_notice_summary);

        updateAllPrefs();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();
        if (preference == mNavbarPulse) {
            boolean val = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver,
                NAVBAR_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mAmbientPulse) {
            boolean val = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver,
                AMBIENT_PULSE_ENABLED, val ? 1 : 0, UserHandle.USER_CURRENT);
            updateAllPrefs();
            return true;
        } else if (preference == mPulseColorMode) {
            updateColorPrefs(Integer.valueOf(String.valueOf(newValue)));
            return true;
        } else if (preference == mPulseRenderMode) {
            updateRenderCategories(Integer.valueOf(String.valueOf(newValue)));
            return true;
        }
        return false;
    }

    private void updateAllPrefs() {
        ContentResolver resolver = getContext().getContentResolver();
        boolean navbarPulse = Settings.Secure.getIntForUser(resolver,
                NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        boolean ambientPulse = Settings.Secure.getIntForUser(resolver,
                AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        mNavbarPulse.setEnabled(navbarPulse);
        mAmbientPulse.setEnabled(ambientPulse);

        int colorMode = Settings.Secure.getIntForUser(resolver,
                PULSE_COLOR_MODE, COLOR_TYPE_LAVALAMP, UserHandle.USER_CURRENT);
        updateColorPrefs(colorMode);

        int renderMode = Settings.Secure.getIntForUser(resolver,
                PULSE_RENDER_STYLE, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT);
        updateRenderCategories(renderMode);
    }

    private void updateColorPrefs(int colorMode) {
        ContentResolver resolver = getContext().getContentResolver();
        boolean navbarPulse = Settings.Secure.getIntForUser(resolver,
                NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        boolean ambientPulse = Settings.Secure.getIntForUser(resolver,
                AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        mPulseColor.setEnabled((colorMode == COLOR_TYPE_USER) && (navbarPulse || ambientPulse));
        mPulseLavaSpeed.setEnabled((colorMode == COLOR_TYPE_LAVALAMP) && (navbarPulse || ambientPulse));
    }

    private void updateRenderCategories(int renderMode) {
        ContentResolver resolver = getContext().getContentResolver();
        boolean navbarPulse = Settings.Secure.getIntForUser(resolver,
                NAVBAR_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        boolean ambientPulse = Settings.Secure.getIntForUser(resolver,
                AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0;

        mFadingBarsCat.setEnabled((renderMode == RENDER_STYLE_FADING_BARS) && (navbarPulse || ambientPulse));
        mSolidBarsCat.setEnabled((renderMode == RENDER_STYLE_SOLID_LINES) && (navbarPulse || ambientPulse));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }
}
