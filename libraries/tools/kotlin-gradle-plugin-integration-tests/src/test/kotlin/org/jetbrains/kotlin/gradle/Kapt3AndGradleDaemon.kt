/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName

@DisplayName("Kapt caching inside Gradle daemon")
@DaemonsGradlePluginTests
class Kapt3AndGradleDaemon : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions
        .copy(
            kaptOptions = BuildOptions.KaptOptions(
                verbose = true,
                useWorkers = true,
                includeCompileClasspath = false
            )
        )

    @AfterEach
    internal fun tearDown() {
        // Stops Gradle and Kotlin daemon, so new run will pick up new jvm arguments
        ConnectorServices.reset()
    }

    @DisplayName("Javac should be loaded only once")
    @GradleTest
    fun testJavacIsLoadedOnce(gradleVersion: GradleVersion) {
        project("javacIsLoadedOnce".withPrefix, gradleVersion) {
            build("assemble") {
                val loadsCount = "Loaded com.sun.tools.javac.util.Context from"
                    .toRegex(RegexOption.LITERAL)
                    .findAll(output)
                    .count()

                assert(loadsCount == 1) {
                    """
                    |${printBuildOutput()}
                    |
                    | 'javac' is loaded more than once
                    """.trimMargin()
                }
            }
        }
    }

    @DisplayName("Annotation processor class should be loaded only once")
    @GradleTest
    fun testAnnotationProcessorClassIsLoadedOnce(gradleVersion: GradleVersion) {
        project(
            "javacIsLoadedOnce".withPrefix,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kaptOptions = defaultBuildOptions.kaptOptions!!.copy(
                    classLoadersCacheSize = 10
                )
            )
        ) {
            val loadPattern = ("Loaded example.ExampleAnnotationProcessor from").toRegex(RegexOption.LITERAL)
            fun BuildResult.classLoadingCount() = loadPattern.findAll(output).count()

            build("build") {
                assertTasksExecuted(":module1:kaptKotlin", ":module2:kaptKotlin")
                assert(classLoadingCount() == 1) {
                    """
                    |${printBuildOutput()}
                    |
                    |AP class is loaded not once: ${classLoadingCount()} times.
                    """.trimMargin()
                }
            }

            listOf(
                subProject("module1").kotlinSourcesDir().resolve("module1/Module1Class.kt"),
                subProject("module2").kotlinSourcesDir().resolve("module2/Module2Class.kt")
            ).forEach {
                it.append("\n fun touch() = null")
            }

            build("build") {
                assertTasksExecuted(":module1:kaptKotlin", ":module2:kaptKotlin")
                assert(classLoadingCount() == 0) {
                    """
                    | ${printBuildOutput()}
                    |
                    |AP class shouldn't be loaded on the second build, actually loaded ${classLoadingCount()} times.
                    |
                    """.trimMargin()
                }
            }
        }
    }

    private val String.withPrefix get() = "kapt2/$this"
}