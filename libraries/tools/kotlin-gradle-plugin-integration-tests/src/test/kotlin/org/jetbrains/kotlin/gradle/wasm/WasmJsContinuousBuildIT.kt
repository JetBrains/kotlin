/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.wasm

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.js.AbstractWebContinuousBuildIT
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmDevServerTaskImpl.Companion.DEFAULT_PORT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.Timeout
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

@MustRunOnChangesInGradle
class WasmJsContinuousBuildIT : AbstractWebContinuousBuildIT() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            // Continuous build requires file watching is enabled.
            fileSystemWatchEnabled = true,
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsRunContinuousBuildWithoutBundler(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }
                }
            }

            projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                .also {
                    it.createParentDirectories()
                }
                .writeText(
                    """
                    fun main() {
                        println("Hello, world")
                    }
                    """.trimIndent()
                )

            val compiledWasm =
                projectPath.resolve("build/compileSync/wasmJs/main/developmentExecutable/kotlin/wasm-browser-simple-project.wasm")

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testWasmJsRunContinuousBuild checker", isDaemon = true) {
                try {
                    val buildStartMark = TimeSource.Monotonic.markNow()
                    fun checkBuildDuration() {
                        check(buildStartMark.elapsedNow() < 10.minutes) {
                            "build took too long - ${buildStartMark.elapsedNow()}"
                        }
                    }

                    println("Waiting for the first compilation to succeed...")
                    while (!compiledWasm.exists()) {
                        Thread.sleep(1000)
                        checkBuildDuration()
                    }
                    println("First compilation completed.")

                    println("Waiting before file modification...")
                    // wait to give Gradle a chance to catch up with file events
                    Thread.sleep(5000)

                    // modify a file to trigger a re-build
                    projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                        .replaceText("Hello, world", "Hello again!!!")
                    println("Modified main.kt")

                    println("Waiting for the second compilation to succeed...")
                    // The string should be presented in binary wasm file
                    while ("Hello again!!!" !in compiledWasm.readText()) {
                        Thread.sleep(1000)
                        checkBuildDuration()
                    }
                    println("Second compilation completed")
                } catch (t: Throwable) {
                    println("Exception in ${Thread.currentThread().name}:\n${t.stackTraceToString()}")
                    throw t
                } finally {
                    println("Releasing daemon stdin stream...")
                    // close the stream, which will allow Gradle to finish the build
                    daemonRelease.close()
                    println("Released daemon stdin stream.")
                }
            }

            build(
                "wasmJsBrowserDevelopmentRun",
                buildOptions = defaultBuildOptions.copy(
                    verboseVfsLogging = true,
                    continuousBuild = true,
                ),
                inputStream = daemonStdin,
                forwardBuildOutput = true,
            ) {
                checker.join()

                assertFileContains(
                    compiledWasm,
                    """Hello again!!!""".trimMargin()
                )

                // verify yarn dependency resolution can run
                assertTasksExecuted(":kotlinWasmStoreYarnLock")
                assertTasksExecuted(":wasmJsGenerateImportMapServer")

                // verify there's no error in the ExecAsyncHandle thread management
                assertOutputDoesNotContain("Exception in thread")

                assertOutputContains("Development server started at http://localhost:8080")
            }
        }

        // @GradleTest automatically deletes the project directory.
        // However, sometimes it does this too quickly.
        // So, give some time to allow the Gradle daemon to close successfully, avoiding the error:
        // org.gradle.internal.build.BuildLayoutValidator$BuildLayoutException: Directory '...' does not contain a Gradle build.
        Thread.sleep(5000)
    }

    @GradleTest
    @TestMetadata("wasm-run-continuous")
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {
        doTest(
            gradleVersion,
            "wasm",
            "wasmJs"
        ) {
            assertFileContains(
                it,
                "Hello, world!"
            )

            assertFileContains(
                it,
                "Hello again!!!"
            )
        }
    }

    @GradleTest
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsWithAlreadyOccupiedDefaultPort(
        gradleVersion: GradleVersion,
    ) {

        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "wasm-browser-simple-project"
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }
                }
            }

            projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                .also {
                    it.createParentDirectories()
                }
                .writeText(
                    """
                    fun main() {
                        println("Hello, world")
                    }
                    """.trimIndent()
                )

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val closer = thread(name = "testWasmJsWithAlreadyOccupiedDefaultPort closer", isDaemon = true) {
                daemonRelease.close()
            }

            val testAction = {
                build(
                    "wasmJsBrowserDevelopmentRun",
                    buildOptions = defaultBuildOptions.copy(
                        continuousBuild = true,
                    ),
                    inputStream = daemonStdin,
                    forwardBuildOutput = true,
                ) {
                    closer.join()

                    assertOutputContains("Development server started at http://localhost:")
                }
            }

            try {
                ServerSocket(
                    DEFAULT_PORT,
                    0,
                    InetAddress.getByName("localhost")
                ).use {
                    testAction()
                }
            } catch (_: IOException) {
                // in case of another test already binds default port
                testAction()
            }
        }

        // @GradleTest automatically deletes the project directory.
        // However, sometimes it does this too quickly.
        // So, give some time to allow the Gradle daemon to close successfully, avoiding the error:
        // org.gradle.internal.build.BuildLayoutValidator$BuildLayoutException: Directory '...' does not contain a Gradle build.
        Thread.sleep(5000)
    }

    @GradleTest
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsDevServerServesOnlyKotlinSourcesFromProjectRoot(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = PROJECT_NAME
            }

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmJs {
                        binaries.executable()
                        browser(bundler = KotlinBrowserBundler.NONE)
                    }
                }
            }

            val mainSource =
                """
                fun main() {
                    println("Hello, world")
                }
                """.trimIndent()
            val indexHtml = "<html><body>Hello, world</body></html>"

            projectPath.resolve("src/wasmJsMain/kotlin/A.kt")
                .also { it.createParentDirectories() }
                .writeText(mainSource)

            projectPath.resolve("src/wasmJsMain/resources/index.html")
                .also { it.createParentDirectories() }
                .writeText(indexHtml)

            projectPath.resolve("local.properties").writeText("password=hunter2")
            projectPath.resolve(".env").writeText("TOKEN=hunter2")

            // a Kotlin source outside of the project root, only the containment check can refuse it
            projectPath.resolveSibling("outside.kt").writeText("fun outside() {}")

            val contentDirectory = projectPath.resolve("build/wasm/packages/$PROJECT_NAME/kotlin")
            val contentDirectoryPath = projectPath.relativize(contentDirectory).invariantSeparatorsPathString
            val contentDirectoryToRoot = "../".repeat(contentDirectory.nameCount - projectPath.nameCount)

            val servedFromProjectRoot = listOf(
                "/src/wasmJsMain/kotlin/A.kt",
                "/$contentDirectoryPath/${contentDirectoryToRoot}src/wasmJsMain/kotlin/A.kt",
            )

            val refusedProjectRootFiles = listOf(
                "local.properties",
                ".env",
                "build.gradle",
                "src/wasmJsMain/resources/index.html",
            )
            refusedProjectRootFiles.forEach { assertFileExists(projectPath.resolve(it)) }

            val refusedPaths = refusedProjectRootFiles.map { "/$it" } + listOf(
                "/src/wasmJsMain/kotlin",
                "/../outside.kt",
                "/%2e%2e/outside.kt",
                "/$contentDirectoryPath/$contentDirectoryToRoot../outside.kt",
                "//" + projectPath.resolve("local.properties").invariantSeparatorsPathString.trimStart('/'),
            )

            val serverPort = findFreePort()
            val artifactPaths = mutableListOf<String>()
            val responses = linkedMapOf<String, DevServerResponse>()

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testWasmJsDevServerServesOnlyKotlinSourcesFromProjectRoot checker", isDaemon = true) {
                try {
                    val buildStartMark = TimeSource.Monotonic.markNow()
                    fun checkBuildDuration() {
                        check(buildStartMark.elapsedNow() < 10.minutes) {
                            "build took too long - ${buildStartMark.elapsedNow()}"
                        }
                    }

                    println("Waiting for the development server to start...")
                    while (runCatching { httpGet(serverPort, "/").status }.getOrNull() != 200) {
                        Thread.sleep(1000)
                        checkBuildDuration()
                    }
                    println("Development server has started.")

                    val contentDirectoryFile = contentDirectory.toFile()
                    artifactPaths += contentDirectoryFile.walkTopDown()
                        .filter { it.isFile }
                        .map { it.relativeTo(contentDirectoryFile).invariantSeparatorsPath }
                        .sorted()
                    println("Produced artifacts: $artifactPaths")

                    val requestedPaths = buildList {
                        add("/")
                        artifactPaths.forEach { add("/$it") }
                        addAll(servedFromProjectRoot)
                        addAll(refusedPaths)
                    }
                    for (path in requestedPaths) {
                        responses[path] = httpGet(serverPort, path)
                    }
                    println("All requests have been performed.")
                } catch (t: Throwable) {
                    println("Exception in ${Thread.currentThread().name}:\n${t.stackTraceToString()}")
                    throw t
                } finally {
                    println("Releasing daemon stdin stream...")
                    // close the stream, which will allow Gradle to finish the build
                    daemonRelease.close()
                    println("Released daemon stdin stream.")
                }
            }

            build(
                "wasmJsBrowserDevelopmentRun", "--port", serverPort.toString(),
                buildOptions = defaultBuildOptions.copy(
                    verboseVfsLogging = true,
                    continuousBuild = true,
                ),
                inputStream = daemonStdin,
                forwardBuildOutput = true,
            ) {
                checker.join()

                val expectedStatuses = buildMap {
                    // the content directory contains only produced artifacts, so all of them are served
                    put("/", 200)
                    artifactPaths.forEach { put("/$it", 200) }
                    servedFromProjectRoot.forEach { put(it, 200) }
                    refusedPaths.forEach { put(it, 404) }
                }

                assertEquals(expectedStatuses, responses.mapValues { (_, response) -> response.status })

                // the produced artifacts are enumerated in runtime, make sure the expectations on them are not vacuous
                assertContains(artifactPaths, "index.html")
                assertContains(artifactPaths, "$PROJECT_NAME.wasm")

                assertEquals(indexHtml, responses.getValue("/").body)

                val servedSource = responses.getValue("/src/wasmJsMain/kotlin/A.kt")
                assertEquals(mainSource, servedSource.body)
                assertEquals("text/kotlin; charset=utf-8", servedSource.contentType)

                assertOutputContains("Development server started at http://localhost:$serverPort")
            }
        }

        // @GradleTest automatically deletes the project directory.
        // However, sometimes it does this too quickly.
        // So, give some time to allow the Gradle daemon to close successfully, avoiding the error:
        // org.gradle.internal.build.BuildLayoutValidator$BuildLayoutException: Directory '...' does not contain a Gradle build.
        Thread.sleep(5000)
    }

    private class DevServerResponse(val status: Int, val contentType: String?, val body: String)

    /**
     * Sends a GET request for [rawPath] as is.
     * [HttpClient] doesn't normalize the request [URI], so `..` segments reach the server exactly as written.
     */
    private fun httpGet(port: Int, rawPath: String): DevServerResponse {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port$rawPath")).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return DevServerResponse(
            status = response.statusCode(),
            contentType = response.headers().firstValue("Content-Type").orElse(null),
            body = response.body(),
        )
    }

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

    private companion object {
        const val PROJECT_NAME = "wasm-browser-simple-project"

        val httpClient: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
    }
}
