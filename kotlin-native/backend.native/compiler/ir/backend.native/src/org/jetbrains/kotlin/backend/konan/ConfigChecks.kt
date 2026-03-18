/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.config.konanExportKdoc
import org.jetbrains.kotlin.konan.config.konanPrintBitcode
import org.jetbrains.kotlin.konan.config.konanPrintFiles
import org.jetbrains.kotlin.konan.config.verifyBitcode

/**
 * Convenient methods to check compilation parameters.
 */
interface ConfigChecks {

    val config: NativeSecondStageCompilationConfig

    fun shouldExportKDoc() = config.configuration.konanExportKdoc

    fun shouldVerifyBitCode() = config.configuration.verifyBitcode

    fun shouldPrintBitCode() = config.configuration.konanPrintBitcode

    fun shouldPrintFiles() = config.configuration.konanPrintFiles

    fun shouldContainDebugInfo() = config.debug

    fun shouldContainLocationDebugInfo() = shouldContainDebugInfo() || config.lightDebug

    fun shouldContainAnyDebugInfo() = shouldContainDebugInfo() || shouldContainLocationDebugInfo()

    fun shouldUseDebugInfoFromNativeLibs() = shouldContainAnyDebugInfo() && config.useDebugInfoInNativeLibs

    fun shouldOptimize() = config.optimizationsEnabled

    fun shouldInlineSafepoints() = config.inlineForPerformance

    fun useLazyFileInitializers() = config.propertyLazyInitialization

}
