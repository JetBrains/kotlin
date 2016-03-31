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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Context;
import com.google.common.annotations.Beta;

/**
 * Interface implemented by listeners to be notified of lint events
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public interface LintListener {
    /** The various types of events provided to lint listeners */
    enum EventType {
        /** A lint check is about to begin */
        STARTING,

        /** Lint is about to check the given project, see {@link Context#getProject()} */
        SCANNING_PROJECT,

        /** Lint is about to check the given library project, see {@link Context#getProject()} */
        SCANNING_LIBRARY_PROJECT,

        /** Lint is about to check the given file, see {@link Context#file} */
        SCANNING_FILE,

        /** A new pass was initiated */
        NEW_PHASE,

        /** The lint check was canceled */
        CANCELED,

        /** The lint check is done */
        COMPLETED,
    }

    /**
     * Notifies listeners that the event of the given type has occurred.
     * Additional information, such as the file being scanned, or the project
     * being scanned, is available in the {@link Context} object (except for the
     * {@link EventType#STARTING}, {@link EventType#CANCELED} or
     * {@link EventType#COMPLETED} events which are fired outside of project
     * contexts.)
     *
     * @param driver the driver running through the checks
     * @param type the type of event that occurred
     * @param context the context providing additional information
     */
    void update(@NonNull LintDriver driver, @NonNull EventType type,
            @Nullable Context context);
}
