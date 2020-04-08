/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.idea.perf.profilers.async.AsyncProfilerHandler
import org.jetbrains.kotlin.idea.testFramework.logMessage

interface ProfilerHandler {
    fun startProfiling(activityName: String, options: List<String> = emptyList())

    fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String> = emptyList())

    companion object {
        private var instance: ProfilerHandler? = null

        @Synchronized
        fun getInstance(): ProfilerHandler {
            if (instance == null) {
                if (!SystemInfo.isWindows) {
                    try {
                        instance = AsyncProfilerHandler()
                    } catch (e: Throwable) {
                        logMessage { "asyncProfiler not found" }
                    }
                }
            }

            if (instance == null) {
                // fallback to dummy
                instance = DummyProfilerHandler
            }
            return instance!!
        }
    }

}

object DummyProfilerHandler : ProfilerHandler {
    override fun startProfiling(activityName: String, options: List<String>) {}

    override fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String>) {}
}