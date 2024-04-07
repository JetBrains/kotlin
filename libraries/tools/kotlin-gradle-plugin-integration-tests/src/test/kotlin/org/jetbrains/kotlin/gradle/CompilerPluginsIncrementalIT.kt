/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path

@DisplayName("Compiler plugin incremental compilation")
@OtherGradlePluginTests
abstract class CompilerPluginsIncrementalIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            incremental = true
        )

    @DisabledOnOs(OS.WINDOWS, disabledReason = "Kotlin compiler holds an open file descriptor to plugin jar file")
    @DisplayName("KT-38570: After changing compiler plugin code, next incremental build picks it up")
    @GradleTest
    open fun afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion: GradleVersion) {
        project("incrementalChangeInPlugin".prefix, gradleVersion) {
            val classesDirectory = subProject("library").kotlinClassesDir("main")
            build("assemble") {
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeClass",
                    "public java.lang.String myMethod();"
                )
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeInterface",
                    "public abstract java.lang.String myMethod();"
                )
            }

            subProject("plugin")
                .kotlinSourcesDir()
                .resolve("test/compiler/plugin/MyMethodGenerator.kt")
                .modify {
                    it.replace("\"myMethod\"", "\"myNewMethod\"")
                }

            build("assemble") {
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeClass",
                    "public java.lang.String myNewMethod();"
                )
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeInterface",
                    "public abstract java.lang.String myNewMethod();"
                )
            }
        }
    }

    private val String.prefix get() = "compilerPlugins/$this"
}

class CompilerPluginsK1IncrementalIT : CompilerPluginsIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class CompilerPluginsK2IncrementalIT : CompilerPluginsIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}

@DisplayName("Compiler plugin incremental compilation with disabled precise compilation outputs backup")
abstract class CompilerPluginsIncrementalWithoutPreciseBackupIT : CompilerPluginsIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}

@DisplayName("Compiler plugin incremental compilation with precise compilation outputs backup")
class CompilerPluginsK1IncrementalWithoutPreciseBackupIT : CompilerPluginsIncrementalWithoutPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class CompilerPluginsK2IncrementalWithoutPreciseBackupIT : CompilerPluginsIncrementalWithoutPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}