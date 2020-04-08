/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers.async

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.perf.profilers.ProfilerHandler
import org.jetbrains.kotlin.idea.testFramework.logMessage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal class AsyncProfilerHandler : ProfilerHandler {

    private val asyncProfiler: Any

    private var profilingStarted = false
    private var profilingOptions: List<String>? = null

    init {
        val asyncLibPath = asyncLib().absolutePath
        val getInstanceMethod = asyncLibClass.getMethod("getInstance", String::class.java)

        // use reflection to exclude compile time dependency on async profiler that has not been published to maven yet
        asyncProfiler = getInstanceMethod.invoke(null, asyncLibPath) as Any

        logMessage { "asyncProfiler successfully loaded" }
    }

    private fun execute(command: String) {
        logMessage { "asyncProfiler command: '$command'" }
        executeMethod.invoke(asyncProfiler, command)
    }

    override fun startProfiling(activityName: String, options: List<String>) {
        try {
            profilingOptions = options
            execute(AsyncProfilerCommandBuilder.buildStartCommand(options))
            profilingStarted = true
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String>) {
        val combinedOptions = ArrayList(options)
        val commandBuilder = AsyncProfilerCommandBuilder(snapshotsPath)
        val name = activityName.replace(' ', '_').replace('/', '_')
        val stopAndDumpCommands = commandBuilder.buildStopAndDumpCommands(name, combinedOptions)
        for (stopCommand in stopAndDumpCommands) {
            execute(stopCommand)
        }
        profilingStarted = false
    }

    companion object {
        const val AGENT_FILE_NAME = "libasyncProfiler.so"
        private val asyncLibClass: Class<*> = Class.forName("one.profiler.AsyncProfiler")!!
        private val executeMethod = asyncLibClass.getMethod("execute", String::class.java)
        private var extractedFile: File? = null

        private fun asyncLib(): File {
            if (extractedFile == null) {
                extractedFile = File("${System.getenv("ASYNC_PROFILER_HOME")}/build/$AGENT_FILE_NAME")
            }
            if (extractedFile == null || !extractedFile!!.exists()) {
                val extracted = FileUtil.createTempFile("extracted_$AGENT_FILE_NAME", null, true)
                val osName = if (SystemInfo.isLinux) "linux" else "macos"
                val inputStream = asyncLibClass.getResourceAsStream("/binaries/$osName/$AGENT_FILE_NAME")
                Files.copy(inputStream, extracted.toPath(), StandardCopyOption.REPLACE_EXISTING)
                extractedFile = extracted
            }
            return extractedFile!!
        }

    }

}

class AsyncProfilerCommandBuilder(private val snapshotsPath: String) {
    fun buildStopAndDumpCommands(activityName: String, options: List<String>): List<String> {
        if (options.isEmpty()) {
            val dumpFileName = getDumpFileName(activityName, "svg")
            return listOf(buildCommand("stop", listOf("svg", "file=$dumpFileName")))
        } else {
            val dumpOptions = ArrayList<String>()
            val profilingOptions = ArrayList<String>()
            for (option in options) {
                val myDumpOptions = listOf("summary", "traces", "flat", "jfr", "collapsed", "svg", "tree")
                val trimmedOption = option.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
                if (myDumpOptions.contains(trimmedOption)) {
                    dumpOptions.add(option + ",file=" + getDumpFileName(activityName, trimmedOption))
                } else {
                    profilingOptions.add(option)
                }
            }
            val stopCommands = ArrayList<String>()
            for (option in dumpOptions) {
                stopCommands.add(
                    buildCommand(
                        "stop",
                        mergeParameters(
                            profilingOptions,
                            listOf(*option.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                        )
                    )
                )
            }
            return stopCommands
        }
    }

    private fun getDumpFileName(activityName: String, profilingType: String): String {
        return "$snapshotsPath${File.separator}$activityName" + when (profilingType) {
            "jfr" -> ".jfr"
            "svg" -> ".svg"
            "tree" -> ".html"
            else -> ".txt"
        }
    }

    companion object {

        private val DEFAULT_PROFILING_OPTIONS: List<String> = listOf("event=cpu", "framebuf=10000000", "interval=1000000")

        fun buildStartCommand(options: List<String>): String {
            return if (options.isEmpty()) {
                buildCommand("start", DEFAULT_PROFILING_OPTIONS)
            } else {
                buildCommand("start", mergeParameters(options, DEFAULT_PROFILING_OPTIONS))
            }
        }

        private fun buildCommand(initialCommand: String, options: List<String>): String {
            val builder = StringBuilder(initialCommand)
            for (option in options) {
                builder.append(",").append(option)
            }
            return builder.toString()
        }

        private fun parseParametersList(options: List<String>): Map<String, String> {
            return options.map { option ->
                option.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            }.flatMap { it.toList() }.map { it to it }.toMap()
        }

        private fun mergeParameters(options: List<String>, defaultOptions: List<String>): List<String> {
            val userOptions = parseParametersList(options)
            val defOptions = parseParametersList(defaultOptions)
            val mergedOptions = HashMap(defOptions)
            mergedOptions.putAll(userOptions)
            return ContainerUtil.map(mergedOptions.keys) { key ->
                if (mergedOptions[key]?.isEmpty() == true)
                    key
                else
                    key + "=" + mergedOptions[key]
            }
        }
    }
}