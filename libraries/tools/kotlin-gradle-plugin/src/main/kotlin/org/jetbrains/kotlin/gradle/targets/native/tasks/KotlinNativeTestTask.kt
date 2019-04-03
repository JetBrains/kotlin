/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.DefaultProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.native.internal.parseKotlinNativeStackTraceAsJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.io.File

open class KotlinNativeTest : KotlinTest() {
    @Suppress("LeakingThis")
    @Internal
    val processOptions: ProcessForkOptions = DefaultProcessForkOptions(fileResolver)

    var executable: File
        @InputFile
        @SkipWhenEmpty
        get() = File(processOptions.executable)
        set(value) {
            processOptions.executable = value.absolutePath
        }

    var workingDir: String
        @Input get() = processOptions.workingDir.canonicalPath
        set(value) {
            processOptions.workingDir = File(value)
        }

    @Suppress("unused")
    fun processOptions(options: ProcessForkOptions.() -> Unit) {
        options(processOptions)
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val extendedForkOptions = DefaultProcessForkOptions(fileResolver)
        processOptions.copyTo(extendedForkOptions)

        val clientSettings = TCServiceMessagesClientSettings(
            name,
            testNameSuffix = targetName,
            prependSuiteName = targetName != null,
            treatFailedTestOutputAsStacktrace = true,
            stackTraceParser = ::parseKotlinNativeStackTraceAsJvm
        )

        val cliArgs = CliArgs("TEAMCITY", includePatterns, excludePatterns)

        return TCServiceMessagesTestExecutionSpec(
            extendedForkOptions,
            cliArgs.toList(),
            false,
            clientSettings
        )
    }

    private class CliArgs(
        val testLogger: String? = null,
        val testGradleFilter: Set<String> = setOf(),
        val testNegativeGradleFilter: Set<String> = setOf()
    ) {
        fun toList() = mutableListOf<String>().also {
            if (testLogger != null) {
                it.add("--ktest_logger=$testLogger")
            }

            if (testGradleFilter.isNotEmpty()) {
                it.add("--ktest_gradle_filter=${testGradleFilter.joinToString(",")}")
            }

            if (testNegativeGradleFilter.isNotEmpty()) {
                it.add("--ktest_negative_gradle_filter=${testNegativeGradleFilter.joinToString(",")}")
            }
        }
    }
}
