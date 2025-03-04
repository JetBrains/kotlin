/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.bootclasspath;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOTE: Intentionally written in Java to avoid `java.lang.NoClassDefFoundError: kotlin/jvm/internal/Intrinsics`
 * when patching java.io.File, which is in the bootclasspath.
 */
@SuppressWarnings("unused")
public class FileAccessMonitorAgent {
    public static final Set<String> accessedFiles = ConcurrentHashMap.newKeySet();

    public static void recordFileAccess(String path) {
        accessedFiles.add(path);
    }

    public static Set<String> getAccessedFiles() {
        return new HashSet<>(accessedFiles);
    }

    public static void reset() {
        accessedFiles.clear();
    }
}
