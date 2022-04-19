/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.collections.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.io.path.appendText
import kotlin.test.fail

@DisplayName("Build statistics")
@JvmGradlePluginTests
class BuildStatisticsWithKtorIT : KGPBaseTest() {

    companion object {
        fun CompileStatisticsData.validateMandatoryField(kotlinVersion: String, validationData: ValidationData): List<String> {
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

        fun CompileStatisticsData.validateIncrementalData(validationData: ValidationData): List<String> {
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

        fun CompileStatisticsData.validateNonIncrementalData(validationData: ValidationData): List<String> {
            if (validationData.nonIncrementalReasons.isEmpty()) return emptyList()
            val validationErrors = LinkedList<String>()
            if (!tags.contains(StatTag.NON_INCREMENTAL.name)) {
                validationErrors.add("NON_INCREMENTAL tag was no set")
            }
            return validationErrors
        }

    }


    private lateinit var server: ApplicationEngine

    @BeforeAll
    fun initEmbeddedService() {
        val ktorTestDataAnnotations = this.javaClass.methods.flatMap { it.annotations.filterIsInstance<KtorTestData>() }
        println("start embedded server with data: ${ktorTestDataAnnotations.joinToArrayString()}")
        server = embeddedServer(Netty, port = 8080)
        {
            val ktorServerData = ktorTestDataAnnotations.associateBy({ it.projectName }) { it.validationData.toMutableList() }
            val failedResults = ConcurrentList<String>()

            suspend fun responseBadRequest(call: ApplicationCall, message: String) {
                println("Validation errors: $message")
                failedResults.plus(message)
                call.respond(status = HttpStatusCode.BadRequest, message)
            }

            routing {
                post("/badRequest") {
                    call.respond(HttpStatusCode.BadRequest, "Some reason")
                }
                post("/validate") {
                    val body = call.receive<String>()
                    println("TRACE: routing was called: $body")

                    val statData = Gson().fromJson(body, CompileStatisticsData::class.java)
                    val projectValidation = ktorServerData[statData.projectName]
                    if (projectValidation == null) {
                        responseBadRequest(call, "Unknown validation for project ${statData.projectName}")
                        return@post
                    }

                    val ktorTestData = projectValidation.firstOrNull { it.taskName == statData.taskName }

                    if (ktorTestData == null) {
                        responseBadRequest(call, "${statData.projectName}: Unknown validation for task ${statData.taskName}")
                        return@post
                    }

                    //validate response
                    statData.validateMandatoryField(defaultBuildOptions.kotlinVersion, ktorTestData).let {
                        if (it.isNotEmpty()) {
                            responseBadRequest(
                                call,
                                "${statData.projectName}: Fail to validate mandatory fields: ${it.joinToArrayString()}"
                            )
                            return@post
                        }
                    }
                    statData.validateIncrementalData(ktorTestData).let {
                        if (it.isNotEmpty()) {
                            responseBadRequest(
                                call,
                                "${statData.projectName}: Fail to validate incremental fields: ${it.joinToArrayString()}"
                            )
                            return@post
                        }
                    }
                    statData.validateNonIncrementalData(ktorTestData).let {
                        if (it.isNotEmpty()) {
                            responseBadRequest(
                                call,
                                "${statData.projectName}: Fail to validate non-incremental fields: ${it.joinToArrayString()}"
                            )
                            return@post
                        }
                    }
                    call.respond(HttpStatusCode.OK)
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
        try {
            val connection = URL("http://localhost:8080/results").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == HttpStatusCode.InternalServerError.value) {
                fail(connection.responseMessage)
            }
        } finally {
            server.stop(1000, 1000)
        }
    }


    @DisplayName("Http build report request problems are logged only ones")
    @GradleTest
    fun testHttpServiceWithBadRequest(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            enableStatisticReports(BuildReportType.HTTP, "http://localhost:8080/badRequest")
            build("assemble") {
                assertOutputContainsExactTimes("Failed to send statistic to", 1)
            }
        }
    }

    @KtorTestData(
        "validateMandatoryField",
        [
            //first run
            ValidationData(":lib:compileKotlin", expectedTags = ["NON_INCREMENTAL"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            ValidationData(":app:compileKotlin", expectedTags = ["NON_INCREMENTAL"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"])
        ]
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
            ValidationData(":lib:compileKotlin", expectedTags = ["NON_INCREMENTAL", "CONFIGURATION_CACHE"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            ValidationData(":app:compileKotlin", expectedTags = ["NON_INCREMENTAL", "CONFIGURATION_CACHE"], nonIncrementalReasons = ["UNKNOWN_CHANGES_IN_GRADLE_INPUTS"]),
            //second run
            ValidationData(":lib:compileKotlin", expectedTags = ["INCREMENTAL", "CONFIGURATION_CACHE"]),
            ValidationData(":app:compileKotlin", expectedTags = ["INCREMENTAL", "CONFIGURATION_CACHE"]),
       ]
    )
    @DisplayName("Validate configutaration cache tag")
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
        enableStatisticReports(BuildReportType.HTTP, "http://localhost:8080/validate")
        settingsGradle.appendText("rootProject.name=\'$projectName\'")
    }

    fun testConfigurationCacheTag(gradleVersion: GradleVersion) {

    }
}

@Repeatable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KtorTestData(
    val projectName: String,
    val validationData: Array<ValidationData>,
)

annotation class ValidationData(
    //mandatory fields
    val taskName: String,
    val taskResult: String = "SUCCESS",
    val expectedTags: Array<String> = [],

    //fields for non-incremental compilation
    val nonIncrementalReasons: Array<String> = [], //if empty incremental validation expected

    //fields for incremental compilation
    val changedFiles: Array<String> = [],
)



