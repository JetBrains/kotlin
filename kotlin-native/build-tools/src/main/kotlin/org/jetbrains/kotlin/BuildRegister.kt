/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

private val Project.performanceServerUrl: String
    get() = findProperty("kotlin.native.performance.server.url")?.toString() ?: "http://localhost:3000"

/**
 * Task to save benchmarks results on server.
 *
 * @property bundleSize size of build
 * @property onlyBranch register only builds for branch
 * @property fileWithResult json file with benchmarks run results
 * @property performanceServer URL of the performance server
 */
open class BuildRegister : DefaultTask() {
    @get:Input
    @get:Optional
    var onlyBranch: String? = null

    @get:Input
    @get:Optional
    var bundleSize: Int? = null

    @get:Input
    @get:Optional
    var buildNumberSuffix: String? = null

    @get:Input
    @get:Optional
    var fileWithResult: String = "nativeReport.json"

    @get:Input
    @get:Optional
    var performanceServer: String? = project.performanceServerUrl

    private fun sendPostRequest(url: String, body: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return connection.apply {
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            requestMethod = "POST"
            doOutput = true
            val outputWriter = OutputStreamWriter(outputStream)
            outputWriter.write(body)
            outputWriter.flush()
        }.let {
            if (it.responseCode == 200) it.inputStream else it.errorStream
        }.let { streamToRead ->
            BufferedReader(InputStreamReader(streamToRead)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
                response.toString()
            }
        }
    }

    @TaskAction
    fun run() {
        // Get TeamCity properties.
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") ?: error("Can't load teamcity config!")

        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))
        val buildId = buildProperties.getProperty("teamcity.build.id")
        val teamCityUser = buildProperties.getProperty("teamcity.auth.userId")
        val teamCityPassword = buildProperties.getProperty("teamcity.auth.password")

        // Get branch.
        val currentBuild = getBuild("id:$buildId", teamCityUser, teamCityPassword)
        val branch = getBuildProperty(currentBuild, "branchName")

        // Send post request to register build.
        val requestBody = buildString {
            append("{\"buildId\":\"$buildId\",")
            append("\"teamCityUser\":\"$teamCityUser\",")
            append("\"teamCityPassword\":\"$teamCityPassword\",")
            append("\"fileWithResult\":\"$fileWithResult\",")
            append("\"bundleSize\": ${bundleSize?.let { "\"$bundleSize\"" } ?: bundleSize},")
            append("\"buildNumberSuffix\": ${buildNumberSuffix?.let { "\"$buildNumberSuffix\"" } ?: buildNumberSuffix}}")
        }
        if (onlyBranch == null || onlyBranch == branch) {
            try {
                println(sendPostRequest("$performanceServer/register", requestBody))
            } catch (t: Throwable) {
                println("Failed to send POST to '$performanceServer/register'")
                throw t
            }
        } else {
            println("Skipping registration. Current branch $branch, need registration for $onlyBranch!")
        }
    }
}