/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UndeclaredInputsGuard {

    private volatile static UndeclaredInputsGuard INSTANCE;

    private final String rootDir;
    private final String buildDir;
    private final Set<String> declaredInputs;
    private final Set<String> undeclaredInputs;

    public static void initialize(String rootDir, String buildDir, Collection<String> declaredInputs) {
        INSTANCE = new UndeclaredInputsGuard(rootDir, buildDir, declaredInputs);
    }

    public static UndeclaredInputsGuard getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("The UndeclaredInputsGuard instance is not yet available!");
        }
        return INSTANCE;
    }

    private UndeclaredInputsGuard(String rootDir, String buildDir, Collection<String> declaredInputs) {
        this.rootDir = rootDir;
        this.buildDir = buildDir;
        this.declaredInputs = Collections.unmodifiableSet(new HashSet<>(declaredInputs));
        this.undeclaredInputs = ConcurrentHashMap.newKeySet();
    }

    public void checkPath(String path) {
        // Short circuit and deduplication
        if (path == null || undeclaredInputs.contains(path)) {
            return;
        }
        // We use File instead of Path because it's more lightweight.
        // Some paths from user code are relative, so we convert them to absolute paths (if not already)
        File file = new File(path).getAbsoluteFile();

        if (isUndeclaredInput(file) && !file.isDirectory()) {
            File canonicalFile = convertToCanonicalIfNecessary(file);

            if (canonicalFile.equals(file) || isUndeclaredInput(canonicalFile)) {
                UndeclaredInputEvent.emit(file.toString());
                undeclaredInputs.add(path);
            }
        }
    }

    private boolean isUndeclaredInput(File file) {
        // The order of expressions matters here, the Set::contains is the most expensive operation
        return insideRootProjectDir(file) &&
               notInsideCurrentProjectBuildDir(file) &&
               !declaredInputs.contains(file.getPath());
    }

    /**
     * Filter out files outside the root project directory (like Gradle caches)
     */
    private boolean insideRootProjectDir(File file) {
        return file.getPath().startsWith(rootDir);
    }

    /**
     * Filter out files inside the current project's build directory
     * (tests sometimes write files there and then read them back)
     */
    private boolean notInsideCurrentProjectBuildDir(File file) {
        return !file.getPath().startsWith(buildDir);
    }

    /**
     * Convert paths like "/foo/../bar" to "/bar".
     * This is an expensive operation, so we try to do it as rarely as possible.
     */
    private static File convertToCanonicalIfNecessary(File file) {
        if (file.getPath().contains(".") || file.getPath().contains("..")) {
            try {
                return file.getCanonicalFile();
            }
            catch (IOException e) {
                throw new RuntimeException("Unable to get canonical path for: " + file.getPath(), e);
            }
        }
        return file;
    }

    public Set<String> getUndeclaredInputs() {
        return Collections.unmodifiableSet(undeclaredInputs);
    }
}
