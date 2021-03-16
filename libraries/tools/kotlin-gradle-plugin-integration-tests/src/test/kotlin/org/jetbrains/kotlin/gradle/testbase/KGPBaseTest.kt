/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Base class for all Kotlin Gradle plugin integration tests.
 */
abstract class KGPBaseTest {
    open val defaultBuildOptions = BuildOptions()

    companion object {
        @TempDir
        @JvmStatic
        lateinit var workingDir: Path
    }

    data class BuildOptions(
        val logLevel: LogLevel = LogLevel.INFO,
        val kotlinVersion: String = KOTLIN_VERSION,
        val warningMode: WarningMode = WarningMode.Fail
    ) {
        fun toArguments(): List<String> {
            val arguments = mutableListOf<String>()
            when (logLevel) {
                LogLevel.DEBUG -> arguments.add("--debug")
                LogLevel.INFO -> arguments.add("--info")
                LogLevel.WARN -> arguments.add("--warn")
                LogLevel.QUIET -> arguments.add("--quiet")
                else -> Unit
            }
            arguments.add("-Pkotlin_version=$kotlinVersion")
            when (warningMode) {
                WarningMode.Fail -> arguments.add("--warning-mode=fail")
                WarningMode.All -> arguments.add("--warning-mode=all")
                WarningMode.Summary -> arguments.add("--warning-mode=summary")
                WarningMode.None -> arguments.add("--warning-mode=none")
            }

            return arguments.toList()
        }
    }
}