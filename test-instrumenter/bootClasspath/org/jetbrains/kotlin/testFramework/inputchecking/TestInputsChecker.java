/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TestInputsChecker {

    private volatile static TestInputsChecker INSTANCE;

    private final String rootDir;
    private final String buildDir;
    @Nullable private final String internalKlibCacheDir;
    @Nullable private final String internalKlibStdlibCacheDir;
    private final Set<String> declaredInputs;
    private final Set<String> undeclaredInputs;
    private final boolean failFast;

    public static void initialize(
            String rootDir,
            String buildDir,
            @Nullable String internalKlibCacheDir,
            @Nullable String internalKlibStdlibCacheDir,
            Collection<String> declaredInputs,
            boolean failFast
    ) {
        INSTANCE = new TestInputsChecker(rootDir, buildDir, internalKlibCacheDir, internalKlibStdlibCacheDir, declaredInputs, failFast);
    }

    public static TestInputsChecker getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("The UndeclaredInputsGuard instance is not yet available!");
        }
        return INSTANCE;
    }

    private TestInputsChecker(
            String rootDir,
            String buildDir,
            @Nullable String internalKlibCacheDir,
            @Nullable String internalKlibStdlibCacheDir,
            Collection<String> declaredInputs,
            boolean failFast
    ) {
        this.rootDir = rootDir;
        this.buildDir = buildDir;
        this.internalKlibCacheDir = internalKlibCacheDir;
        this.internalKlibStdlibCacheDir = internalKlibStdlibCacheDir;
        this.declaredInputs = Collections.unmodifiableSet(new HashSet<>(declaredInputs));
        this.undeclaredInputs = ConcurrentHashMap.newKeySet();
        this.failFast = failFast;
    }

    public void checkPath(String path) {
        // Short circuit and deduplication
        if (path == null || undeclaredInputs.contains(path)) {
            return;
        }
        // We use File instead of Path because it's more lightweight.
        // Some paths from user code are relative, so we convert them to absolute paths (if not already)
        File file = new File(path).getAbsoluteFile();

        if (!isAllowedInput(file) && !file.isDirectory()) {
            File canonicalFile = convertToCanonicalIfNecessary(file);

            if (canonicalFile.equals(file) || !isAllowedInput(canonicalFile)) {
                if (failFast) {
                    throw new UndeclaredInputException(file.getPath());
                } else {
                    UndeclaredInputEvent.emit(file.toString());
                    undeclaredInputs.add(path);
                }
            }
        }
    }

    /**
     * <p>The file is allowed to be read either because it's inside one
     * of the whitelisted locations, or it's declared as Gradle input.</p>
     *
     * <p>The order of expressions matters here (from the fastest to the slowest).</p>
     */
    private boolean isAllowedInput(File file) {
        return isOutsideRootDir(file) ||
               isInsideBuildDir(file) ||
               isDynamicallyCreatedKlibCache(file) ||
               declaredInputs.contains(file.getPath());
    }

    /**
     * Allow reading files outside the root project directory (like Gradle caches).
     */
    private boolean isOutsideRootDir(File file) {
        return !file.getPath().startsWith(rootDir);
    }

    /**
     * Allow reading files inside the current project's build directory
     * (tests sometimes write files there and then read them back).
     */
    private boolean isInsideBuildDir(File file) {
        return file.getPath().startsWith(buildDir);
    }

    /**
     * <p>Allow reading files inside "kotlin-native/dist/klib/cache" as they are written dynamically during test execution.</p>
     *
     * <p>There is only one exception: the stdlib cache. It is produced by :kotlin-native:distStdlibCache
     * and written into ".../klib/cache/{target}-gSTATIC-system/stdlib-per-file-cache".</p>
     */
    private boolean isDynamicallyCreatedKlibCache(File file) {
        return isKlibCache(file) && !isKlibStdlibCache(file);
    }

    private boolean isKlibCache(File file) {
        return Optional.ofNullable(internalKlibCacheDir)
                .map(it -> file.getPath().startsWith(internalKlibCacheDir))
                .orElse(false);
    }

    private boolean isKlibStdlibCache(File file) {
        return Optional.ofNullable(internalKlibStdlibCacheDir)
                .map(it -> file.getPath().startsWith(it))
                .orElse(false);
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
}
