/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.DOT_NATIVE_LIBS;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UnsafeNativeCodeDetector extends Detector implements Detector.UastScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            UnsafeNativeCodeDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue LOAD = Issue.create(
            "UnsafeDynamicallyLoadedCode",
            "`load` used to dynamically load code",
            "Dynamically loading code from locations other than the application's library " +
            "directory or the Android platform's built-in library directories is dangerous, " +
            "as there is an increased risk that the code could have been tampered with. " +
            "Applications should use `loadLibrary` when possible, which provides increased " +
            "assurance that libraries are loaded from one of these safer locations. " +
            "Application developers should use the features of their development " +
            "environment to place application native libraries into the lib directory " +
            "of their compiled APKs.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    public static final Issue UNSAFE_NATIVE_CODE_LOCATION = Issue.create(
            "UnsafeNativeCodeLocation", //$NON-NLS-1$
            "Native code outside library directory",
            "In general, application native code should only be placed in the application's " +
            "library directory, not in other locations such as the res or assets directories. " +
            "Placing the code in the library directory provides increased assurance that the " +
            "code will not be tampered with after application installation. Application " +
            "developers should use the features of their development environment to place " +
            "application native libraries into the lib directory of their compiled " +
            "APKs. Embedding non-shared library native executables into applications should " +
            "be avoided when possible.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final String RUNTIME_CLASS = "java.lang.Runtime"; //$NON-NLS-1$
    private static final String SYSTEM_CLASS = "java.lang.System"; //$NON-NLS-1$

    private static final byte[] ELF_MAGIC_VALUE = { (byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46 };

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Implements Detector.UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        // Identify calls to Runtime.load() and System.load()
        return Collections.singletonList("load");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        // Report calls to Runtime.load() and System.load()
        if ("load".equals(method.getName())) {
            JavaEvaluator evaluator = context.getEvaluator();
            if (evaluator.isMemberInSubClassOf(method, RUNTIME_CLASS, false) ||
                    evaluator.isMemberInSubClassOf(method, SYSTEM_CLASS, false)) {
                context.report(LOAD, call, context.getUastLocation(call),
                        "Dynamically loading code using `load` is risky, please use " +
                                "`loadLibrary` instead when possible");
            }
        }
    }

    // ---- Look for code in resource and asset directories ----

    @Override
    public void afterCheckLibraryProject(@NonNull Context context) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        checkResourceFolders(context, context.getProject());
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        checkResourceFolders(context, context.getProject());
    }

    private static boolean isNativeCode(File file) {
        if (!file.isFile()) {
            return false;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                byte[] bytes = new byte[4];
                int length = fis.read(bytes);
                return (length == 4) && (Arrays.equals(ELF_MAGIC_VALUE, bytes));
            } finally {
                fis.close();
            }
        } catch (IOException ex) {
            return false;
        }
    }

    private static void checkResourceFolders(Context context, @NonNull Project project) {
        if (!context.getScope().contains(Scope.RESOURCE_FOLDER)) {
            // Don't do work when doing in-editor analysis of Java files
            return;
        }
        List<File> resourceFolders = project.getResourceFolders();
        for (File res : resourceFolders) {
            File[] folders = res.listFiles();
            if (folders != null) {
                for (File typeFolder : folders) {
                    if (typeFolder.getName().startsWith(SdkConstants.FD_RES_RAW)) {
                        File[] rawFiles = typeFolder.listFiles();
                        if (rawFiles != null) {
                            for (File rawFile : rawFiles) {
                                checkFile(context, rawFile);
                            }
                        }
                    }
                }
            }
        }

        List<File> assetFolders = project.getAssetFolders();
        for (File assetFolder : assetFolders) {
            File[] assets = assetFolder.listFiles();
            if (assets != null) {
                for (File asset : assets) {
                    checkFile(context, asset);
                }
            }
        }
    }

    private static void checkFile(@NonNull Context context, @NonNull File file) {
        if (isNativeCode(file)) {
            if (LintUtils.endsWith(file.getPath(), DOT_NATIVE_LIBS)) {
                context.report(UNSAFE_NATIVE_CODE_LOCATION, Location.create(file),
                        "Shared libraries should not be placed in the res or assets " +
                        "directories. Please use the features of your development " +
                        "environment to place shared libraries in the lib directory of " +
                        "the compiled APK.");
            } else {
                context.report(UNSAFE_NATIVE_CODE_LOCATION, Location.create(file),
                        "Embedding non-shared library native executables into applications " +
                        "should be avoided when possible, as there is an increased risk that " +
                        "the executables could be tampered with after installation. Instead, " +
                        "native code should be placed in a shared library, and the features of " +
                        "the development environment should be used to place the shared library " +
                        "in the lib directory of the compiled APK.");
            }
        }
    }
}
