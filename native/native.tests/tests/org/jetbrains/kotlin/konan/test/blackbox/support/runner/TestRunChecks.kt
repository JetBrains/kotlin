/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jetbrains.kotlin.utils.yieldIfNotNull
import java.io.File
import kotlin.time.Duration

internal sealed interface TestRunCheck {
    sealed class ExecutionTimeout(val timeout: Duration) : TestRunCheck {
        class ShouldNotExceed(timeout: Duration) : ExecutionTimeout(timeout)
        class ShouldExceed(timeout: Duration) : ExecutionTimeout(timeout)
    }

    sealed class ExitCode : TestRunCheck {
        object AnyNonZero : ExitCode()
        class Expected(val expectedExitCode: Int) : ExitCode()
    }

    enum class Output {
        STDOUT,
        STDERR,

        /**
         * [STDOUT] followed by [STDERR]
         */
        ALL,
    }

    class OutputDataFile(val output: Output = Output.ALL, val file: File) : TestRunCheck

    class OutputMatcher(val output: Output = Output.ALL, val match: (String) -> Boolean): TestRunCheck

    class FileCheckMatcher(val settings: Settings, val testDataFile: File) : TestRunCheck {
        val prefixes: String
            get() {
                val testTarget = settings.get<KotlinNativeTargets>().testTarget
                val checkPrefixes = buildList {
                    add("CHECK")
                    add("CHECK-${testTarget.abiInfoString}")
                    add("CHECK-${testTarget.name.toUpperCaseAsciiOnly()}")
                    if (testTarget.family.isAppleFamily) {
                        add("CHECK-APPLE")
                    }
                    if (testTarget.needSmallBinary()) {
                        add("CHECK-SMALLBINARY")
                    } else {
                        add("CHECK-BIGBINARY")
                    }
                }
                val optimizationMode = settings.get<OptimizationMode>().name
                val checkPrefixesWithOptMode = checkPrefixes.map { "$it-$optimizationMode" }
                val cacheMode = settings.get<CacheMode>().alias
                val checkPrefixesWithCacheMode = checkPrefixes.map { "$it-CACHE_$cacheMode" }
                return (checkPrefixes + checkPrefixesWithOptMode + checkPrefixesWithCacheMode).joinToString(",")
            }
    }
}

internal data class TestRunChecks(
    val executionTimeoutCheck: ExecutionTimeout,
    private val exitCodeCheck: ExitCode?,
    val outputDataFile: OutputDataFile?,
    val outputMatcher: OutputMatcher?,
    val fileCheckMatcher: FileCheckMatcher?,
) : Iterable<TestRunCheck> {

    override fun iterator() = iterator {
        yield(executionTimeoutCheck)
        yieldIfNotNull(exitCodeCheck)
        yieldIfNotNull(outputDataFile)
        yieldIfNotNull(outputMatcher)
        yieldIfNotNull(fileCheckMatcher)
    }

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(timeout),
            exitCodeCheck = ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
    }
}

// Shameless borrowing `val KonanTarget.abiInfo` from module `:kotlin-native:backend.native`, which cannot be imported here for now.
private val KonanTarget.abiInfoString: String
    get() = when {
        this == KonanTarget.MINGW_X64 -> "WINDOWSX64"
        !family.isAppleFamily && architecture == Architecture.ARM64 -> "AAPCS"
        else -> "DEFAULTABI"
    }
