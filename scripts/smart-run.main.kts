#!/usr/bin/env kotlin

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Run from the repository checkout with: kotlin scripts/smart-run.main.kts
 */

import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.readLines
import kotlin.system.exitProcess

private val teamCityServerUrl = "https://buildserver.labs.intellij.net"
private val buildConfigurationPrefix = "Kotlin_KotlinDev_Domain_"
private val domainPattern = Regex("[A-Za-z][A-Za-z0-9]*")

private data class CommandResult(val exitCode: Int, val output: String, val error: String)

private interface RunningCommand {
    fun waitFor(): CommandResult
    fun destroy()
}

private class ProcessCommandRunner(private val workingDirectory: Path) {
    fun run(executable: String, arguments: List<String>, environment: Map<String, String> = emptyMap(), input: String? = null): CommandResult {
        return start(executable, arguments, environment, input).waitFor()
    }

    fun start(
        executable: String,
        arguments: List<String>,
        environment: Map<String, String> = emptyMap(),
        input: String? = null,
    ): RunningCommand {
        val command = platformCommand(executable, arguments)
        val process = ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .apply { environment().putAll(environment) }
            .start()

        if (input == null) {
            process.outputStream.close()
        } else {
            process.outputStream.bufferedWriter().use { it.write(input) }
        }

        val output = CompletableFuture.supplyAsync { process.inputStream.use { it.readAllBytes().decodeToString() } }
        val error = CompletableFuture.supplyAsync { process.errorStream.use { it.readAllBytes().decodeToString() } }
        return object : RunningCommand {
            override fun waitFor(): CommandResult {
                val exitCode = process.waitFor()
                return CommandResult(exitCode, output.get(), error.get())
            }

            override fun destroy() {
                process.destroy()
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }

    private fun platformCommand(executable: String, arguments: List<String>): List<String> {
        val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val isBatchFile = executable.endsWith(".bat", ignoreCase = true) || executable.endsWith(".cmd", ignoreCase = true)
        return if (isWindows && isBatchFile) {
            listOf(System.getenv("COMSPEC") ?: "cmd.exe", "/d", "/c", executable) + arguments
        } else {
            listOf(executable) + arguments
        }
    }
}

private data class DomainBuild(
    val domain: String,
    var id: String = "-",
    var status: String = "START ERROR",
    var url: String = "-",
    @Volatile var finished: Boolean = true,
    @Volatile var watcher: RunningCommand? = null,
)

private class SmartRun(
    private val projectRoot: Path,
    private val output: PrintStream = System.out,
    private val error: PrintStream = System.err,
) {
    private val runner = ProcessCommandRunner(projectRoot)
    private val gradleExecutable = System.getenv("SMART_RUN_GRADLE") ?: projectRoot.resolve(
        if (isWindows) "gradlew.bat" else "gradlew"
    ).absolutePathString()
    private val gitExecutable = System.getenv("SMART_RUN_GIT") ?: "git"
    private val teamCityExecutable = System.getenv("SMART_RUN_TEAMCITY") ?: "teamcity"
    private val teamCityEnvironment = mapOf("TEAMCITY_URL" to teamCityServerUrl)
    private val builds = Collections.synchronizedList(mutableListOf<DomainBuild>())
    private val cancellationStarted = AtomicBoolean(false)
    private val completed = AtomicBoolean(false)

    private val isWindows: Boolean
        get() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    fun run(): Int {
        val domains = inferAffectedDomains() ?: return 1
        if (domains.isEmpty()) {
            output.println("No affected domains found; no TeamCity builds were started.")
            return 0
        }

        val revision = runCommand(gitExecutable, listOf("rev-parse", "--verify", "HEAD"))
        if (revision.exitCode != 0 || revision.output.trim().isEmpty()) {
            error.println("Error: Could not determine the current Git HEAD.")
            printCommandError(revision)
            return 1
        }
        val headRevision = revision.output.trim()
        val currentBranchResult = runCommand(gitExecutable, listOf("symbolic-ref", "--quiet", "--short", "HEAD"))
        val currentBranch = currentBranchResult.output.trim().takeIf { currentBranchResult.exitCode == 0 && it.isNotEmpty() }
        val teamCityBranch = when {
            currentBranch == null -> "smart/detached-${headRevision.take(12)}"
            currentBranch.startsWith("smart/") -> currentBranch
            else -> "smart/$currentBranch"
        }
        val pushRemote = resolvePushRemote(currentBranch) ?: return 1

        output.println("Pushing HEAD $headRevision to $pushRemote/$teamCityBranch.")
        val push = runCommand(gitExecutable, listOf("push", pushRemote, "HEAD:refs/heads/$teamCityBranch"))
        printCommandOutput(push)
        if (push.exitCode != 0) {
            error.println("Error: Could not push HEAD to $pushRemote/$teamCityBranch; no TeamCity builds were started.")
            return 1
        }

        val shutdownHook = Thread({ cancelUnfinishedBuilds() }, "smart-run-cancellation")
        Runtime.getRuntime().addShutdownHook(shutdownHook)
        return try {
            startAndWatchBuilds(domains, teamCityBranch, headRevision)
        } catch (failure: InterruptedException) {
            cancelUnfinishedBuilds()
            Thread.currentThread().interrupt()
            130
        } catch (failure: Exception) {
            error.println("Error: Smart run failed: ${failure.message ?: failure::class.java.simpleName}")
            cancelUnfinishedBuilds()
            1
        } finally {
            completed.set(true)
            runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
        }
    }

    private fun inferAffectedDomains(): List<String>? {
        val gradle = runCommand(
            gradleExecutable,
            listOf("inferAffectedDomains", "-Ptest.federation.enabled=true"),
        )
        printCommandOutput(gradle)
        if (gradle.exitCode != 0) {
            error.println("Error: inferAffectedDomains failed.")
            return null
        }

        val affectedDomainsFile = projectRoot.resolve(".test-federation.affected-domains.txt")
        if (!Files.isRegularFile(affectedDomainsFile)) {
            error.println("Error: inferAffectedDomains did not create $affectedDomainsFile.")
            return null
        }
        return affectedDomainsFile.readLines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .onEach { domain ->
                if (!domainPattern.matches(domain)) {
                    throw IllegalArgumentException("inferAffectedDomains produced an invalid domain: $domain")
                }
            }
    }

    private fun resolvePushRemote(currentBranch: String?): String? {
        val candidates = buildList {
            if (currentBranch != null) add(gitConfig("branch.$currentBranch.pushRemote"))
            add(gitConfig("remote.pushDefault"))
            if (currentBranch != null) add(gitConfig("branch.$currentBranch.remote"))
        }.filterNotNull()

        candidates.firstOrNull()?.let { candidate ->
            if (candidate == ".") {
                error.println("Error: The current branch is configured to push to the local repository.")
                return null
            }
            if (remoteExists(candidate)) return candidate
            error.println("Error: The configured Git push remote '$candidate' does not exist.")
            return null
        }

        if (remoteExists("origin")) return "origin"
        val remotes = runCommand(gitExecutable, listOf("remote"))
        if (remotes.exitCode == 0) {
            remotes.output.lineSequence().map(String::trim).filter(String::isNotEmpty).toList().singleOrNull()?.let { return it }
        }
        error.println("Error: Could not determine which Git remote should receive the smart branch.")
        return null
    }

    private fun gitConfig(key: String): String? {
        val result = runCommand(gitExecutable, listOf("config", "--get", key))
        return result.output.trim().takeIf { result.exitCode == 0 && it.isNotEmpty() }
    }

    private fun remoteExists(remote: String): Boolean {
        return runCommand(gitExecutable, listOf("remote", "get-url", remote)).exitCode == 0
    }

    private fun startAndWatchBuilds(domains: List<String>, branch: String, revision: String): Int {
        output.println("Starting ${domains.size} affected domain build(s) on $teamCityServerUrl.")
        var failed = false
        domains.forEach { domain ->
            val build = DomainBuild(domain)
            builds += build
            val result = runCommand(
                teamCityExecutable,
                listOf(
                    "--no-input", "--no-color", "run", "start", "$buildConfigurationPrefix$domain",
                    "--branch", branch, "--revision", revision, "--no-push", "--json",
                ),
                teamCityEnvironment,
            )
            val buildId = result.output.jsonNumber("id")
            if (result.exitCode == 0 && buildId != null) {
                build.id = buildId
                build.status = "QUEUED"
                build.url = result.output.jsonValue("webUrl") ?: "-"
                build.finished = false
            } else {
                error.println("Error: Failed to start the $domain domain build.")
                printCommandError(result)
                failed = true
            }
        }

        buildSnapshot().filter { !it.finished }.forEach { build ->
            build.watcher = runner.start(
                teamCityExecutable,
                listOf("--no-input", "--no-color", "run", "watch", build.id, "--json"),
                teamCityEnvironment,
            )
            build.status = "WATCHING"
        }

        output.println()
        output.println("Build status (updates are printed without redrawing the terminal):")
        printStatusTable()

        buildSnapshot().forEach { build ->
            val watcher = build.watcher ?: return@forEach
            val result = watcher.waitFor()
            build.watcher = null
            val finalStatus = result.output.jsonValue("status")
            build.status = when {
                !finalStatus.isNullOrEmpty() && finalStatus != "UNKNOWN" -> finalStatus
                result.exitCode == 0 -> "FINISHED"
                else -> "WATCH ERROR"
            }
            result.output.jsonValue("webUrl")?.let { build.url = it }
            build.finished = true
            if (result.exitCode != 0 || build.status != "SUCCESS") failed = true
            output.printf("Completed %s  %s%n", build.domain, build.status)
            printCommandError(result)
        }

        output.println()
        output.println("Final build status:")
        printStatusTable()
        return if (failed) {
            error.println("One or more affected domain builds failed.")
            1
        } else {
            output.println("All affected domain builds completed successfully.")
            0
        }
    }

    private fun cancelUnfinishedBuilds() {
        if (completed.get() || !cancellationStarted.compareAndSet(false, true)) return
        val unfinished = buildSnapshot().filter { !it.finished && it.id != "-" }
        if (unfinished.isEmpty()) return

        unfinished.forEach { it.watcher?.destroy() }
        error.println()
        error.println("Cancelling unfinished TeamCity builds...")
        unfinished.forEach { build ->
            val cancellation = runCommand(
                teamCityExecutable,
                listOf("--no-input", "--no-color", "run", "cancel", build.id, "--yes", "--comment", "Cancelled by smart-run"),
                teamCityEnvironment,
            )
            if (cancellation.exitCode == 0) {
                error.println("  Cancelled ${build.domain} build ${build.id}.")
            } else if (cancelBuildViaApi(build.id)) {
                error.println("  Cancelled ${build.domain} build ${build.id} using the REST fallback.")
            } else {
                error.println("  Warning: Could not cancel ${build.domain} build ${build.id}.")
                printCommandError(cancellation, "    ")
            }
        }
    }

    private fun cancelBuildViaApi(buildId: String): Boolean {
        val build = runCommand(
            teamCityExecutable,
            listOf("--no-input", "--no-color", "api", "/app/rest/builds/id:$buildId?fields=id,state", "--raw"),
            teamCityEnvironment,
        )
        if (build.exitCode != 0) return false
        val endpoint = when (build.output.jsonValue("state")) {
            "finished" -> return true
            "queued" -> "/app/rest/buildQueue/id:$buildId"
            "running" -> "/app/rest/builds/id:$buildId"
            else -> return false
        }
        val request = "{\"comment\":\"Cancelled by smart-run\",\"readdIntoQueue\":false}\n"
        return runCommand(
            teamCityExecutable,
            listOf("--no-input", "--no-color", "api", endpoint, "-X", "POST", "--input", "-", "--silent"),
            teamCityEnvironment,
            request,
        ).exitCode == 0
    }

    private fun printStatusTable() {
        val snapshot = buildSnapshot()
        val domainWidth = maxOf(6, snapshot.maxOfOrNull { it.domain.length } ?: 6)
        output.println("${"Domain".padEnd(domainWidth)} | ${"Build ID".padEnd(10)} | ${"Status".padEnd(12)} | URL")
        output.println("${"-".repeat(domainWidth)}-+-${"-".repeat(10)}-+-${"-".repeat(12)}-+----")
        snapshot.forEach { build ->
            output.println("${build.domain.padEnd(domainWidth)} | ${build.id.padEnd(10)} | ${build.status.padEnd(12)} | ${build.url}")
        }
    }

    private fun buildSnapshot(): List<DomainBuild> = synchronized(builds) { builds.toList() }

    private fun runCommand(
        executable: String,
        arguments: List<String>,
        environment: Map<String, String> = emptyMap(),
        input: String? = null,
    ): CommandResult {
        return try {
            runner.run(executable, arguments, environment, input)
        } catch (failure: Exception) {
            CommandResult(1, "", "${failure.message ?: failure::class.java.simpleName}\n")
        }
    }

    private fun printCommandOutput(result: CommandResult) {
        if (result.output.isNotEmpty()) output.print(result.output)
        printCommandError(result)
    }

    private fun printCommandError(result: CommandResult, prefix: String = "") {
        result.error.lineSequence().filter(String::isNotEmpty).forEach { error.println("$prefix$it") }
    }
}

private fun String.jsonNumber(key: String): String? {
    return Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*([0-9]+)").find(this)?.groupValues?.get(1)
}

private fun String.jsonValue(key: String): String? {
    return Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(this)?.groupValues?.get(1)
        ?: jsonNumber(key)
}

private val projectRoot = __FILE__.toPath().toAbsolutePath().normalize().parent.parent
private val exitCode = try {
    SmartRun(projectRoot).run()
} catch (failure: IllegalArgumentException) {
    System.err.println("Error: ${failure.message}")
    1
}
exitProcess(exitCode)