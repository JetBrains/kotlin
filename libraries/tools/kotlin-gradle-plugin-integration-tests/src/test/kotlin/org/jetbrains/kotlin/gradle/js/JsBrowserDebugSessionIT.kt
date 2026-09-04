/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class, ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSession
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.util.assertDoesNotThrow
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@JsBrowserGradlePluginTests
class JsBrowserDebugSessionIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy().disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun `verify the debug session handshake with the IDE`(gradleVersion: GradleVersion) {
        IdeaKotlinJsBrowserDebugSession.startForIde().use { ide ->
            val gradleProcess = thread {
                project(
                    "empty",
                    gradleVersion = gradleVersion,
                    buildOptions = defaultBuildOptions,
                ) {
                    jsProjectWithDummyTest { chromium() }

                    // Start test with debug session
                    build(":jsBrowserTest", ide.asGradleProperty()) {
                        assertTasksExecuted(":jsBrowserTest")
                    }
                }
            }

            // 5 minutes in case if CI agent is way too slow
            val debuggableBrowser = ide.awaitBrowser(5.minutes)
            // simulate that IDE attaches and configures debugger
            probeCdpVersion(debuggableBrowser.cdpUrl)
            ide.sendDebuggerReady(debuggableBrowser)
            // let build system to finish test execution
            ide.awaitFinished(10.seconds)

            gradleProcess.join(5.seconds.inWholeMilliseconds)
        }
    }

    @GradleTest
    fun `test abort command from ide`(gradleVersion: GradleVersion) {
        IdeaKotlinJsBrowserDebugSession.startForIde().use { ide ->
            val gradleProcess = thread {
                project(
                    "empty",
                    gradleVersion = gradleVersion,
                    buildOptions = defaultBuildOptions,
                ) {
                    jsProjectWithDummyTest { chromium() }

                    // Start test with debug session
                    build(":jsBrowserTest", ide.asGradleProperty()) {
                        assertTasksFailed(":jsBrowserTest")
                        assertOutputContains("Kotlin/JS browser debug session was aborted by the IDE: Stop tests")
                    }
                }
            }

            // 5 minutes in case if CI agent is way too slow
            val debuggableBrowser = ide.awaitBrowser(5.minutes)
            // simulate that IDE attaches and configures debugger
            probeCdpVersion(debuggableBrowser.cdpUrl)
            ide.sendDebuggerReady(debuggableBrowser)
            ide.abort("Stop tests")

            gradleProcess.join(5.seconds.inWholeMilliseconds)
        }
    }
}

private fun IdeaKotlinJsBrowserDebugSession.asGradleProperty() =
    "-P${PropertiesProvider.PropertyNames.KOTLIN_JS_IDE_DEBUG_SESSION_URL}=$connectionUrl"

private fun probeCdpVersion(cdpUrl: String) {
    val code = assertDoesNotThrow {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$cdpUrl/json/version").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            connection.responseCode
        } finally {
            connection?.disconnect()
        }
    }
    assert(code == 200) { "CDP version probe failed: $code" }
}

private fun TestProject.jsProjectWithDummyTest(
    testSource: String? = null,
    testConfigure: KotlinJsBrowserTestDsl.() -> Unit,
) {
    addKgpToBuildScriptCompilationClasspath()
    buildScriptInjection {
        project.applyMultiplatform {
            js().browser {
                test.apply {
                    testConfigure()
                }
            }
            sourceSets.commonTest.dependencies {
                implementation(kotlin("test"))
            }
        }

        project.projectDir.resolve("src/jsTest/kotlin/DummyTest.kt").apply {
            parentFile.mkdirs()
            val source = testSource ?: """
                class DummyTest {
                  @kotlin.test.Test
                  fun dummy() {
                    println("dummy test")
                  }
                }
            """.trimIndent()
            writeText(source)
        }
    }
}
