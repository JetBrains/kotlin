/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MPPTools")

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.gradle.api.execution.TaskExecutionListener
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.report.*
import org.jetbrains.report.json.*
import java.nio.file.Paths
import java.io.File
import java.io.FileInputStream
import java.io.BufferedOutputStream
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/*
 * This file includes short-cuts that may potentially be implemented in Kotlin MPP Gradle plugin in the future.
 */

// Short-cuts for detecting the host OS.
@get:JvmName("isMacos")
val isMacos by lazy { hostOs == "Mac OS X" }

@get:JvmName("isWindows")
val isWindows by lazy { hostOs.startsWith("Windows") }

@get:JvmName("isLinux")
val isLinux by lazy { hostOs == "Linux" }

// Short-cuts for mostly used paths.
@get:JvmName("mingwPath")
val mingwPath by lazy { System.getenv("MINGW64_DIR") ?: "c:/msys64/mingw64" }

@get:JvmName("kotlinNativeDataPath")
val kotlinNativeDataPath by lazy {
    System.getenv("KONAN_DATA_DIR") ?: Paths.get(userHome, ".konan").toString()
}

// A short-cut for evaluation of the default host Kotlin/Native preset.
@JvmOverloads
fun defaultHostPreset(
    subproject: Project,
    whitelist: List<KotlinTargetPreset<*>> = listOf(subproject.kotlin.presets.macosX64, subproject.kotlin.presets.linuxX64, subproject.kotlin.presets.mingwX64)
): KotlinTargetPreset<*> {

    if (whitelist.isEmpty())
        throw Exception("Preset whitelist must not be empty in Kotlin/Native ${subproject.displayName}.")

    val presetCandidate = when {
        isMacos -> subproject.kotlin.presets.macosX64
        isLinux -> subproject.kotlin.presets.linuxX64
        isWindows -> subproject.kotlin.presets.mingwX64
        else -> null
    }

    return if (presetCandidate != null && presetCandidate in whitelist)
        presetCandidate
    else
        throw Exception("Host OS '$hostOs' is not supported in Kotlin/Native ${subproject.displayName}.")
}

fun getNativeProgramExtension(): String = when {
    isMacos -> ".kexe"
    isLinux -> ".kexe"
    isWindows -> ".exe"
    else -> error("Unknown host")
}

fun getKotlinNativeExecutable(target: KotlinTarget, buildType: String) =
        target.compilations.main.getBinary("EXECUTABLE", buildType).toString()

fun getFileSize(filePath: String): Long? {
    val file = File(filePath)
    return if (file.exists()) file.length() else null
}

fun getCodeSizeBenchmark(programName: String, filePath: String): BenchmarkResult {
    val codeSize = getFileSize(filePath)
    return BenchmarkResult("$programName.codeSize",
            codeSize?. let { BenchmarkResult.Status.PASSED } ?: run { BenchmarkResult.Status.FAILED },
            codeSize?.toDouble() ?: 0.0, codeSize?.toDouble() ?: 0.0, 1, 0)
}

// Create benchmarks json report based on information get from gradle project
fun createJsonReport(projectProperties: Map<String, Any>): String {
    fun getValue(key: String): String = projectProperties[key] as? String ?: "unknown"
    val machine = Environment.Machine(getValue("cpu"), getValue("os"))
    val jdk = Environment.JDKInstance(getValue("jdkVersion"), getValue("jdkVendor"))
    val env = Environment(machine, jdk)
    val flags = (projectProperties["flags"] ?: emptyList<String>()) as List<String>
    val backend = Compiler.Backend(Compiler.backendTypeFromString(getValue("type"))!! ,
                                    getValue("compilerVersion"), flags)
    val kotlin = Compiler(backend, getValue("kotlinVersion"))
    val benchDesc = getValue("benchmarks")
    val benchmarksArray = JsonTreeParser.parse(benchDesc)
    val benchmarks = BenchmarksReport.parseBenchmarksArray(benchmarksArray)
            .union(listOf<BenchmarkResult>(projectProperties["compileTime"] as BenchmarkResult,
                                            projectProperties["codeSize"] as BenchmarkResult)).toList()
    val report = BenchmarksReport(env, benchmarks, kotlin)
    return report.toJson()
}

// Find file with set name in directory.
fun findFile(fileName: String, directory: String): String? =
    File(directory).walkBottomUp().find { it.name == fileName }?.getAbsolutePath()

fun uploadFileToBintray(url: String, project: String, version: String, packageName: String, bintrayFilePath: String,
                        filePath: String, username: String? = null, password: String? = null) {
    val uploadUrl = "$url/$project/$packageName/$version/$bintrayFilePath?publish=1"
    sendUploadRequest(uploadUrl, filePath, username, password)
}

fun sendUploadRequest(url: String, fileName: String, username: String? = null, password: String? = null) {
    val uploadingFile = File(fileName)
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.doOutput = true
    connection.doInput = true
    connection.requestMethod = "PUT"
    connection.setRequestProperty("Content-type", "text/plain")
    if (username != null && password != null) {
        val auth = Base64.getEncoder().encode((username + ":" + password).toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
    }

    try {
        connection.connect()
        BufferedOutputStream(connection.outputStream).use { output ->
            BufferedInputStream(FileInputStream(uploadingFile)).use { input ->
                input.copyTo(output)
            }
        }
        val response = connection.responseMessage
        println("Upload request ended with ${connection.responseCode} - $response")
    } catch (t: Throwable) {
        error("Couldn't upload file $fileName to $url")
    }
}

// A short-cut to add a Kotlin/Native run task.
@JvmOverloads
fun createRunTask(
        subproject: Project,
        name: String,
        target: KotlinTarget,
        configureClosure: Closure<Any>? = null
): Task {
    val task = subproject.tasks.create(name, RunKotlinNativeTask::class.java, target)
    task.configure(configureClosure ?: task.emptyConfigureClosure())
    return task
}

fun getJvmCompileTime(programName: String): BenchmarkResult =
        TaskTimerListener.getBenchmarkResult(programName, listOf("compileKotlinMetadata", "jvmJar"))

fun getNativeCompileTime(programName: String): BenchmarkResult =
        TaskTimerListener.getBenchmarkResult(programName, listOf("compileKotlinNative", "linkMainReleaseExecutableNative"))

// Class time tracker for all tasks.
class TaskTimerListener: TaskExecutionListener {
    companion object {
        val tasksTimes = mutableMapOf<String, Double>()

        fun getBenchmarkResult(programName: String, tasksNames: List<String>): BenchmarkResult {
            val time = tasksNames.map { tasksTimes[it] ?: 0.0 }.sum()
            // TODO get this info from gradle plugin with exit code end stacktrace.
            val status = tasksNames.map { tasksTimes.containsKey(it) }.reduce { a, b -> a && b }
            return BenchmarkResult("$programName.compileTime",
                                    if (status) BenchmarkResult.Status.PASSED else BenchmarkResult.Status.FAILED,
                                    time, time, 1, 0)
        }

    }

    private var startTime = System.currentTimeMillis()

    override fun beforeExecute(task: Task) {
        startTime = System.nanoTime()
    }

     override fun afterExecute(task: Task, taskState: TaskState) {
         tasksTimes[task.name] = (System.nanoTime() - startTime) / 1000.0
     }
}

fun addTimeListener(subproject: Project) {
    subproject.gradle.addListener(TaskTimerListener())
}
