/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.custom.settings.preferences;

import android.provider.Settings;

public final class SecureSettingsStore {
    /**
     * Whether to show pulse in navigation bar
     * @hide
     */
    public static final String NAVBAR_PULSE_ENABLED = "navbar_pulse_enabled";

    /**
     * Whether to show pulse on lockscreen
     * @hide
     */
    public static final String LOCKSCREEN_PULSE_ENABLED = "lockscreen_pulse_enabled";

    /**
     * Whether to show pulse on ambient display
     * @hide
     */
    public static final String AMBIENT_PULSE_ENABLED = "ambient_pulse_enabled";

    /**
     * Pulse color mode
     * @hide
     */
    public static final String PULSE_COLOR_MODE = "pulse_color_mode";

    /**
     * Pulse render style
     * @hide
     */
    public static final String PULSE_RENDER_STYLE = "pulse_render_style";

    private SecureSettingsStore() {
        // Prevent instantiation
    }
}
