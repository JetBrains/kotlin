/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.walkDaemons
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import java.nio.file.Files
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*

internal abstract class KotlinDaemonStatisticsService : BuildService<KotlinDaemonStatisticsService.Parameters>, AutoCloseable {
    internal interface Parameters : BuildServiceParameters {
        val rootBuildDir: DirectoryProperty
    }

    private class DaemonInfo(
        val displayName: String?,
        val kotlinVersion: String?,
        val usedMemory: Long?,
        val maxMemory: Long?,
        val rootBuildDir: File?,
        val totalGcTime: Long?
    )

    private class DaemonsStatistics(
        val daemonsInfo: List<DaemonInfo>,
        val ts: String
    )

    private val logger: Logger = Logging.getLogger(KotlinDaemonStatisticsService::class.java)
    private val currentTs
        get() = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
    private val startTs = currentTs

    init {
        KotlinCompilerClient.startedDaemons.clear()
    }

    private val Long.asMemoryString: String
        get() {
            val absB = if (this == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(this)
            if (absB < 1024) {
                return "$this B"
            }
            var value = absB
            val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
            var i = 40
            while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
                value = value shr 10
                ci.next()
                i -= 10
            }
            value *= java.lang.Long.signum(this).toLong()
            return java.lang.String.format("%.1f %ciB", value / 1024.0, ci.current())
        }

    private fun <T> CompileService.CallResult<T>.getOrNull() = if (isGood) {
        get()
    } else {
        null
    }

    private val humanizedMemorySizeRegex = "(\\d+)([kmg]?)".toRegex()

    private fun String.memToBytes(): Long? =
        humanizedMemorySizeRegex
            .matchEntire(this.trim().toLowerCase())
            ?.groups?.let { match ->
                match[1]?.value?.let {
                    it.toLong() *
                            when (match[2]?.value) {
                                "k" -> 1 shl 10
                                "m" -> 1 shl 20
                                "g" -> 1 shl 30
                                else -> 1
                            }
                }
            }

    private val statisticsBeforeBuild = collectDaemonStatistics(startTs)

    override fun close() {
        val statisticsAfterBuild = collectDaemonStatistics(currentTs)
        writeDaemonsReport(statisticsBeforeBuild, statisticsAfterBuild)
    }

    private fun collectDaemonStatistics(ts: String): DaemonsStatistics {
        val registryDir = File(COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH)
        val daemons = walkDaemons(
            registryDir,
            "[a-zA-Z0-9]*",
            Files.createTempFile(registryDir.toPath(), "kotlin-daemon-client-tsmarker", null).toFile()
        )
        val daemonsInfo = daemons
            .map { (daemon, _, _) ->
                val kotlinVersion = try {
                    daemon.getKotlinVersion().getOrNull()
                } catch (e: Exception) {
                    null
                }
                val totalGcTime = try {
                    daemon.getTotalGcTime().get()
                } catch (e: Exception) {
                    null
                }
                val rootDir = try {
                    daemon.getDaemonOptions().get().rootBuildDir
                } catch (e: Exception) {
                    null
                }
                val daemonInfo = daemon.getDaemonInfo().getOrNull()
                val usedMemory = daemon.getUsedMemory().getOrNull()
                val maxMemory = daemon.getDaemonJVMOptions().getOrNull()?.maxMemory?.memToBytes()
                DaemonInfo(daemonInfo, kotlinVersion, usedMemory, maxMemory, rootDir, totalGcTime)
            }.toList()

        return DaemonsStatistics(daemonsInfo, ts)
    }

    private fun writeDaemonsReport(statisticsBefore: DaemonsStatistics, statisticsAfter: DaemonsStatistics) {
        val reportDir = parameters.rootBuildDir.get().dir("build/reports/kotlin-build")
        reportDir.asFile.mkdirs()
        val reportFile = reportDir.file("daemons-report-$startTs.txt").asFile
        reportFile.writeText(
            buildString {
                appendLine("Kotlin daemons running at the start of the build:")
                appendDaemonsStatistics(statisticsBefore)
                appendLine("Kotlin daemons running at the end of the build:")
                appendDaemonsStatistics(statisticsAfter)
            }
        )
        logger.warn("Kotlin daemon report is written to $reportFile")
    }

    private fun StringBuilder.appendDaemonsStatistics(daemonInfo: DaemonsStatistics) {
        appendLine("    Timestamp: ${daemonInfo.ts}")
        daemonInfo.daemonsInfo.forEach {
            val startedByThisBuilds = KotlinCompilerClient.startedDaemons.contains(it.displayName)
            appendLine(
                """
                |    Daemon: ${it.displayName} ${if (startedByThisBuilds) "<---- started by this build" else ""}
                |        Started to build project: ${it.rootBuildDir ?: "Unknown"}
                |        Kotlin version: ${it.kotlinVersion ?: "Unknown"}
                |        Used memory: ${it.usedMemory?.let { mem -> "${mem.asMemoryString} ($mem bytes)" } ?: "Unknown"}
                |        Max memory: ${it.maxMemory?.let { mem -> "${mem.asMemoryString} ($mem bytes)" } ?: "Unknown"}
                |        GC time since daemon start: ${(it.totalGcTime?.let { gcTime -> "$gcTime ms" }) ?: "Unknown"}
                |    
                """.trimMargin()
            )
        }
    }
}