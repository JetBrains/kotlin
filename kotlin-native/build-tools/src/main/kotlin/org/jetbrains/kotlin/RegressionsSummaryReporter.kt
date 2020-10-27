/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackPreparedMessage

import java.io.FileInputStream
import java.io.File
import java.util.Properties

/**
 * Task to produce regressions report and send it to slack. Requires a report with current benchmarks result
 * and path to analyzer tool
 *
 * @property targetsResultFiles  map with pathes to results of each target
 */
open class RegressionsSummaryReporter : DefaultTask() {
    @Input
    lateinit var targetsResultFiles: Map<String, String>

    val performanceServer = "https://kotlin-native-perf-summary.labs.jb.gg/"

    @TaskAction
    fun run() {
        // Get TeamCity properties.
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") ?:
            error("Can't load teamcity config!")

        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))
        val buildId = buildProperties.getProperty("teamcity.build.id")
        val buildTypeId = buildProperties.getProperty("teamcity.buildType.id")
        val buildNumber = buildProperties.getProperty("build.number")
        val results = mutableMapOf<String, MutableMap<String, String>>()

        // Parse and merge results from all targets.
        targetsResultFiles.forEach { (key, value) ->
            val file = File(value)
            if (file.exists()) {
                file.forEachLine {
                    val matchResult = "(\\w+)\\s*:\\s*(\\w+)".toRegex().find(it)
                    val propertyName = matchResult?.groups?.get(1)?.value
                    val propertyValue = matchResult?.groups?.get(2)?.value
                    if (propertyName != null && propertyValue != null) {
                        results[propertyName]?.let { it[key] = propertyValue }
                                ?: run { results.put(propertyName, mutableMapOf(key to propertyValue)) }
                    }
                }
            }
        }

        val message = buildString {
            append("*Performance summary on charts* - $performanceServer\n")
            results.forEach { (property, targets) ->
                append("$property: ${targets.map {(target, value) -> "$value ($target)"}.joinToString(" | ")}\n")
            }
        }
        val summaryStatus = results["status"]?.values?.fold("STABLE") {summary, element ->
            when {
                summary == "FAILED" || element == "FAILED" -> "FAILED"
                summary == "FIXED" || element == "FIXED" -> "FIXED"
                summary == "STABLE" -> element
                summary == element -> summary
                else -> "UNSTABLE"
            }
        }

        val attachement = SlackAttachment()
        with (attachement) {
            setTitle("Performance Summary (build $buildNumber)")
            setTitleLink("https://buildserver.labs.intellij.net/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId")
            setText(message)
            if (summaryStatus == "FIXED" || summaryStatus == "IMPROVED") {
                setColor("#36a64f")
            } else if (summaryStatus == "FAILED" || summaryStatus == "REGRESSED") {
                setColor("#ff0000")
            }
        }

        // Send to channel or user directly.
        val session = SlackSessionFactory.createWebSocketSlackSession(buildProperties.getProperty("konan-reporter-token"))
        session.connect()

        val channel = session.findChannelByName(buildProperties.getProperty("konan-channel-name"))
        val preparedMessage = SlackPreparedMessage.Builder()
                .addAttachment(attachement)
                .build()
        session.sendMessage(channel, preparedMessage)

        session.disconnect()
    }
}