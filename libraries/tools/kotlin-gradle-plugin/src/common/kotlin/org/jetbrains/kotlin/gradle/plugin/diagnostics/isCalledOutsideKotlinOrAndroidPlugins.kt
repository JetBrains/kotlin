/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

/**
 * Use this utility in a function that is DIRECTLY exposed to consumers.
 * To determine whether the call is coming from Kotlin or Android Gradle plugins.
 */
internal val isCalledOutsideKotlinOrAndroidPlugins: Boolean
    get() {
        val trace = Thread.currentThread().stackTrace
            // index 0 is 'Thread.getStackTrace' method
            // index 1 is this method
            // index 2 is the caller, that interested in its own callstack i.e.
            // so we drop 3 in total, and then the "callers"
            .drop(3)
            .filter { frame ->
                val className = frame.className

                // Skip frames from reflection and Gradle decorated objects
                !className.startsWith("jdk.internal.reflect.") &&
                        !className.startsWith("java.lang.reflect.") &&
                        !className.contains("_Decorated")
            }

        val firstNonSkippedFrame = trace.firstOrNull() ?: return true
        val className = firstNonSkippedFrame.className

        if (className.startsWith("org.jetbrains.kotlin")) return false
        if (className.startsWith("com.android.build")) return false

        return true
    }