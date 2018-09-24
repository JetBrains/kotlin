/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.klint.detector.api;

import com.android.annotations.NonNull;
import com.google.common.annotations.Beta;

/**
 * Enum which describes the different computation speeds of various detectors
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public enum Speed {
    /** The detector can run very quickly */
    FAST("Fast"),

    /** The detector runs reasonably fast */
    NORMAL("Normal"),

    /** The detector might take a long time to run */
    SLOW("Slow"),

    /** The detector might take a huge amount of time to run */
    REALLY_SLOW("Really Slow");

    private final String mDisplayName;

    Speed(@NonNull String displayName) {
        mDisplayName = displayName;
    }

    /**
     * Returns the user-visible description of the speed of the given
     * detector
     *
     * @return the description of the speed to display to the user
     */
    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }
}