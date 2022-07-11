/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.collections.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.stat.*
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.utils.`is`
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import java.io.IOException
import java.net.ServerSocket
import java.util.*
import kotlin.io.path.appendText

@DisplayName("Build statistics")
@JvmGradlePluginTests
class BuildStatisticsWithKtorIT : KGPBaseTest() {

    companion object {
        fun CompileStatisticsData.validateMandatoryField(kotlinVersion: String, validationData: TaskExecutionValidationData): List<String> {
            val validationErrors = LinkedList<String>()
            if (taskResult != validationData.taskResult) {
                validationErrors.add("Unexpected taskResult: $taskResult instead of ${validationData.taskResult}")
            }
            if (this.kotlinVersion != kotlinVersion) {
                validationErrors.add("Unexpected kotlinVersion: ${this.kotlinVersion} instead of ${kotlinVersion}")
            }
            if (compilerArguments.isEmpty()) {
                validationErrors.add("Empty compiler arguments")
            }
            if (performanceMetrics.isEmpty()) {
                validationErrors.add("Empty performance metrics")
            }
            if (buildTimesMetrics.isEmpty()) {
                validationErrors.add("Empty build metrics")
            }
            for (tag: String in validationData.expectedTags) {
                if (!tags.contains(tag)) {
                    validationErrors.add("Does not contains \'$tag\' tag")
                }
            }
            return validationErrors
        }

        fun CompileStatisticsData.validateIncrementalData(validationData: TaskExecutionValidationData): List<String> {
            if (validationData.nonIncrementalReasons.isNotEmpty()) return emptyList()
            val validationErrors = LinkedList<String>()
            if (changes.size != validationData.changedFiles.size) {
                validationErrors.add("Changed files do not equal: ${changes.joinToArrayString()} instead of ${validationData.changedFiles.joinToArrayString()} ")
            }
            if (!tags.contains(StatTag.INCREMENTAL.name)) {
                validationErrors.add("INCREMENTAL tag was no set")
            }
            return validationErrors
        }

        fun CompileStatisticsData.validateNonIncrementalData(validationData: TaskExecutionValidationData): List<String> {
            if (validationData.nonIncrementalReasons.isEmpty()) return emptyList()
            val validationErrors = LinkedList<String>()
            if (!tags.contains(StatTag.NON_INCREMENTAL.name)) {
                validationErrors.add("NON_INCREMENTAL tag was no set")
            }
            return validationErrors
        }

        fun BuildFinishStatisticsData.validate(validationData: BuildExecutionValidationData): List<String> {
            if (validationData.tasks.isEmpty()) return emptyList()
            val validationErrors = LinkedList<String>()
            if (!startParameters.tasks.containsAll(validationData.tasks.toList())) {
                validationErrors.add("Different set of executed tasks. Expected ${validationData.tasks.asList()} actual ${startParameters.tasks}")
            }
            return validationErrors
        }

        fun getEmptyPort(): ServerSocket {
            val startPort = 8080
            val endPort = 8180
            for (port in startPort..endPort) {
                try {
                    return ServerSocket(port).also {
                        println("Use $port port")
                        it.close()
                    }
                } catch (_: IOException) {
                    continue // try next port
                }

            }
            throw IOException("Failed to find free IP port in range $startPort..$endPort")
        }

    }

    private lateinit var server: ApplicationEngine

    private val port = getEmptyPort().localPort

    @BeforeAll
    fun initEmbeddedService() {
        val ktorTestDataAnnotations = this.javaClass.methods.flatMap { it.annotations.filterIsInstance<KtorTestData>() }
        println("start embedded server with data: ${ktorTestDataAnnotations.joinToArrayString()}")
        server = embeddedServer(Netty, port = port)
        {
            val ktorServerData = ktorTestDataAnnotations.associateBy { it.projectName }
            val failedResults = ConcurrentList<String>()

            suspend fun responseBadRequest(call: ApplicationCall, message: String) {
                println("Validation errors: $message")
                failedResults.plus(message)
                call.respond(status = HttpStatusCode.BadRequest, message)
            }

            suspend fun validateBuildExecutionData(
                statisticsData: BuildFinishStatisticsData,
                projectValidation: KtorTestData,
                call: ApplicationCall
            ) {
                statisticsData.validate(projectValidation.buildValidationData).let {
                    if (it.isNotEmpty()) {
                        responseBadRequest(
                            call,
                            "${projectValidation.projectName}: Fail to validate statistics after build executed: ${it.joinToArrayString()}"
                        )
                        return
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }

            suspend fun validateTaskExecutionData(statData: CompileStatisticsData, projectValidation: KtorTestData, call: ApplicationCall) {
                val ktorTestData = projectValidation.validationData.firstOrNull { it.taskName == statData.taskName }

                if (ktorTestData == null) {
                    responseBadRequest(call, "${projectValidation.projectName}: Unknown validation for task ${statData.taskName}")
                    return
                }

                //validate response
                statData.validateMandatoryField(defaultBuildOptions.kotlinVersion, ktorTestData).let {
                    if (it.isNotEmpty()) {
                        responseBadRequest(
                            call,
                            "${projectValidation.projectName}: Fail to validate mandatory fields: ${it.joinToArrayString()}"
                        )
                        return
                    }
                }
                statData.validateIncrementalData(ktorTestData).let {
                    if (it.isNotEmpty()) {
                        responseBadRequest(
                            call,
                            "${projectValidation.projectName}: Fail to validate incremental fields: ${it.joinToArrayString()}"
                        )
                        return
                    }
                }
                statData.validateNonIncrementalData(ktorTestData).let {
                    if (it.isNotEmpty()) {
                        responseBadRequest(
                            call,
                            "${projectValidation.projectName}: Fail to validate non-incremental fields: ${it.joinToArrayString()}"
                        )
                        return
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            routing {
                post("/badRequest") {
                    call.respond(HttpStatusCode.BadRequest, "Some reason")
                }
                post("/validate") {
                    val body = call.receive<String>()
                    println("TRACE: routing was called: $body")

                    val jsonObject = JsonParser.parseString(body).asJsonObject
                    val projectName = jsonObject["projectName"].asString

                    val projectValidation = ktorServerData[projectName]
                    if (projectValidation == null) {
                        responseBadRequest(call, "Unknown validation for project $projectName")
                        return@post
                    }

                    val type = jsonObject["type"].asString

                    when (BuildDataType.valueOf(type)) {
                        BuildDataType.TASK_DATA -> validateTaskExecutionData(
                            Gson().fromJson(body, CompileStatisticsData::class.java),
                            projectValidation,
                            call
                        )
                        BuildDataType.BUILD_DATA -> validateBuildExecutionData(
                            Gson().fromJson(body, BuildFinishStatisticsData::class.java),
                            projectValidation,
                            call
                        )
                    }
                }
                get("/results") {
                    if (failedResults.isEmpty()) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, message = failedResults.joinToArrayString())
                    }
                }
            }
        }
        server.start()
    }

    @AfterAll
    fun shutDownEmbeddedService() {
        server.stop(1000, 1000)
    }


    @DisplayName("Http build report request problems are logged only ones")
    @GradleTest
    fun testHttpServiceWithBadRequest(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "http://localhost:$port/badRequest")
            build("assemble") {
                assertOutputContainsExactTimes("Failed to send statistic to", 1)
            }
        }
    }

    @KtorTestData(
        "validateMandatoryField",
        [
            //first run
            TaskExecutionValidationData(":lib:compileKotlin", expectedTags = ["NON_INCREMENTAL"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            TaskExecutionValidationData(":app:compileKotlin", expectedTags = ["NON_INCREMENTAL"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"])
        ],
        BuildExecutionValidationData(["assemble"])
    )
    @DisplayName("Validate mandatory field for http request body")
    @GradleTest
    fun testHttpRequest(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            setProjectForTest("validateMandatoryField")
            build("assemble") {
                assertOutputDoesNotContain("Failed to send statistic to")
            }

        }
    }

    @KtorTestData(
        "validateConfigurationCache",
        [
            //first run
            TaskExecutionValidationData(":lib:compileKotlin", expectedTags = ["NON_INCREMENTAL", "CONFIGURATION_CACHE"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            TaskExecutionValidationData(":app:compileKotlin", expectedTags = ["NON_INCREMENTAL", "CONFIGURATION_CACHE"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            //second run
            TaskExecutionValidationData(":lib:compileKotlin", expectedTags = ["INCREMENTAL", "CONFIGURATION_CACHE"]),
            TaskExecutionValidationData(":app:compileKotlin", expectedTags = ["INCREMENTAL", "CONFIGURATION_CACHE"]),
       ],
        BuildExecutionValidationData(["assemble"])
    )
    @DisplayName("Validate configuration cache tag")
    @GradleTest
    fun testConfigurationCache(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(configurationCache = true)
        project("incrementalMultiproject", gradleVersion) {
            setProjectForTest("validateConfigurationCache")
            build("assemble", buildOptions = buildOptions) {
                assertOutputDoesNotContain("Failed to send statistic to")
            }
            projectPath.resolve("lib/src/main/kotlin/bar/B.kt")
            build("assemble", buildOptions = buildOptions) {
                assertOutputDoesNotContain("Failed to send statistic to")
            }
        }
    }

    private fun TestProject.setProjectForTest(projectName: String) {
        enableStatisticReports(BuildReportType.HTTP, "http://localhost:$port/validate")
        settingsGradle.appendText("rootProject.name=\'$projectName\'")
    }
}

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KtorTestData(
    val projectName: String,
    val validationData: Array<TaskExecutionValidationData>,
    val buildValidationData: BuildExecutionValidationData = BuildExecutionValidationData()
)

annotation class TaskExecutionValidationData(
    //mandatory fields
    val taskName: String,
    val taskResult: String = "SUCCESS",
    val expectedTags: Array<String> = [],

    //fields for non-incremental compilation
    val nonIncrementalReasons: Array<String> = [], //if empty incremental validation expected

    //fields for incremental compilation
    val changedFiles: Array<String> = [],
)

annotation class BuildExecutionValidationData(
    val tasks: Array<String> = [],
    val excludedTasks: Array<String> = [],
    val projectProperties: Array<String> = [],
    val systemProperties: Array<String> = [],
)



