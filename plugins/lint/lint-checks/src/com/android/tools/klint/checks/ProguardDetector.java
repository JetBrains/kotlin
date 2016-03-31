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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.PROGUARD_CONFIG;
import static com.android.SdkConstants.PROJECT_PROPERTIES;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;

/**
 * Check which looks for errors in Proguard files.
 */
public class ProguardDetector extends Detector {

    private static final Implementation IMPLEMENTATION = new Implementation(ProguardDetector.class,
            Scope.PROGUARD_SCOPE);

    /** The main issue discovered by this detector */
    public static final Issue WRONG_KEEP = Issue.create(
            "Proguard", //$NON-NLS-1$
            "Using obsolete ProGuard configuration",
            "Using `-keepclasseswithmembernames` in a proguard config file is not " +
            "correct; it can cause some symbols to be renamed which should not be.\n" +
            "Earlier versions of ADT used to create proguard.cfg files with the " +
            "wrong format. Instead of `-keepclasseswithmembernames` use " +
            "`-keepclasseswithmembers`, since the old flags also implies " +
            "\"allow shrinking\" which means symbols only referred to from XML and " +
            "not Java (such as possibly CustomViews) can get deleted.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION)
            .addMoreInfo(
                    "http://http://code.google.com/p/android/issues/detail?id=16384"); //$NON-NLS-1$

    /** Finds ProGuard files that contain non-project specific configuration
     * locally and suggests replacing it with an include path */
    public static final Issue SPLIT_CONFIG = Issue.create(
            "ProguardSplit", //$NON-NLS-1$
            "Proguard.cfg file contains generic Android rules",

            "Earlier versions of the Android tools bundled a single `proguard.cfg` file " +
            "containing a ProGuard configuration file suitable for Android shrinking and " +
            "obfuscation. However, that version was copied into new projects, which " +
            "means that it does not continue to get updated as we improve the default " +
            "ProGuard rules for Android.\n" +
            "\n" +
            "In the new version of the tools, we have split the ProGuard configuration " +
            "into two halves:\n" +
            "* A simple configuration file containing only project-specific flags, in " +
            "your project\n" +
            "* A generic configuration file containing the recommended set of ProGuard " +
            "options for Android projects. This generic file lives in the SDK install " +
            "directory which means that it gets updated along with the tools.\n" +
            "\n" +
            "In order for this to work, the proguard.config property in the " +
            "`project.properties` file now refers to a path, so you can reference both " +
            "the generic file as well as your own (and any additional files too).\n" +
            "\n" +
            "To migrate your project to the new setup, create a new `proguard-project.txt` file " +
            "in your project containing any project specific ProGuard flags as well as " +
            "any customizations you have made, then update your project.properties file " +
            "to contain:\n" +
            "`proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:proguard-project.txt`",

            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            IMPLEMENTATION);

    @Override
    public void run(@NonNull Context context) {
        String contents = context.getContents();
        if (contents != null) {
            if (context.isEnabled(WRONG_KEEP)) {
                int index = contents.indexOf(
                        // Old pattern:
                        "-keepclasseswithmembernames class * {\n" + //$NON-NLS-1$
                        "    public <init>(android.");              //$NON-NLS-1$
                if (index != -1) {
                    context.report(WRONG_KEEP,
                            Location.create(context.file, contents, index, index),
                            "Obsolete ProGuard file; use `-keepclasseswithmembers` instead of " +
                            "`-keepclasseswithmembernames`");
                }
            }
            if (context.isEnabled(SPLIT_CONFIG)) {
                int index = contents.indexOf("-keep public class * extends android.app.Activity");
                if (index != -1) {
                    // Only complain if project.properties actually references this file;
                    // no need to bother the users who got a default proguard.cfg file
                    // when they created their projects but haven't actually hooked it up
                    // to shrinking & obfuscation.
                    File propertyFile = new File(context.file.getParentFile(), PROJECT_PROPERTIES);
                    if (!propertyFile.exists()) {
                        return;
                    }
                    String properties = context.getClient().readFile(propertyFile);
                    int i = properties.indexOf(PROGUARD_CONFIG);
                    if (i == -1) {
                        return;
                    }
                    // Make sure the entry isn't just commented out, such as
                    // # To enable ProGuard to shrink and obfuscate your code, uncomment this:
                    // #proguard.config=proguard.cfg
                    for (; i >= 0; i--) {
                        char c = properties.charAt(i);
                        if (c == '#') {
                            return;
                        }
                        if (c == '\n') {
                            break;
                        }
                    }
                    if (properties.contains(PROGUARD_CONFIG)) {
                        context.report(SPLIT_CONFIG,
                            Location.create(context.file, contents, index, index),
                            String.format(
                            "Local ProGuard configuration contains general Android " +
                            "configuration: Inherit these settings instead? " +
                            "Modify `project.properties` to define " +
                            "`proguard.config=${sdk.dir}/tools/proguard/proguard-android.txt:%1$s`" +
                            " and then keep only project-specific configuration here",
                            context.file.getName()));
                    }
                }
            }
        }
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }
}
