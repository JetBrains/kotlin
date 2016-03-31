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
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Looks for packaged private key files.
 */
public class PrivateKeyDetector extends Detector implements Detector.OtherFileScanner {
    /** Packaged private key files */
    public static final Issue ISSUE = Issue.create(
            "PackagedPrivateKey", //$NON-NLS-1$
            "Packaged private key",

            "In general, you should not package private key files inside your app.",

            Category.SECURITY,
            8,
            Severity.FATAL,
            new Implementation(PrivateKeyDetector.class, Scope.OTHER_SCOPE));

    /** Constructs a new {@link PrivateKeyDetector} check */
    public PrivateKeyDetector() {
    }

    private static boolean isPrivateKeyFile(File file) {
        if (!file.isFile() ||
            (!LintUtils.endsWith(file.getPath(), "pem") &&  //NON-NLS-1$
             !LintUtils.endsWith(file.getPath(), "key"))) { //NON-NLS-1$
            return false;
        }

        try {
            String firstLine = Files.readFirstLine(file, Charsets.US_ASCII);
            return firstLine != null &&
                firstLine.startsWith("---") &&     //NON-NLS-1$
                firstLine.contains("PRIVATE KEY"); //NON-NLS-1$
        } catch (IOException ex) {
            // Don't care
        }

        return false;
    }

    // ---- Implements OtherFileScanner ----

    @NonNull
    @Override
    public EnumSet<Scope> getApplicableFiles() {
        return Scope.OTHER_SCOPE;
    }

    @Override
    public void run(@NonNull Context context) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        File file = context.file;
        if (isPrivateKeyFile(file)) {
            String fileName = file.getParentFile().getName() + File.separator
                + file.getName();
            String message = String.format(
                "The `%1$s` file seems to be a private key file. " +
                "Please make sure not to embed this in your APK file.", fileName);
            context.report(ISSUE, Location.create(file), message);
        }
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }
}
