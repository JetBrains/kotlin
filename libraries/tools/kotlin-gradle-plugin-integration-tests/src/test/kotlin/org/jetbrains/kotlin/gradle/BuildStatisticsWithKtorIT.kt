/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.report.data.GradleCompileStatisticsData
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.*

@DisplayName("Build statistics")
class BuildStatisticsWithKtorIT : KGPBaseTest() {

    companion object {
        fun runWithKtorService(action: (Int) -> Unit) {
            var server: ApplicationEngine? = null
            try {
                val port = getEmptyPort().localPort
                server = embeddedServer(Netty, host = "localhost", port = port)
                {
                    val requests = ArrayBlockingQueue<String>(10)

                    routing {
                        get("/isReady") {
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/badRequest") {
                            call.respond(HttpStatusCode.BadRequest, "Some reason")
                        }
                        post("/put") {
                            val body = call.receive<String>()
                            requests.add(body)
                            call.respond(HttpStatusCode.OK)
                        }
                        get("/validate") {
                            try {
                                call.respond(status = HttpStatusCode.OK, requests.poll(2, TimeUnit.SECONDS))
                            } catch (e: Exception) {
                                call.respond(status = HttpStatusCode.NotFound, e.message ?: e::class)
                            }
                        }

                    }
                }.start()
                awaitInitialization(port)
                action(port)
            } finally {
                server?.stop(1000, 1000)
            }
        }

        private fun getEmptyPort(): ServerSocket {
            val startPort = 8080
            val endPort = 8180
            for (port in startPort..endPort) {
                try {
                    return ServerSocket().apply {
                        bind(InetSocketAddress("localhost", port))
                    }.also {
                        println("Use $port port")
                        it.close()
                    }
                } catch (_: IOException) {
                    continue // try next port
                }
            }
            throw IOException("Failed to find free IP port in range $startPort..$endPort")
        }

        private fun awaitInitialization(port: Int, maxAttempts: Int = 20) {
            var attempts = 0
            val waitingTime = 500L
            while (initCall(port) != HttpStatusCode.OK.value) {
                attempts += 1
                if (attempts == maxAttempts) {
                    fail("Failed to await server initialization for ${waitingTime * attempts}ms")
                }
                Thread.sleep(waitingTime)
            }
        }

        private fun initCall(port: Int): Int {
            return try {
                val connection = URL("http://localhost:$port/isReady").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                connection.responseCode
            } catch (e: IOException) {
                fail("Unable to open connection: ${e.message}", e)
            }
        }

        private fun validateCall(port: Int, validate: (JsonObject) -> Unit) {
            try {
                val connection = URL("http://localhost:$port/validate").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                assertEquals(HttpStatusCode.OK.value, connection.responseCode)
                val body = connection.inputStream.bufferedReader().readText()
                val jsonObject = JsonParser.parseString(body).asJsonObject
                validate(jsonObject)
            } catch (e: IOException) {
                fail("Unable to open connection: ${e.message}", e)
            }
        }

        fun validateTaskData(port: Int, validate: (GradleCompileStatisticsData) -> Unit) {
            validateCall(port) { jsonObject ->
                val type = jsonObject["type"].asString
                assertEquals(BuildDataType.TASK_DATA, BuildDataType.valueOf(type))
                val taskData = Gson().fromJson(jsonObject, GradleCompileStatisticsData::class.java)
                validate(taskData)
            }
        }

        fun validateBuildData(port: Int, validate: (BuildFinishStatisticsData) -> Unit) {
            validateCall(port) { jsonObject ->
                val type = jsonObject["type"].asString
                assertEquals(BuildDataType.BUILD_DATA, BuildDataType.valueOf(type))
                val buildData = Gson().fromJson(jsonObject, BuildFinishStatisticsData::class.java)
                validate(buildData)
            }
        }

    }

    @DisplayName("Http build report request problems are logged only ones")
    @GradleTest
    @JvmGradlePluginTests
    fun testHttpServiceWithBadRequest(gradleVersion: GradleVersion) {
        runWithKtorService { port ->
            project("incrementalMultiproject", gradleVersion) {
                enableStatisticReports(BuildReportType.HTTP, "http://localhost:$port/badRequest")
                build("assemble") {
                    assertOutputContainsExactTimes("Failed to send statistic to", 1)
                }
            }
        }
    }

    private fun simpleTestHttpReport(
        gradleVersion: GradleVersion,
        additionalProjectSetup: (TestProject) -> Unit = {},
        compileTaskAssertions: (GradleCompileStatisticsData) -> Unit,
    ) {
        runWithKtorService { port ->
            project("incrementalMultiproject", gradleVersion) {
                additionalProjectSetup(this)
                setProjectForTest(port)
                build("clean", "assemble") {
                    assertOutputDoesNotContain("Failed to send statistic to")
                }
            }
            validateTaskData(port) { taskData ->
                assertEquals(":lib:compileKotlin", taskData.getTaskName())
                compileTaskAssertions(taskData)
            }
            validateTaskData(port) { taskData ->
                assertEquals(":app:compileKotlin", taskData.getTaskName())
                compileTaskAssertions(taskData)
            }
            validateBuildData(port) { buildData ->
                assertContains(buildData.startParameters.tasks, "assemble")
            }
        }
    }

    @DisplayName("Validate mandatory field for http request body")
    @GradleTest
    @JvmGradlePluginTests
    fun testHttpRequest(gradleVersion: GradleVersion) {
        simpleTestHttpReport(gradleVersion) { taskData ->
            assertContains(taskData.getTags(), StatTag.NON_INCREMENTAL)
            assertContains(taskData.getNonIncrementalAttributes().map { it.name }, "UNKNOWN_CHANGES_IN_GRADLE_INPUTS")
            assertFalse(taskData.getPerformanceMetrics().keys.isEmpty())
            assertFalse(taskData.getBuildTimesMetrics().keys.isEmpty())
            assertFalse(taskData.getCompilerArguments().isEmpty())
            assertEquals(
                defaultBuildOptions.kotlinVersion, taskData.getKotlinVersion(),
                "Unexpected kotlinVersion: ${taskData.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
            )
        }
    }

    @DisplayName("Compiler arguments reporting can be disabled")
    @GradleTest
    @JvmGradlePluginTests
    fun testDisablingCompilerArgumentsReporting(gradleVersion: GradleVersion) {
        simpleTestHttpReport(gradleVersion, { project ->
            project.gradleProperties.append(
                """
                |
                |kotlin.build.report.include_compiler_arguments=false
                """.trimMargin()
            )
        }) { taskData ->
            assertContains(taskData.getTags(), StatTag.NON_INCREMENTAL)
            assertContains(taskData.getNonIncrementalAttributes().map { it.name }, "UNKNOWN_CHANGES_IN_GRADLE_INPUTS")
            assertFalse(taskData.getPerformanceMetrics().keys.isEmpty())
            assertFalse(taskData.getBuildTimesMetrics().keys.isEmpty())
            assertTrue(taskData.getCompilerArguments().isEmpty())
            assertEquals(
                defaultBuildOptions.kotlinVersion, taskData.getKotlinVersion(),
                "Unexpected kotlinVersion: ${taskData.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
            )
        }
    }

    @DisplayName("Validate configuration cache tag")
    @GradleTest
    @JvmGradlePluginTests
    fun testConfigurationCache(gradleVersion: GradleVersion) {
        runWithKtorService { port ->

            val buildOptions = defaultBuildOptions.copy(configurationCache = true)
            project("incrementalMultiproject", gradleVersion) {
                setProjectForTest(port)
                build("assemble", buildOptions = buildOptions) {
                    assertOutputDoesNotContain("Failed to send statistic to")
                }
                projectPath.resolve("lib/src/main/kotlin/bar/B.kt").modify { it.replace("fun b() {}", "fun b() = 1") }
                build("assemble", buildOptions = buildOptions) {
                    assertOutputDoesNotContain("Failed to send statistic to")
                }
            }
            validateTaskData(port) { taskData ->
                assertEquals(":lib:compileKotlin", taskData.getTaskName())
                assertContentEquals(
                    listOf(
                        StatTag.ARTIFACT_TRANSFORM,
                        StatTag.NON_INCREMENTAL,
                        StatTag.CONFIGURATION_CACHE,
                        StatTag.KOTLIN_1,
                    ),
                    taskData.getTags().sorted(),
                )
                assertEquals(
                    defaultBuildOptions.kotlinVersion, taskData.getKotlinVersion(),
                    "Unexpected kotlinVersion: ${taskData.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
                )
            }
            validateTaskData(port) { taskData ->
                assertEquals(":app:compileKotlin", taskData.getTaskName())
                assertContentEquals(
                    listOf(
                        StatTag.ARTIFACT_TRANSFORM,
                        StatTag.NON_INCREMENTAL,
                        StatTag.CONFIGURATION_CACHE,
                        StatTag.KOTLIN_1
                    ), taskData.getTags().sorted()
                )
                assertEquals(
                    defaultBuildOptions.kotlinVersion, taskData.getKotlinVersion(),
                    "Unexpected kotlinVersion: ${taskData.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
                )
            }
            validateBuildData(port) { buildData ->
                assertContains(buildData.startParameters.tasks, "assemble")
            }
            //second build
            validateTaskData(port) { taskData ->
                assertEquals(":lib:compileKotlin", taskData.getTaskName())
                assertContentEquals(
                    listOf(StatTag.ARTIFACT_TRANSFORM, StatTag.INCREMENTAL, StatTag.CONFIGURATION_CACHE, StatTag.KOTLIN_1),
                    taskData.getTags().sorted()
                )
            }
            validateTaskData(port) { taskData ->
                assertEquals(":app:compileKotlin", taskData.getTaskName())
                assertContentEquals(
                    listOf(StatTag.ARTIFACT_TRANSFORM, StatTag.INCREMENTAL, StatTag.CONFIGURATION_CACHE, StatTag.KOTLIN_1),
                    taskData.getTags().sorted()
                )
            }
        }
    }

    @DisplayName("Build reports for native")
    @GradleTest
    @NativeGradlePluginTests
    fun buildReportForNative(gradleVersion: GradleVersion) {
        runWithKtorService { port ->
            nativeProject(
                "k2-native-intermediate-metadata",
                gradleVersion,
                buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.WARN)
            ) {
                setProjectForTest(port)
                build(
                    "build",
                    // Disables cache for explicit task execution and metric collection
                    "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false",
                ) {
                    assertOutputDoesNotContain("Failed to send statistic to")
                }
            }
            val commonizerNativeDistributionTask = ":commonizeNativeDistribution"
            validateTaskData(port) { taskReport ->
                assertEquals(commonizerNativeDistributionTask, taskReport.getTaskName())


                assertContains(
                    taskReport.getBuildTimesMetrics().keys,
                    GradleBuildTime.NATIVE_IN_EXECUTOR,
                    "Assertion failed for task \"$commonizerNativeDistributionTask\""
                )
                assertEquals(
                    defaultBuildOptions.kotlinVersion, taskReport.getKotlinVersion(),
                    "Unexpected kotlinVersion: ${taskReport.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
                )
            }

            val compileNativeTasks = listOf(
                ":compileCommonMainKotlinMetadata",
                ":compileNativeMainKotlinMetadata",
            )

            for (task in compileNativeTasks) {
                validateTaskData(port) { taskReport ->
                    //can be sure in task execution order
                    assertEquals(task, taskReport.getTaskName())

                    assertContains(
                        taskReport.getBuildTimesMetrics().keys,
                        GradleBuildTime.NATIVE_IN_PROCESS,
                        "Assertion failed for task \"$task\""
                    )
                    assertContains(
                        taskReport.getBuildTimesMetrics().keys,
                        GradleBuildTime.RUN_ENTRY_POINT,
                        "Assertion failed for task \"$task\""
                    )
                    assertEquals(
                        defaultBuildOptions.kotlinVersion, taskReport.getKotlinVersion(),
                        "Unexpected kotlinVersion: ${taskReport.getKotlinVersion()} instead of ${defaultBuildOptions.kotlinVersion}"
                    )
                }
            }
        }
    }


    private fun TestProject.setProjectForTest(port: Int) {
        enableStatisticReports(BuildReportType.HTTP, "http://localhost:$port/put")
    }
}






