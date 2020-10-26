/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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

import org.jetbrains.report.json.*

import java.io.FileInputStream
import java.io.IOException
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.Properties

/**
 * Task to produce regressions report and send it to slack. Requires a report with current benchmarks result
 * and path to analyzer tool
 *
 * @property currentBenchmarksReportFile  path to file with becnhmarks result
 * @property analyzer path to analyzer tool
 * @property htmlReport name of result html report
 * @property defaultBranch name of default branch
 * @property summaryFile name of file with short summary
 * @property bundleBuild property to show if current build is full or not
 */
open class RegressionsReporter : DefaultTask() {

    val slackUsers = mapOf(
            "olonho" to "nikolay.igotti",
            "nikolay.igotti" to "nikolay.igotti",
            "ilya.matveev" to "ilya.matveev",
            "ilmat192" to "ilya.matveev",
            "vasily.v.levchenko" to "minamoto",
            "vasily.levchenko" to "minamoto",
            "alexander.gorshenev" to "alexander.gorshenev",
            "igor.chevdar" to "igor.chevdar",
            "pavel.punegov" to "Pavel Punegov",
            "dmitriy.dolovov" to "dmitriy.dolovov",
            "svyatoslav.scherbina" to "svyatoslav.scherbina",
            "sbogolepov" to "sergey.bogolepov",
            "Alexey.Zubakov" to "Alexey.Zubakov",
            "kirill.shmakov" to "kirill.shmakov",
            "elena.lepilkina" to "elena.lepilkina")

    @Input
    lateinit var currentBenchmarksReportFile: String

    @Input
    lateinit var analyzer: String

    @Input
    lateinit var htmlReport: String

    @Input
    lateinit var defaultBranch: String

    @Input
    lateinit var summaryFile: String

    @Input
    var bundleBuild: Boolean = false

    private fun tabUrl(buildId: String, buildTypeId: String, tab: String) =
            "$teamCityUrl/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId&tab=$tab"

    private fun testReportUrl(buildId: String, buildTypeId: String) =
            tabUrl(buildId, buildTypeId, "testsInfo")

    private fun previousBuildLocator(buildTypeId: String, branchName: String) =
            "buildType:id:$buildTypeId,branch:name:$branchName,status:SUCCESS,state:finished,count:1"

    private fun changesListUrl(buildLocator: String) =
            "$teamCityUrl/app/rest/changes/?locator=build:$buildLocator"

    private fun getCommits(buildLocator: String, user: String, password: String): CommitsList {
        val changes = try {
            sendGetRequest(changesListUrl(buildLocator), user, password)
        } catch (t: Throwable) {
            error("Try to get commits! TeamCity is unreachable!")
        }
        return CommitsList(JsonTreeParser.parse(changes))
    }

    @TaskAction
    fun run() {
        // Get TeamCity properties.
        val teamcityConfig = System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE") ?:
            error("Can't load teamcity config!")

        val buildProperties = Properties().apply { load(FileInputStream(teamcityConfig)) }
        val buildId = buildProperties.getProperty("teamcity.build.id")
        val buildTypeId = buildProperties.getProperty("teamcity.buildType.id")
        val buildNumber = buildProperties.getProperty("build.number")
        val user = buildProperties.getProperty("teamcity.auth.userId")
        val password = buildProperties.getProperty("teamcity.auth.password")

        // Get branch.
        val currentBuild = getBuild("id:$buildId", user, password)
        val branch = getBuildProperty(currentBuild,"branchName")

        val testReportUrl = testReportUrl(buildId, buildTypeId)

        // Get previous build on branch.
        getBuild(previousBuildLocator(buildTypeId,branch), user, password)

        // Get changes description.
        val changesList = getCommits("id:$buildId", user, password)
        val changesInfo = "*Changes* in branch *$branch:*\n" + buildString {
            changesList.commits.forEach {
                append("        - Change ${it.revision} by ${it.developer} (details: ${it.webUrlWithDescription})\n")
            }
        }

        // File name on Artifactory is the same as current.
        val artifactoryFileName = currentBenchmarksReportFile.substringAfterLast("/")

        // Get compare to build.
        val compareToBuild = getBuild(previousBuildLocator(buildTypeId, defaultBranch), user, password)
        val compareToBuildLink = getBuildProperty(compareToBuild,"webUrl")
        val compareToBuildNumber = getBuildProperty(compareToBuild,"number")
        val target = System.getProperty("os.name").replace("\\s".toRegex(), "")

        // Generate comparison report.
        val output = arrayOf(analyzer, "-r", "html", currentBenchmarksReportFile, "artifactory:$compareToBuildNumber:$target:$artifactoryFileName", "-o", htmlReport)
                .runCommand()

        if (output.contains("Uncaught exception")) {
            error("Error during comparasion of $currentBenchmarksReportFile and " +
                    "artifactory:$compareToBuildNumber:$target:$artifactoryFileName with $analyzer! " +
                    "Please check files existance and their correctness.")
        }
        arrayOf("$analyzer", "-r", "statistics", "$currentBenchmarksReportFile", "artifactory:$compareToBuildNumber:$target:$artifactoryFileName", "-o", "$summaryFile")
                .runCommand()

        val reportLink = "https://kotlin-native-perf-summary.labs.jb.gg/?target=$target&build=$buildNumber"
        val detailedReportLink = "https://kotlin-native-performance.labs.jb.gg/?" +
                "report=artifactory:$buildNumber:$target:$artifactoryFileName&" +
                "compareTo=artifactory:$compareToBuildNumber:$target:$artifactoryFileName"

        val title = "\n*Performance report for target $target (build $buildNumber)* - $reportLink\n" +
                "*Detailed info - * $detailedReportLink"

        val header = "$title\n$changesInfo\n\nCompare to build $compareToBuildNumber: $compareToBuildLink\n\n"
        val footer = "*Benchmarks statistics:* $testReportUrl"
        val message = "$header\n$footer\n"

        // Send to channel or user directly.
        val session = SlackSessionFactory.createWebSocketSlackSession(buildProperties.getProperty("konan-reporter-token"))
        session.connect()

        if (branch == defaultBranch) {
            if (bundleBuild) {
                val channel = session.findChannelByName(buildProperties.getProperty("konan-channel-name"))
                session.sendMessage(channel, message)
            }
        }
        session.disconnect()
    }
}
