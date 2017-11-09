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

package com.android.tools.klint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.annotations.Beta;

/**
 * Lint configuration for an Android project such as which specific rules to include,
 * which specific rules to exclude, and which specific errors to ignore.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class Configuration {
    /**
     * Checks whether this issue should be ignored because the user has already
     * suppressed the error? Note that this refers to individual issues being
     * suppressed/ignored, not a whole detector being disabled via something
     * like {@link #isEnabled(Issue)}.
     *
     * @param context the context used by the detector when the issue was found
     * @param issue the issue that was found
     * @param location the location of the issue
     * @param message the associated user message
     * @return true if this issue should be suppressed
     */
    public boolean isIgnored(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        return false;
    }

    /**
     * Returns false if the given issue has been disabled. This is just
     * a convenience method for {@code getSeverity(issue) != Severity.IGNORE}.
     *
     * @param issue the issue to check
     * @return false if the issue has been disabled
     */
    public boolean isEnabled(@NonNull Issue issue) {
        return getSeverity(issue) != Severity.IGNORE;
    }

    /**
     * Returns the severity for a given issue. This is the same as the
     * {@link Issue#getDefaultSeverity()} unless the user has selected a custom
     * severity (which is tool context dependent).
     *
     * @param issue the issue to look up the severity from
     * @return the severity use for issues for the given detector
     */
    public Severity getSeverity(@NonNull Issue issue) {
        return issue.getDefaultSeverity();
    }

    // Editing configurations

    /**
     * Marks the given warning as "ignored".
     *
     * @param context The scanning context
     * @param issue the issue to be ignored
     * @param location The location to ignore the warning at, if any
     * @param message The message for the warning
     */
    public abstract void ignore(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message);

    /**
     * Sets the severity to be used for this issue.
     *
     * @param issue the issue to set the severity for
     * @param severity the severity to associate with this issue, or null to
     *            reset the severity to the default
     */
    public abstract void setSeverity(@NonNull Issue issue, @Nullable Severity severity);

    // Bulk editing support

    /**
     * Marks the beginning of a "bulk" editing operation with repeated calls to
     * {@link #setSeverity} or {@link #ignore}. After all the values have been
     * set, the client <b>must</b> call {@link #finishBulkEditing()}. This
     * allows configurations to avoid doing expensive I/O (such as writing out a
     * config XML file) for each and every editing operation when they are
     * applied in bulk, such as from a configuration dialog's "Apply" action.
     */
    public void startBulkEditing() {
    }

    /**
     * Marks the end of a "bulk" editing operation, where values should be
     * committed to persistent storage. See {@link #startBulkEditing()} for
     * details.
     */
    public void finishBulkEditing() {
    }
}
