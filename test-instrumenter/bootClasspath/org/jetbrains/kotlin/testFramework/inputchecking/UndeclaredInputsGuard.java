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

import static org.jetbrains.kotlin.testFramework.inputchecking.UndeclaredInputsGuard.DetectorState.*;

public class UndeclaredInputsGuard {

    private volatile static UndeclaredInputsGuard INSTANCE;

    private final String rootDir;
    private final String buildDir;
    @Nullable private final String internalKlibCacheDir;
    @Nullable private final String internalKlibStdlibCacheDir;
    private final Set<String> declaredInputs;
    private final Set<String> undeclaredInputs;

    public static void initialize(
            String rootDir,
            String buildDir,
            @Nullable String internalKlibCacheDir,
            @Nullable String internalKlibStdlibCacheDir,
            Collection<String> declaredInputs
    ) {
        INSTANCE = new UndeclaredInputsGuard(rootDir, buildDir, internalKlibCacheDir, internalKlibStdlibCacheDir, declaredInputs);
    }

    public static UndeclaredInputsGuard getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("The UndeclaredInputsGuard instance is not yet available!");
        }
        return INSTANCE;
    }

    private UndeclaredInputsGuard(
            String rootDir,
            String buildDir,
            @Nullable String internalKlibCacheDir,
            @Nullable String internalKlibStdlibCacheDir,
            Collection<String> declaredInputs
    ) {
        this.rootDir = rootDir;
        this.buildDir = buildDir;
        this.internalKlibCacheDir = internalKlibCacheDir;
        this.internalKlibStdlibCacheDir = internalKlibStdlibCacheDir;
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
        return new Detector(file).detectUndeclaredInput();
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

    private class Detector {

        private final File file;
        private DetectorState state = UNKNOWN;

        private Detector(File file) {
            this.file = file;
        }

        public boolean detectUndeclaredInput() {
            excludeIfOutsideRootDir();
            excludeIfInsideBuildDir();
            excludeIfDynamicallyCreatedKlibCache();
            includeIfNotFoundAmongDeclaredInputs();
            return state == INCLUDED;
        }

        /**
         * Filter out files outside the root project directory (like Gradle caches)
         */
        private void excludeIfOutsideRootDir() {
            if (state == UNKNOWN && !file.getPath().startsWith(rootDir)) {
                state = EXCLUDED;
            }
        }

        /**
         * Filter out files inside the current project's build directory
         * (tests sometimes write files there and then read them back)
         */
        private void excludeIfInsideBuildDir() {
            if (state == UNKNOWN && file.getPath().startsWith(buildDir)) {
                state = EXCLUDED;
            }
        }

        /**
         * <p>Filter out files inside "kotlin-native/dist/klib/cache" as they are written dynamically during test execution.</p>
         *
         * <p>There is only one exception: the stdlib cache. It is produced by :kotlin-native:distStdlibCache
         * and written into ".../klib/cache/{target}-gSTATIC-system/stdlib-per-file-cache".</p>
         */
        private void excludeIfDynamicallyCreatedKlibCache() {
            if (state == UNKNOWN && isKlibCache() && !isKlibStdlibCache()) {
                state = EXCLUDED;
            }
        }

        private boolean isKlibCache() {
            return Optional.ofNullable(internalKlibCacheDir)
                    .map(it -> file.getPath().startsWith(internalKlibCacheDir))
                    .orElse(false);
        }

        private boolean isKlibStdlibCache() {
            return Optional.ofNullable(internalKlibStdlibCacheDir)
                    .map(it -> file.getPath().startsWith(it))
                    .orElse(false);
        }

        private void includeIfNotFoundAmongDeclaredInputs() {
            if (state == UNKNOWN && !declaredInputs.contains(file.getPath())) {
                state = INCLUDED;
            }
        }
    }

    enum DetectorState { INCLUDED, EXCLUDED, UNKNOWN }
}
