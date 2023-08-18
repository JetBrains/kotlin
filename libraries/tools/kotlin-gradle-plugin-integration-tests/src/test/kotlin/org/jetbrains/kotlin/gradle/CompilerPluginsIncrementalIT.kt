/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.exists

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
            build("assemble")

            val genDir = subProject("library").projectPath.resolve("build/sample-dir/plugin/test/gen")
            assert(genDir.exists()) { "$genDir does not exists!" }

            val generatedFiles = genDir.toFile()
                .walkTopDown()
                .asSequence()
                .filterNot { it.isDirectory }
                .associate {
                    it.absolutePath to it.readBytes()
                }

            subProject("plugin")
                .kotlinSourcesDir()
                .resolve("test/compiler/plugin/TestComponentRegistrar.kt")
                .modify {
                    it.replace("world.", "something else.")
                }

            build("assemble") {
                assert(genDir.exists()) { "$genDir does not exists!" }
            }

            genDir.toFile()
                .walkTopDown()
                .asSequence()
                .filterNot { it.isDirectory }
                .forEach {
                    assert(!it.readBytes().contentEquals(generatedFiles[it.absolutePath])) {
                        """
                        
                        File ${it.absolutePath} content is equals to previous one!
                        New content:
                        ${it.readText()}
                        """.trimIndent()
                    }
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

    @Disabled("KT-61171")
    override fun afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion: GradleVersion) {
        super.afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion)
    }
}

@DisplayName("Compiler plugin incremental compilation with precise compilation outputs backup")
abstract class CompilerPluginsIncrementalWithPreciseBackupIT : CompilerPluginsIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}

@DisplayName("Compiler plugin incremental compilation with precise compilation outputs backup")
class CompilerPluginsK1IncrementalWithPreciseBackupIT : CompilerPluginsIncrementalWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

class CompilerPluginsK2IncrementalWithPreciseBackupIT : CompilerPluginsIncrementalWithPreciseBackupIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    @Disabled("KT-61171")
    override fun afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion: GradleVersion) {
        super.afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion)
    }
}