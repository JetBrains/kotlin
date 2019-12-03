/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.fileloggers.FileRecordLogger
import org.jetbrains.kotlin.statistics.fileloggers.IRecordLogger
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.fileloggers.NullRecordLogger
import org.jetbrains.kotlin.statistics.metrics.*
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BuildSessionLogger(
    private val rootPath: File,
    private val maxProfileFiles: Int = DEFAULT_MAX_PROFILE_FILES,
    private val maxFileSize: Long = DEFAULT_MAX_PROFILE_FILE_SIZE
) : IStatisticsValuesConsumer {

    companion object {
        const val STATISTICS_FOLDER_NAME = "kotlin-profile"
        const val STATISTICS_FILE_NAME_PATTERN = "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{3}.profile"

        private const val DEFAULT_MAX_PROFILE_FILES = 1_000
        private const val DEFAULT_MAX_PROFILE_FILE_SIZE = 100_000L
    }

    private val profileFileNameFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd-HH-mm-ss-SSS'.profile'")
    private val statisticsFolder: File = File(
        rootPath,
        STATISTICS_FOLDER_NAME
    ).also { it.mkdirs() }

    private var buildSession: BuildSession? = null
    private var trackingFile: IRecordLogger? = null

    private val metricsContainer = MetricsContainer()

    @Synchronized
    fun startBuildSession(buildSinceDaemonStart: Long, buildStartedTime: Long?) {
        report(NumericalMetrics.GRADLE_BUILD_NUMBER_IN_CURRENT_DAEMON, buildSinceDaemonStart)

        buildSession = BuildSession(buildStartedTime)
        initTrackingFile()
    }

    @Synchronized
    fun isBuildSessionStarted() = buildSession != null

    @Synchronized
    private fun closeTrackingFile() {
        metricsContainer.flush(trackingFile)
        trackingFile?.close()
        trackingFile = null
    }

    @Synchronized
    private fun initTrackingFile() {
        closeTrackingFile()

        // Get list of existing files. Try to create folder if possible, return from function if failed to create folder
        val fileCandidates =
            statisticsFolder.listFiles()?.filter { it.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()) }?.toMutableList()
                ?: if (statisticsFolder.mkdirs()) emptyList<File>() else return

        for (i in 0 until fileCandidates.size - maxProfileFiles) {
            val file2delete = fileCandidates[i]
            if (file2delete.isFile) {
                file2delete.delete()
            }
        }

        // emergency check. What if a lot of files are locked due to some reason
        if (statisticsFolder.listFiles()?.size ?: 0 > maxProfileFiles * 2) {
            return
        }

        fun newFile(): File = File(statisticsFolder, profileFileNameFormatter.format(LocalDateTime.now()))
        val lastFile = fileCandidates.lastOrNull() ?: newFile()

        trackingFile = try {
            if (lastFile.length() < maxFileSize) {
                FileRecordLogger(lastFile)
            } else {
                FileRecordLogger(newFile())
            }
        } catch (e: IOException) {
            try {
                FileRecordLogger(newFile())
            } catch (e: IOException) {
                NullRecordLogger()
            }
        }
    }

    @Synchronized
    fun finishBuildSession(action: String?, failure: Throwable?) {
        // nanotime could not be used as build start time in nanotime is unknown. As result, the measured duration
        // could be affected by system clock correction
        val finishTime = System.currentTimeMillis()
        buildSession?.also {
            if (it.buildStartedTime != null) {
                report(NumericalMetrics.GRADLE_BUILD_DURATION, finishTime - it.buildStartedTime)
            }
            report(NumericalMetrics.GRADLE_EXECUTION_DURATION, finishTime - it.projectEvaluatedTime)
        }
        buildSession = null
    }

    @Synchronized
    fun unlockJournalFile() {
        closeTrackingFile()
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        metricsContainer.report(metric, value, subprojectName)
    }

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?) {
        metricsContainer.report(metric, value, subprojectName)
    }

    override fun report(metric: StringMetrics, value: String, subprojectName: String?) {
        metricsContainer.report(metric, value, subprojectName)
    }
}
