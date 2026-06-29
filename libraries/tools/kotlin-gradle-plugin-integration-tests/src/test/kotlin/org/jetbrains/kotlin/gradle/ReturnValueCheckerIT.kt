/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.writeText

@JvmGradlePluginTests
@DisplayName("Return value checker DSL")
class ReturnValueCheckerIT : KGPBaseTest() {

    private val unusedReturnValueWarning = "Unused return value"

    @DisplayName("returnValueChecker() applies check mode and reports unused return values in both production and test sources")
    @GradleTest
    fun returnValueCheckerDefault(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            addUnusedReturnValueSources()
            // The zero-argument overload is callable from the Groovy DSL (Kotlin default parameters are not).
            buildGradle.appendText(
                """
                |
                |kotlin.returnValueChecker()
                """.trimMargin()
            )

            build("compileKotlin", "compileTestKotlin") {
                assertCompilerArgument(":compileKotlin", "-Xreturn-value-checker=check")
                assertCompilerArgument(":compileTestKotlin", "-Xreturn-value-checker=check")
                assertTaskOutputContains(":compileKotlin", unusedReturnValueWarning)
                assertTaskOutputContains(":compileTestKotlin", unusedReturnValueWarning)
            }
        }
    }

    @DisplayName("returnValueChecker(Full) applies full mode and reports unused return values in both production and test sources")
    @GradleTest
    fun returnValueCheckerFull(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            addUnusedReturnValueSources()
            buildGradle.appendText(
                """
                |
                |import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
                |kotlin.returnValueChecker(ReturnValueCheckerMode.Full, ReturnValueCheckerMode.Full)
                """.trimMargin()
            )

            build("compileKotlin", "compileTestKotlin") {
                assertCompilerArgument(":compileKotlin", "-Xreturn-value-checker=full")
                assertCompilerArgument(":compileTestKotlin", "-Xreturn-value-checker=full")
                assertTaskOutputContains(":compileKotlin", unusedReturnValueWarning)
                assertTaskOutputContains(":compileTestKotlin", unusedReturnValueWarning)
            }
        }
    }

    @DisplayName("returnValueChecker with a disabled test mode reports unused return values only in production sources")
    @GradleTest
    fun returnValueCheckerDisabledForTests(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            addUnusedReturnValueSources()
            buildGradle.appendText(
                """
                |
                |import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
                |kotlin.returnValueChecker(ReturnValueCheckerMode.Check, ReturnValueCheckerMode.Disabled)
                """.trimMargin()
            )

            build("compileKotlin", "compileTestKotlin") {
                assertCompilerArgument(":compileKotlin", "-Xreturn-value-checker=check")
                // The production mode must not leak into tests. 'disable' equals the compiler default and is therefore
                // omitted by the argument serializer, so no -Xreturn-value-checker argument is present at all.
                assertNoCompilerArgument(":compileTestKotlin", "-Xreturn-value-checker=check")
                assertNoCompilerArgument(":compileTestKotlin", "-Xreturn-value-checker=disable")
                assertTaskOutputContains(":compileKotlin", unusedReturnValueWarning)
                assertTaskOutputDoesNotContain(":compileTestKotlin", unusedReturnValueWarning)
            }
        }
    }

    @DisplayName("returnValueChecker configuration is compatible with the configuration cache")
    @GradleTest
    fun returnValueCheckerConfigurationCache(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |kotlin.returnValueChecker()
                """.trimMargin()
            )

            // Stores the configuration cache, then reruns to verify it is reused (the mode is a plain extension
            // property captured by value at configuration time, so it must be configuration-cache compatible).
            assertSimpleConfigurationCacheScenarioWorks(
                ":compileKotlin",
                buildOptions = defaultBuildOptions,
            )
        }
    }

    /**
     * Adds a `@MustUseReturnValues`-annotated production declaration whose return value is ignored in both production and
     * test sources. Such a usage is reported by the return value checker in both `check` and `full` modes, so it lets the
     * test observe the actual `Unused return value` compiler warning.
     */
    private fun TestProject.addUnusedReturnValueSources() {
        kotlinSourcesDir("main").resolve("producer.kt").writeText(
            """
            |@MustUseReturnValues
            |class Producer {
            |    fun produce(): String = "result"
            |}
            |
            |fun ignoreInMain() {
            |    Producer().produce()
            |}
            """.trimMargin()
        )
        kotlinSourcesDir("test").resolve("consumer.kt").writeText(
            """
            |fun ignoreInTest() {
            |    Producer().produce()
            |}
            """.trimMargin()
        )
    }
}
