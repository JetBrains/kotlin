/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers

interface PhaseProfiler {
    fun start()

    fun stop()
}

object DummyPhaseProfiler : PhaseProfiler {
    override fun start() {}
    override fun stop() {}
}

class ActualPhaseProfiler(
    private val activityName: String,
    private val profilerPath: String,
    private val profilerHandler: ProfilerHandler
) :
    PhaseProfiler {
    override fun start() {
        profilerHandler.startProfiling(activityName)
    }

    override fun stop() {
        profilerHandler.stopProfiling(profilerPath, activityName)
    }
}