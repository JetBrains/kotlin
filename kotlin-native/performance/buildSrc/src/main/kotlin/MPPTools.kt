/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MPPTools")

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.gradle.api.execution.TaskExecutionListener
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.report.*
import org.jetbrains.report.json.*
import java.io.File

/*
 * This file includes short-cuts that may potentially be implemented in Kotlin MPP Gradle plugin in the future.
 */

fun getNativeProgramExtension(): String = when {
    PlatformInfo.isMac() -> ".kexe"
    PlatformInfo.isLinux() -> ".kexe"
    PlatformInfo.isWindows() -> ".exe"
    else -> error("Unknown host")
}

fun getFileSize(filePath: String): Long? {
    val file = File(filePath)
    return if (file.exists()) file.length() else null
}

fun getCodeSizeBenchmark(programName: String, filePath: String): BenchmarkResult {
    val codeSize = getFileSize(filePath)
    return BenchmarkResult(programName,
            codeSize?. let { BenchmarkResult.Status.PASSED } ?: run { BenchmarkResult.Status.FAILED },
            codeSize?.toDouble() ?: 0.0, BenchmarkResult.Metric.CODE_SIZE, codeSize?.toDouble() ?: 0.0, 1, 0)
}

// Create benchmarks json report based on information get from gradle project
fun createJsonReport(projectProperties: Map<String, Any>): String {
    fun getValue(key: String): String = projectProperties[key] as? String ?: "unknown"
    val machine = Environment.Machine(getValue("cpu"), getValue("os"))
    val jdk = Environment.JDKInstance(getValue("jdkVersion"), getValue("jdkVendor"))
    val env = Environment(machine, jdk)
    val flags: List<String> = (projectProperties["flags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val backend = Compiler.Backend(Compiler.backendTypeFromString(getValue("type"))!! ,
                                    getValue("compilerVersion"), flags)
    val kotlin = Compiler(backend, getValue("kotlinVersion"))
    val benchDesc = getValue("benchmarks")
    val benchmarksArray = JsonTreeParser.parse(benchDesc)
    val benchmarks = parseBenchmarksArray(benchmarksArray)
            .union((projectProperties["compileTime"] as? List<*>)?.filterIsInstance<BenchmarkResult>() ?: emptyList()).union(
                    listOf(projectProperties["codeSize"] as? BenchmarkResult).filterNotNull()).toList()
    val report = BenchmarksReport(env, benchmarks, kotlin)
    return report.toJson()
}

fun mergeReports(reports: List<File>): String {
    val reportsToMerge = reports.filter { it.exists() }.map {
        val json = it.inputStream().bufferedReader().use { it.readText() }
        val reportElement = JsonTreeParser.parse(json)
        BenchmarksReport.create(reportElement)
    }
    val structuredReports = mutableMapOf<String, MutableList<BenchmarksReport>>()
    reportsToMerge.map { it.compiler.backend.flags.joinToString() to it }.forEach {
        structuredReports.getOrPut(it.first) { mutableListOf<BenchmarksReport>() }.add(it.second)
    }
    val jsons = structuredReports.map { (_, value) -> value.reduce { result, it -> result + it }.toJson() }
    return when(jsons.size) {
        0 -> ""
        1 -> jsons[0]
        else -> jsons.joinToString(prefix = "[", postfix = "]")
    }
}

fun getCompileOnlyBenchmarksOpts(project: Project, defaultCompilerOpts: List<String>): List<String> {
    val dist = project.file(project.findProperty("kotlin.native.home") ?: "dist")
    val useCache = !project.hasProperty("disableCompilerCaches")
    val cacheOption = "-Xcache-directory=$dist/klib/cache/${HostManager.host.name}-gSTATIC-system"
            .takeIf { useCache && !PlatformInfo.isWindows() } // TODO: remove target condition when we have cache support for other targets.
    return (project.findProperty("nativeBuildType") as String?)?.let {
        if (it.equals("RELEASE", true))
            listOf("-opt")
        else if (it.equals("DEBUG", true))
            listOfNotNull("-g", cacheOption)
        else listOf()
    } ?: defaultCompilerOpts + listOfNotNull(cacheOption?.takeIf { !defaultCompilerOpts.contains("-opt") })
}

fun getCompileBenchmarkTime(subproject: Project,
                            programName: String, tasksNames: Iterable<String>,
                            repeats: Int, exitCodes: Map<String, Int>) =
    (1..repeats).map { number ->
        var time = 0.0
        var status = BenchmarkResult.Status.PASSED
        tasksNames.forEach {
            time += TaskTimerListener.getTimerListenerOfSubproject(subproject).getTime("$it$number")
            status = if (exitCodes["$it$number"] != 0) BenchmarkResult.Status.FAILED else status
        }

        BenchmarkResult(programName, status, time, BenchmarkResult.Metric.COMPILE_TIME, time, number, 0)
    }.toList()

// Class time tracker for all tasks.
class TaskTimerListener: TaskExecutionListener {
    companion object {
        internal val timerListeners = mutableMapOf<String, TaskTimerListener>()

        internal fun getTimerListenerOfSubproject(subproject: Project) =
                timerListeners[subproject.name] ?: error("TimeListener for project ${subproject.name} wasn't set")
    }

    val tasksTimes = mutableMapOf<String, Double>()

    fun getTime(taskName: String) = tasksTimes[taskName] ?: 0.0

    private var startTime = System.nanoTime()

    override fun beforeExecute(task: Task) {
        startTime = System.nanoTime()
    }

     override fun afterExecute(task: Task, taskState: TaskState) {
         tasksTimes[task.name] = (System.nanoTime() - startTime) / 1000.0
     }
}

fun addTimeListener(subproject: Project) {
    val listener = TaskTimerListener()
    TaskTimerListener.timerListeners.put(subproject.name, listener)
    subproject.gradle.addListener(listener)
}
