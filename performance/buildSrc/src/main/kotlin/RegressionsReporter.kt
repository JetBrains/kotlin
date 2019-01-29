/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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

// Run command line from string.
fun String.runCommand(workingDir: File = File("."),
                      timeoutAmount: Long = 60,
                      timeoutUnit: TimeUnit = TimeUnit.SECONDS): String {
    return try {
        ProcessBuilder(*this.split("\\s".toRegex()).toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start().apply {
                    waitFor(timeoutAmount, timeoutUnit)
                }.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        error("Couldn't run command $this")
    }
}

data class Commit(val revision: String, val developer: String, val webUrlWithDescription: String)

// List of commits.
class CommitsList(data: JsonElement): ConvertedFromJson {

    val commits: List<Commit>

    init {
        if (data !is JsonObject) {
            error("Commits description is expected to be a json object!")
        }
        val changesElement = data.getOptionalField("change")
        commits = changesElement?.let {
            if (changesElement !is JsonArray) {
                error("Change field is expected to be an array. Please, check source.")
            }
           changesElement.jsonArray.map {
                with(it as JsonObject) {
                    Commit(elementToString(getRequiredField("version"), "version"),
                            elementToString(getRequiredField("username"), "username"),
                            elementToString(getRequiredField("webUrl"), "webUrl")
                    )
                }
           }
        } ?: listOf<Commit>()
    }
}

/**
 * Task to produce regressions report and send it to slack. Requires a report with current benchmarks result
 * and path to analyzer tool
 *
 * @property currentBenchmarksReportFile  path to file with becnhmarks result
 * @property analyzer path to analyzer tool
 * @property htmlReport name of result html report
 * @property defaultBranch name of defaukt branch
 */
open class RegressionsReporter : DefaultTask() {

    val teamCityUrl = "http://buildserver.labs.intellij.net"
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

    private fun tabUrl(buildId: String, buildTypeId: String, tab: String) =
            "$teamCityUrl/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId&tab=$tab"

    private fun testReportUrl(buildId: String, buildTypeId: String) =
            tabUrl(buildId, buildTypeId, "testsInfo")

    private fun buildsUrl(buildLocator: String) =
            "$teamCityUrl/app/rest/builds/?locator=$buildLocator"

    private fun previousBuildLocator(buildTypeId: String, branchName: String) =
            "buildType:id:$buildTypeId,branch:name:$branchName,status:SUCCESS,state:finished,count:1"

    private fun changesListUrl(buildLocator: String) =
            "$teamCityUrl/app/rest/changes/?locator=build:$buildLocator"

    private fun sendGetRequest(url: String, username: String? = null, password: String? = null) : String {
        val connection = URL(url).openConnection() as HttpURLConnection
        if (username != null && password != null) {
            val auth = Base64.getEncoder().encode((username + ":" + password).toByteArray()).toString(Charsets.UTF_8)
            connection.addRequestProperty("Authorization", "Basic $auth")
        }
        connection.setRequestProperty("Accept", "application/json");
        connection.connect()
        return connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
    }

    private fun getBuild(buildLocator: String, user: String, password: String) =
            try {
                sendGetRequest(buildsUrl(buildLocator), user, password)
            } catch (t: Throwable) {
                error("Try to get build! TeamCity is unreachable!")
            }

    private fun getBuildProperty(buildJsonDescription: String, property: String) =
            with(JsonTreeParser.parse(buildJsonDescription) as JsonObject) {
                if (getPrimitive("count").int == 0) {
                    error("No build information on TeamCity for $buildJsonDescription!")
                }
                (getArray("build").getObject(0).getPrimitive(property) as JsonLiteral).unquoted()
            }

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

        val buildProperties = Properties()
        buildProperties.load(FileInputStream(teamcityConfig))
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
        val builds = getBuild(previousBuildLocator(buildTypeId,branch), user, password)
        val previousBuildsExist = (JsonTreeParser.parse(builds) as JsonObject).getPrimitive("count").int != 0

        // Get changes description.
        val changesList = getCommits("id:$buildId", user, password)
        val changesInfo = "*Changes* in branch *$branch:*\n" + buildString {
            changesList.commits.forEach {
                append("        - Change ${it.revision} by <@${it.developer}> (details: ${it.webUrlWithDescription})\n")
            }
        }

        println("defaul Branch: $defaultBranch")

        // If branch differs from default and it's first build compare to master, otherwise compare to previous build on branch.
        val compareToBranch = if (previousBuildsExist) branch else defaultBranch

        // File name on bintray is the same as current.
        val bintrayFileName = currentBenchmarksReportFile.substringAfterLast("/")

        // Get compare to build.
        val compareToBuild = getBuild(previousBuildLocator(buildTypeId, compareToBranch), user, password)
        val compareToBuildLink = getBuildProperty(compareToBuild,"webUrl")
        val compareToBuildNumber = getBuildProperty(compareToBuild,"number")
        val target = System.getProperty("os.name").replace("\\s".toRegex(), "")

        // Generate comparison report.
        val output = "$analyzer -r html $currentBenchmarksReportFile bintray:$compareToBuildNumber:$target:$bintrayFileName -o $htmlReport"
                .runCommand()

        if (output.contains("Uncaught exception")) {
            error("Error during comparasion of $currentBenchmarksReportFile and " +
                    "bintray:$compareToBuildNumber:$target:$bintrayFileName with $analyzer! " +
                    "Please check files existance and their correctness.")
        }

        val reportLink = "http://kotlin-native-performance.labs.jb.gg/?" +
                "report=bintray:$buildNumber:$target:$bintrayFileName&" +
                "compareTo=bintray:$compareToBuildNumber:$target:$bintrayFileName"

        val title = "\n*Performance report for target $target (build $buildNumber)* - $reportLink\n"
        val header = "$title\n$changesInfo\n\nCompare to build $compareToBuildNumber: $compareToBuildLink\n\n"
        val footer = "*Benchmarks statistics:* $testReportUrl"
        val message = "$header\n$footer\n"

        // Send to channel or user directly.
        val session = SlackSessionFactory.createWebSocketSlackSession(buildProperties.getProperty("konan-reporter-token"))
        session.connect()

        if (branch == defaultBranch) {
            val channel = session.findChannelByName(buildProperties.getProperty("konan-channel-name"))
            session.sendMessage(channel, message)
        } else {
            changesList.commits.filter { it.developer in slackUsers }. map { it.developer }
                    .toSet().forEach {
                        val slackUser = session.findUserByUserName(slackUsers[it])
                        session.sendMessageToUser(slackUser, message, null)

                    }
        }
        session.disconnect()
    }
}