/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

class KotlinJsIrK1IncrementalGradlePluginIT : AbstractKotlinJsIncrementalGradlePluginIT(irBackend = true) {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK1()
}

class KotlinJsIrK2IncrementalGradlePluginIT : AbstractKotlinJsIncrementalGradlePluginIT(irBackend = true) {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copyEnsuringK2()
}

@JsGradlePluginTests
abstract class AbstractKotlinJsIncrementalGradlePluginIT(
    protected val irBackend: Boolean
) : KGPBaseTest() {
    @Suppress("DEPRECATION")
    private val defaultJsOptions = BuildOptions.JsOptions(
    )

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
        )

    @DisplayName("incremental compilation for js works")
    @GradleTest
    fun testIncrementalCompilation(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        project("kotlin2JsICProject", gradleVersion, buildOptions = buildOptions) {
            build("compileKotlinJs", "compileTestKotlinJs") {
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                assertCompiledKotlinSources(projectPath.allKotlinFiles.relativizeTo(projectPath), output)
            }

            build("compileKotlinJs", "compileTestKotlinJs") {
                assertCompiledKotlinSources(emptyList(), output)
            }

            val modifiedFile = subProject("lib").kotlinSourcesDir().resolve("A.kt") ?: error("No A.kt file in test project")
            modifiedFile.modify {
                it.replace("val x = 0", "val x = \"a\"")
            }
            build("compileKotlinJs", "compileTestKotlinJs") {
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                val affectedFiles =
                    listOf("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt", "useAInLibTest.kt").mapNotNull {
                        projectPath.findInPath(it)
                    }
                assertCompiledKotlinSources(affectedFiles.relativizeTo(projectPath), output)
            }
        }
    }

    @DisplayName("non-incremental compilation works")
    @GradleTest
    fun testIncrementalCompilationDisabled(gradleVersion: GradleVersion) {
        val jsOptions = defaultJsOptions.copy(incrementalJsKlib = false, incrementalJs = false)
        val options = defaultBuildOptions.copy(jsOptions = jsOptions)
        project("kotlin2JsICProject", gradleVersion, buildOptions = options) {
            build("compileKotlinJs", "compileTestKotlinJs") {
                assertOutputDoesNotContain(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            }
        }
    }

    @DisplayName("incremental compilation with multiple js modules after frontend compilation error works")
    @GradleTest
    fun testIncrementalCompilationWithMultipleModulesAfterCompilationError(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(jsOptions = defaultJsOptions.copy(incrementalJsKlib = true))
        project("kotlin-js-ir-ic-multiple-artifacts", gradleVersion, buildOptions = buildOptions) {
            build("compileKotlinJs")

            val libKt = subProject("lib").kotlinSourcesDir().resolve("Lib.kt") ?: error("No Lib.kt file in test project")
            val appKt = subProject("app").kotlinSourcesDir().resolve("App.kt") ?: error("No App.kt file in test project")

            libKt.modify { it.replace("fun answer", "func answe") } // introduce compilation error
            buildAndFail("compileKotlinJs")
            libKt.modify { it.replace("func answe", "fun answer") } // revert compilation error
            appKt.modify { it.replace("Sheldon:", "Sheldon :") } // some change for incremental compilation
            build("compileKotlinJs", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                assertTasksUpToDate(":lib:compileKotlinJs")
                assertTasksExecuted(":app:compileKotlinJs")
                assertCompiledKotlinSources(listOf(appKt).relativizeTo(projectPath), output)
            }
        }
    }
}