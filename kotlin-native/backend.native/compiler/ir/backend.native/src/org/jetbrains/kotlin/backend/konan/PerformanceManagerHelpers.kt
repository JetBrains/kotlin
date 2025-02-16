/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.PerformanceManager

class K2NativeCompilerPerformanceManager : PerformanceManager("Kotlin to Native Compiler") {

    private val children = mutableListOf<ChildCompilerPerformanceManager>()

    /**
     * Creates a separate performance manager. Use case: create a child, use in a separate thread, collect back the measurements.
     * Measurements may be collected back by [collectChildMeasurements].
     *
     * WARNING: not thread-safe!
     */
    internal fun createChild(): PerformanceManager {
        return ChildCompilerPerformanceManager().also {
            children += it
        }
    }

    /**
     * Collects the measurements from the "child" performance managers.
     *
     * WARNING: not thread-safe!
     */
    fun collectChildMeasurements() {
        children.forEach {
            addMeasurementResults(it.getMeasurementResults())
            it.clearMeasurementResults()
        }
    }

    private inner class ChildCompilerPerformanceManager : PerformanceManager("Kotlin to Native Compiler per single thread")
}

var CompilerConfiguration.performanceManager: PerformanceManager?
    get() = this[CLIConfigurationKeys.PERF_MANAGER]
    set(v) {
        this.putIfNotNull(CLIConfigurationKeys.PERF_MANAGER, v)
    }

internal inline fun <T> PerformanceManager?.trackAnalysis(fn: () -> T): T {
    this?.notifyAnalysisStarted()
    try {
        return fn()
    } finally {
        this?.notifyAnalysisFinished()
    }
}

internal inline fun <T> PerformanceManager?.trackIRTranslation(fn: () -> T): T {
    this?.notifyIRTranslationStarted()
    try {
        return fn()
    } finally {
        this?.notifyIRTranslationFinished()
    }
}

internal inline fun <T> PerformanceManager?.trackGeneration(fn: () -> T): T {
    this?.notifyGenerationStarted()
    try {
        return fn()
    } finally {
        this?.notifyGenerationFinished()
    }
}

internal inline fun <T> PerformanceManager?.trackIRLowering(fn: () -> T): T {
    this?.notifyIRLoweringStarted()
    try {
        return fn()
    } finally {
        this?.notifyIRLoweringFinished()
    }
}