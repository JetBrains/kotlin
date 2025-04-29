/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import kotlin.test.assertContains

@JsGradlePluginTests
class JsBrowserTestsIT : KGPBaseTest() {

    @GradleTest
    fun `verify custom custom KotlinJsTest environment variables are used to launch tests`(gradleVersion: GradleVersion) {
        project(
            "buildScriptInjection",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
            )
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js().browser()
                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }

                project.projectDir.resolve("src/jsTest/kotlin/DummyTest.kt").apply {
                    parentFile.mkdirs()
                    writeText(
                        """
                        class DummyTest {
                          @kotlin.test.Test
                          fun dummy() {
                            println("dummy test")
                          }
                        }
                        """.trimIndent()
                    )
                }

                project.tasks.withType(KotlinJsTest::class.java).configureEach { task ->
                    task.environment("CUSTOM_ENV", "custom-env-value")

                    // KT-77134 verify doFirst {} workaround,
                    // which is necessary because KotlinJsTest doesn't use Provider API.
                    val lazyValue = project.provider { "lazy-custom-env-value" }
                    task.doFirst { _ ->
                        task.environment("CUSTOM_ENV_LAZY", lazyValue.get())
                    }
                }
            }

            build(
                ":jsBrowserTest",
                // :jsBrowserTest might fail if no browsers are installed (e.g. on CI).
                // For this test we don't care if the task passes or fails, only if the custom environment variables are set correctly.
                // So, use `GradleRunner.run()` to ignore the build outcome.
                gradleRunnerAction = GradleRunner::run,
            ) {
                val execAsyncHandleLogs = output.lineSequence()
                    .mapNotNull {
                        it
                            .substringAfter(" [DEBUG] [org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle] ", "")
                            .ifBlank { null }
                    }

                val createdExecSpecLog = execAsyncHandleLogs
                    .singleOrNull { it.startsWith("[ExecAsyncHandle :jsBrowserTest] created ExecSpec.") }

                requireNotNull(createdExecSpecLog) {
                    "Could not find 'created ExecSpec' log in build output:\n${execAsyncHandleLogs.joinToString("\n").prependIndent()}"
                }

                val env = createdExecSpecLog.substringAfter("Environment: {").substringBefore("},")
                assertContains(env, "CUSTOM_ENV=custom-env-value")
                assertContains(env, "CUSTOM_ENV_LAZY=lazy-custom-env-value")
            }
        }
    }
}
