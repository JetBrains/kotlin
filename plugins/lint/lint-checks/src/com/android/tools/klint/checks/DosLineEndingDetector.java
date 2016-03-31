/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Document;

/**
 * Checks that the line endings in DOS files are consistent
 */
public class DosLineEndingDetector extends LayoutDetector {
    /** Detects mangled DOS line ending documents */
    public static final Issue ISSUE = Issue.create(
            "MangledCRLF", //$NON-NLS-1$
            "Mangled file line endings",

            "On Windows, line endings are typically recorded as carriage return plus " +
            "newline: \\r\\n.\n" +
            "\n" +
            "This detector looks for invalid line endings with repeated carriage return " +
            "characters (without newlines). Previous versions of the ADT plugin could " +
            "accidentally introduce these into the file, and when editing the file, the " +
            "editor could produce confusing visual artifacts.",

            Category.CORRECTNESS,
            2,
            Severity.ERROR,
            new Implementation(
                    DosLineEndingDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo("https://bugs.eclipse.org/bugs/show_bug.cgi?id=375421"); //$NON-NLS-1$

     /** Constructs a new {@link DosLineEndingDetector} */
    public DosLineEndingDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        String contents = context.getContents();
        if (contents == null) {
            return;
        }

        // We could look for *consistency* and complain if you mix \n and \r\n too,
        // but that isn't really a problem (most editors handle it) so let's
        // not complain needlessly.

        char prev =  0;
        for (int i = 0, n = contents.length(); i < n; i++) {
            char c = contents.charAt(i);
            if (c == '\r' && prev == '\r') {
                String message = "Incorrect line ending: found carriage return (`\\r`) without " +
                        "corresponding newline (`\\n`)";

                // Mark the whole line as the error range, since pointing just to the
                // line ending makes the error invisible in IDEs and error reports etc
                // Find the most recent non-blank line
                boolean blankLine = true;
                for (int index = i - 2; index < i; index++) {
                    char d = contents.charAt(index);
                    if (!Character.isWhitespace(d)) {
                        blankLine = false;
                    }
                }

                int lineBegin = i;
                for (int index = i - 2; index >= 0; index--) {
                    char d = contents.charAt(index);
                    if (d == '\n') {
                        lineBegin = index + 1;
                        if (!blankLine) {
                            break;
                        }
                    } else if (!Character.isWhitespace(d)) {
                        blankLine = false;
                    }
                }

                int lineEnd = Math.min(contents.length(), i + 1);
                Location location = Location.create(context.file, contents, lineBegin, lineEnd);
                context.report(ISSUE, document.getDocumentElement(), location, message);
                return;
            }
            prev = c;
        }
    }
}
