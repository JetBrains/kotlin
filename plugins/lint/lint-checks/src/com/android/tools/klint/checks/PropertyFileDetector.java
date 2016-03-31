/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.DOT_PROPERTIES;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.base.Splitter;

import java.io.File;
import java.util.Iterator;

/**
 * Check for errors in .property files
 * <p>
 * TODO: Warn about bad paths like sdk properties with ' in the path, or suffix of " " etc
 */
public class PropertyFileDetector extends Detector {
    /** Property file not escaped */
    public static final Issue ESCAPE = Issue.create(
            "PropertyEscape", //$NON-NLS-1$
            "Incorrect property escapes",
            "All backslashes and colons in .property files must be escaped with " +
            "a backslash (\\). This means that when writing a Windows path, you " +
            "must escape the file separators, so the path \\My\\Files should be " +
            "written as `key=\\\\My\\\\Files.`",

            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    PropertyFileDetector.class,
                    Scope.PROPERTY_SCOPE));

    /** Using HTTP instead of HTTPS for the wrapper */
    public static final Issue HTTP = Issue.create(
            "UsingHttp", //$NON-NLS-1$
            "Using HTTP instead of HTTPS",
            "The Gradle Wrapper is available both via HTTP and HTTPS. HTTPS is more " +
            "secure since it protects against man-in-the-middle attacks etc. Older " +
            "projects created in Android Studio used HTTP but we now default to HTTPS " +
            "and recommend upgrading existing projects.",

            Category.SECURITY,
            6,
            Severity.WARNING,
            new Implementation(
                    PropertyFileDetector.class,
                    Scope.PROPERTY_SCOPE));

    /** Constructs a new {@link PropertyFileDetector} */
    public PropertyFileDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getPath().endsWith(DOT_PROPERTIES);
    }

    @Override
    public void run(@NonNull Context context) {
        String contents = context.getContents();
        if (contents == null) {
            return;
        }
        int offset = 0;
        Iterator<String> iterator = Splitter.on('\n').split(contents).iterator();
        String line;
        for (; iterator.hasNext(); offset += line.length() + 1) {
            line = iterator.next();
            if (line.startsWith("#") || line.startsWith(" ")) {
                continue;
            }
            if (line.indexOf('\\') == -1 && line.indexOf(':') == -1) {
                continue;
            }
            int valueStart = line.indexOf('=') + 1;
            if (valueStart == 0) {
                continue;
            }
            checkLine(context, contents, line, offset, valueStart);
        }
    }

    private static void checkLine(@NonNull Context context, @NonNull String contents,
            @NonNull String line, int offset, int valueStart) {
        String prefix = "distributionUrl=http\\";
        if (line.startsWith(prefix)) {
            String https = "https" + line.substring(prefix.length() - 1);
            String message = String.format("Replace HTTP with HTTPS for better security; use %1$s",
              https);
            int startOffset = offset + valueStart;
            int endOffset = startOffset + 4; // 4: "http".length()
            Location location = Location.create(context.file, contents, startOffset, endOffset);
            context.report(HTTP, location, message);
        }

        boolean escaped = false;
        boolean hadNonPathEscape = false;
        int errorStart = -1;
        int errorEnd = -1;
        StringBuilder path = new StringBuilder();
        for (int i = valueStart; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
                if (escaped) {
                    path.append(c);
                }
            } else if (c == ':') {
                if (!escaped) {
                    hadNonPathEscape = true;
                    if (errorStart < 0) {
                        errorStart = i;
                    }
                    errorEnd = i;
                } else {
                    escaped = false;
                }
                path.append(c);
            } else {
                if (escaped) {
                    hadNonPathEscape = true;
                    if (errorStart < 0) {
                        errorStart = i;
                    }
                    errorEnd = i;
                }
                escaped = false;
                path.append(c);
            }
        }
        String pathString = path.toString();
        String key = line.substring(0, valueStart);
        if (hadNonPathEscape && key.endsWith(".dir=") || new File(pathString).exists()) {
            String escapedPath = suggestEscapes(line.substring(valueStart, line.length()));

            // NOTE: Keep in sync with {@link #getSuggestedEscape} below
            String message = "Windows file separators (`\\`) and drive letter "
                    + "separators (':') must be escaped (`\\\\`) in property files; use "
                    + escapedPath;
            int startOffset = offset + errorStart;
            int endOffset = offset + errorEnd + 1;
            Location location = Location.create(context.file, contents, startOffset,
                    endOffset);
            context.report(ESCAPE, location, message);
        }
    }

    @NonNull
    static String suggestEscapes(@NonNull String value) {
        value = value.replace("\\:", ":").replace("\\\\", "\\");
        return LintUtils.escapePropertyValue(value);
    }

    /**
     * Returns the escaped string value suggested by the error message which should have
     * been computed by this lint detector.
     *
     * @param message the error message created by this lint detector
     * @param format the format of the error message
     * @return the suggested escaped value
     */
    @Nullable
    public static String getSuggestedEscape(@NonNull String message, @NonNull TextFormat format) {
        return LintUtils.findSubstring(format.toText(message), "; use ", null);
    }
}
