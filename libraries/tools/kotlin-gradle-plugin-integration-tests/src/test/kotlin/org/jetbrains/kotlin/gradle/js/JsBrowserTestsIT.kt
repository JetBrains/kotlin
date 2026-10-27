/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import kotlin.io.path.createParentDirectories
import kotlin.io.path.moveTo
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.jvm.java
import kotlin.test.assertContains
import kotlin.time.Duration.Companion.milliseconds

@JsBrowserGradlePluginTests
class JsBrowserTestsIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun `verify custom custom KotlinJsTest environment variables are used to launch tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                logLevel = LogLevel.DEBUG,
            ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
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

    @GradleTest
    fun `karma should run when kotlin-node-modules are not installed on top-level node_modules`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js().browser {
                        testTask {
                            it.useKarma {
                                useChromeHeadless()
                            }
                        }
                    }
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
            }

            // prepare build/js/
            build("kotlinNpmInstall")
            val rootLevelWebHelpers = projectPath.resolve("build/js/node_modules/kotlin-web-helpers")
            val packageLevelWebHelpers = projectPath.resolve("build/js/packages/empty-test/node_modules/kotlin-web-helpers")
            rootLevelWebHelpers.moveTo(packageLevelWebHelpers)

            // -x kotlinNpmInstall to prevent overwriting
            if (isTeamCityRun) { // Should be fixed by KTI-3326, but only in master branch
                buildAndFail("jsBrowserTest", "-x", "kotlinNpmInstall") {
                    assertTasksFailed(":jsBrowserTest")
                    assertOutputContains("> Errors occurred during launch of browser for testing.")
                    assertOutputDoesNotContain(":jsBrowserTest exited with errors (exit code: 1)")
                }
            } else {
                build("jsBrowserTest", "-x", "kotlinNpmInstall") {
                    assertTasksAreNotInTaskGraph("kotlinNpmInstall")
                    assertTasksExecuted(":jsBrowserTest")
                }
            }
        }
    }

    @GradleTest
    @DisplayName("KT-89521: Wasm test ESM bundle is skipped when there are no test sources")
    fun `wasm test esm bundle is skipped without test sources`(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    wasmJs {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            test {}
                        }
                    }
                }
            }

            build("wasmJsTest") {
                assertTasksNoSource(":wasmJsTestBundleAsEsm")
                assertTasksNoSource(":wasmJsTest")
            }
        }
    }

    @GradleTest
    @DisplayName("KT-89797: Browser test bundles are rebuilt when main code changes")
    fun `browser test bundles are not up-to-date after main code changes`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            test {
                                it.chromium()
                            }
                        }
                    }

                    wasmJs {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            test {
                                it.chromium()
                            }
                        }
                    }

                    sourceSets.commonTest.dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }

            val mainSource = kotlinSourcesDir("webMain").resolve("Main.kt").createParentDirectories()
            mainSource.writeText("fun answer(): Int = 42")
            kotlinSourcesDir("commonTest").resolve("MainTest.kt").createParentDirectories().writeText(
                """
                import kotlin.test.*

                class MainTest {
                    @Test
                    fun answerTest() {
                        assertEquals(42, answer())
                    }
                }
                """.trimIndent()
            )

            val bundleTasks = arrayOf(
                ":compileTestDevelopmentExecutableKotlinJs",
                ":prepareWebpackBundleForKotlinJsTests",
                ":wasmJsTestBundleAsEsm",
            )

            build(*bundleTasks) {
                assertTasksExecuted(*bundleTasks)
            }

            build(*bundleTasks) {
                assertTasksUpToDate(*bundleTasks)
            }

            mainSource.writeText("fun answer(): Int = 24")

            build(*bundleTasks) {
                assertTasksExecuted(*bundleTasks)
            }
        }
    }

    @GradleTest
    fun `smoke js browser test`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            with(test) {
                                chromium()
                            }
                        }
                    }

                    wasmJs {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            test {
                                it.chromium()
                            }
                        }
                    }

                    sourceSets.commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                            // KT-89269: transitively imports @js-joda/core in Wasm browser tests.
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                        }
                    }

                    sourceSets.commonTest.get().compileSource(
                        """
                        import kotlinx.datetime.LocalDate
                        import kotlin.test.*
                        
                        class JsBrowserSmokeTest {
                            @Test
                            fun assertOk() {
                                assertTrue(42 == 42)
                                assertEquals(LocalDate(2026, 1, 2), LocalDate.parse("2026-01-02"))
                            }
                            
                            @Test
                            fun assertFails() {
                                assertTrue(42 == 0)
                            }
                            
                            @Test
                            fun `assert special symbols "🫎"`() {
                                assertNotEquals("🫎", "🎈")
                            }
                        }
                        """.trimIndent()
                    )
                }

                project.tasks.withType(KotlinJsTest::class.java).configureEach {
                    it.filter.setExcludePatterns("JsBrowserSmokeTest.assert special symbols \"🫎\"")
                }
            }

            buildAndFail("jsBrowserTest") {
                assumeFalse(
                    output.contains("error while loading shared libraries: libglib-2.0"),
                    "No libglib-2.0 on the test runner machine"
                )
                assertTasksExecuted(":prepareWebpackBundleForKotlinJsTests")
                assertTasksFailed(":jsBrowserTest")
                assertOutputContains("""Execute JS tests with chromium runner at URL: http.*:prepareWebpackBundleForKotlinJsTests/test.html""".toRegex())
                assertOutputContains("chromium.JsBrowserSmokeTest.assertFails[js, browser, chromium] FAILED")
                assertOutputContains("2 tests completed, 1 failed")
            }

            buildAndFail("wasmJsBrowserTest") {
                assumeFalse(
                    output.contains("error while loading shared libraries: libglib-2.0"),
                    "No libglib-2.0 on the test runner machine"
                )
                assertTasksExecuted(":wasmJsTestBundleAsEsm")
                assertTasksFailed(":wasmJsBrowserTest")
                assertOutputContains("""Execute JS tests with chromium runner at URL: http.*:wasmJsTestBundleAsEsm/test.html""".toRegex())
                assertOutputContains("chromium.JsBrowserSmokeTest.assertFails[wasmJs, browser, chromium] FAILED")
                assertOutputContains("2 tests completed, 1 failed")
            }
        }
    }

    @DisplayName("KT-86958: Unclear error for js test failure on timeout")
    @GradleTest
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
        enabledOnCI = [OS.LINUX, OS.MAC]
    )
    fun `prints clear error message when a test times out`(gradleVersion: GradleVersion) {
        project(
            "empty", gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            with(test) {
                                chromium {
                                    it.timeout.set(1234.milliseconds)
                                }
                            }
                        }
                    }

                    sourceSets.commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }

                    sourceSets.commonTest.get().compileSource(
                        """
                        import kotlin.test.*
                        
                        class JsBrowserTimeoutTest {
                            @Test
                            fun test() {
                                println("hello - sleeping 10 seconds")
                                js(""${'"'}
                                    var end = new Date().getTime() + 10000;
                                    while (new Date().getTime() < end) {
                                        // busy wait
                                    }
                                ""${'"'})
                                println("done sleeping")
                            }
                        }
                        """.trimIndent()
                    )
                }
            }

            buildAndFail("jsBrowserTest") {
                assertTasksFailed(":jsBrowserTest")
                assertOutputContains("chromium.JsBrowserTimeoutTest.test[js, browser, chromium] FAILED")
                assertOutputContains("com.microsoft.playwright.TimeoutError: Timeout 1234ms exceeded")
            }
        }
    }

    @GradleTest
    fun `two subprojects should reuse single http server for tests`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(logLevel = LogLevel.DEBUG)
                .disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            plugins {
                kotlin("multiplatform") apply false
            }

            val subprojectA = project("empty", gradleVersion)
            val subprojectB = project("empty", gradleVersion)

            include(subprojectA, "subprojectA")
            include(subprojectB, "subprojectB")

            for (subproject in listOf(subprojectA, subprojectB)) {
                subproject.plugins {
                    kotlin("multiplatform")
                }
                subproject.buildScriptInjection {
                    project.applyMultiplatform {
                        js {
                            browser {
                                @OptIn(ExperimentalJsTestDsl::class)
                                test {
                                    it.chromium {}
                                }
                            }
                        }
                        sourceSets.commonTest.dependencies {
                            implementation(kotlin("test"))
                        }
                        sourceSets.commonTest.get().compileSource(
                            """
                            import kotlin.test.*
                            
                            class JsBrowserSmokeTest {
                                @Test
                                fun assertOk() {
                                    assertTrue(42 == 42)
                                }
                            }
                            """.trimIndent()
                        )
                    }
                }
            }

            build(
                ":subprojectA:jsBrowserTest",
                ":subprojectB:jsBrowserTest"
            ) {
                assertOutputContainsExactlyTimes("HTTP server for js tests started at", 1)
            }
        }
    }

    @GradleTest
    fun `js browser test execution interruption is logged`(gradleVersion: GradleVersion) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()
        ) {
            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser {
                            @OptIn(ExperimentalJsTestDsl::class)
                            with(test) {
                                chromium()
                                firefox()
                            }
                        }
                    }

                    sourceSets.commonTest {
                        dependencies {
                            implementation(kotlin("test"))
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
                        }
                    }

                    sourceSets.commonTest.get().compileSource(
                        """
                        import kotlinx.coroutines.Dispatchers
                        import kotlinx.coroutines.delay
                        import kotlinx.coroutines.test.runTest
                        import kotlinx.coroutines.withContext
                        import kotlin.test.*
                        import kotlin.time.Duration.Companion.seconds
                        
                        class JsBrowserInterruptionTest {
                            @Test fun t1() { println("Test T1") }
                            
                            @Test
                            fun t2() = runTest {
                                withContext(Dispatchers.Default) {
                                    delay(10.seconds)
                                }
                                println("Test T2")
                            }

                            @Test fun t3() { println("Test T3") }
                        }
                        """.trimIndent()
                    )
                }

                project.tasks.withType(KotlinJsTest::class.java).configureEach { task ->
                    task.doFirst {
                        val testExecutionThread = Thread.currentThread()
                        Thread {
                            Thread.sleep(3000)
                            testExecutionThread.interrupt()
                        }.apply {
                            isDaemon = true
                            start()
                        }
                    }
                }
            }

            buildAndFail("jsBrowserTest") {
                assumeFalse(
                    output.contains("error while loading shared libraries: libglib-2.0"),
                    "No libglib-2.0 on the test runner machine"
                )
                assertTasksFailed(":jsBrowserTest")
                assertOutputContains("Execution of Kotlin/JS browser tests was interrupted for runner 'chromium'.")
                // first test is executed and reported
                assertOutputContains("chromium.JsBrowserInterruptionTest.t1[js, browser, chromium] STANDARD_OUT")

                // second "slow" test got interrupted i.e. failed
                assertOutputContains("chromium.JsBrowserInterruptionTest.t2[js, browser, chromium] FAILED")

                // third test never get executed
                assertOutputDoesNotContain("JsBrowserInterruptionTest.t3")

                // first test never get executed with firefox runner as entire test execution was interrupted
                assertOutputDoesNotContain("JsBrowserInterruptionTest.t1[js, browser, firefox]")
            }
        }
    }
}
