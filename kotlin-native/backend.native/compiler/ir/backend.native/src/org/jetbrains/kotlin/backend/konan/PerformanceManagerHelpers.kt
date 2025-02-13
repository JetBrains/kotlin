/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.util.PerformanceManager

class K2NativeCompilerPerformanceManager : PerformanceManager("Kotlin to Native Compiler") {
    companion object {
        fun createAndEnableIfNeeded(mainPerformanceManager: PerformanceManager?): K2NativeCompilerPerformanceManager? {
            return if (mainPerformanceManager != null) {
                K2NativeCompilerPerformanceManager().also {
                    if (mainPerformanceManager.isEnabled) {
                        it.enableCollectingPerformanceStatistics(mainPerformanceManager.isK2)
                    }
                }
            } else {
                null
            }
        }
    }
}
