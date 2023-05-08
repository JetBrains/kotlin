/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*
import kotlin.test.*

@JsGradlePluginTests
open class KotlinJsIr10IncrementalGradlePluginIT : AbstractKotlinJsIncrementalGradlePluginIT(
    irBackend = true
)

@JsGradlePluginTests
class KotlinJsFirIncrementalGradlePluginIT : KotlinJsIr10IncrementalGradlePluginIT() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            languageVersion = "2.0"
        )
}

@JsGradlePluginTests
open class KotlinJsLegacy10IncrementalGradlePluginIT : AbstractKotlinJsIncrementalGradlePluginIT(
    irBackend = false
)

@JsGradlePluginTests
abstract class AbstractKotlinJsIncrementalGradlePluginIT(
    protected val irBackend: Boolean
) : KGPBaseTest() {
    @Suppress("DEPRECATION")
    private val defaultJsOptions = BuildOptions.JsOptions(
        useIrBackend = irBackend,
        jsCompilerType = if (irBackend) KotlinJsCompilerType.IR else KotlinJsCompilerType.LEGACY
    )

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
        )

    protected fun BuildResult.checkIrCompilationMessage() {
        if (irBackend) {
            assertOutputContains(USING_JS_IR_BACKEND_MESSAGE)
        } else {
            assertOutputDoesNotContain(USING_JS_IR_BACKEND_MESSAGE)
        }
    }

    @DisplayName("incremental compilation for js works")
    @GradleTest
    fun testIncrementalCompilation(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        project("kotlin2JsICProject", gradleVersion, buildOptions = buildOptions) {
            build("compileKotlinJs", "compileTestKotlinJs") {
                checkIrCompilationMessage()
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
                checkIrCompilationMessage()
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                val affectedFiles = listOf("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt").mapNotNull {
                    projectPath.findInPath(it)
                }
                assertCompiledKotlinSources(affectedFiles.relativizeTo(projectPath), output)
            }
        }
    }

    @DisplayName("non-incremental compilation works")
    @GradleTest
    fun testIncrementalCompilationDisabled(gradleVersion: GradleVersion) {
        val jsOptions = defaultJsOptions.run {
            if (irBackend) copy(incrementalJsKlib = false) else copy(incrementalJs = false)
        }
        val options = defaultBuildOptions.copy(jsOptions = jsOptions)
        project("kotlin2JsICProject", gradleVersion, buildOptions = options) {
            build("compileKotlinJs", "compileTestKotlinJs") {
                checkIrCompilationMessage()
                assertOutputDoesNotContain(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            }
        }
    }

    @DisplayName("incremental compilation with multiple js modules after frontend compilation error works")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_6)
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