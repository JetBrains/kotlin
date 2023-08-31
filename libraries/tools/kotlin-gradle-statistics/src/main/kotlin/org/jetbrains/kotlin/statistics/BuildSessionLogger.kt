/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.fileloggers.FileRecordLogger
import org.jetbrains.kotlin.statistics.fileloggers.IRecordLogger
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BuildSessionLogger(
    rootPath: File,
    private val maxProfileFiles: Int = DEFAULT_MAX_PROFILE_FILES,
    private val maxFileAge: Long = DEFAULT_MAX_FILE_AGE,
    private val forceValuesValidation: Boolean = false,
) : StatisticsValuesConsumer {

    companion object {
        const val STATISTICS_FOLDER_NAME = "kotlin-profile"
        const val STATISTICS_FILE_NAME_PATTERN = "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{3}(.\\d+)?.profile"

        private const val DEFAULT_MAX_PROFILE_FILES = 1_000
        private const val DEFAULT_MAX_FILE_AGE = 30 * 24 * 3600 * 1000L //30 days

        fun listProfileFiles(statisticsFolder: File): List<File>? {
            return statisticsFolder.listFiles()?.filterTo(ArrayList()) { it.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()) }?.sorted()
        }
    }

    private val profileFileNameFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd-HH-mm-ss-SSS")

    private val statisticsFolder: File = File(
        rootPath,
        STATISTICS_FOLDER_NAME
    ).also { it.mkdirs() }

    private var buildSession: BuildSession? = null
    private var trackingFile: IRecordLogger? = null

    private val metricsContainer = MetricsContainer(forceValuesValidation)

    @Synchronized
    fun startBuildSession(buildSinceDaemonStart: Long, buildStartedTime: Long?, buildUuid: String? = null) {
        report(NumericalMetrics.GRADLE_BUILD_NUMBER_IN_CURRENT_DAEMON, buildSinceDaemonStart)

        buildSession = BuildSession(buildStartedTime)
        initTrackingFile(buildUuid)
    }

    @Synchronized
    fun isBuildSessionStarted() = buildSession != null

    @Synchronized
    private fun closeTrackingFile() {
        trackingFile?.let {
            metricsContainer.flush(it)
            it.close()
            trackingFile = null
        }
    }

    /**
     * Initializes a new tracking file
     * The following contracts are implemented:
     * - number of tracking files should not be more than maxProfileFiles (the earlier file created the earlier deleted)
     * - files with age (current time - last modified) more than maxFileAge should be deleted (if we trust lastModified returned by FS)
     * - files are ordered on the basis of name (creation timestamp)
     * - if the last file has size less then maxFileSize, the next record will be append to it (new file created otherwise)
     * -
     */
    @Synchronized
    private fun initTrackingFile(buildUid: String?) {
        closeTrackingFile()

        val fileName = buildUid ?: profileFileNameFormatter.format(LocalDateTime.now())
        trackingFile = FileRecordLogger(statisticsFolder, fileName)
    }

    private fun clearOldFiles() {
        // Get list of existing files. Try to create folder if possible, return from function if failed to create folder
        val fileCandidates = listProfileFiles(statisticsFolder) ?: return

        for ((index, file) in fileCandidates.withIndex()) {
            if (index < fileCandidates.size - maxProfileFiles) {
                file.delete()
            } else {
                val lastModified = file.lastModified()
                (lastModified > 0) && (System.currentTimeMillis() - maxFileAge > lastModified)
            }
        }
    }


    @Synchronized
    fun finishBuildSession(
        @Suppress("UNUSED_PARAMETER") action: String?,
        buildFailed: Boolean,
    ) {
        try {
            // nanotime could not be used as build start time in nanotime is unknown. As result, the measured duration
            // could be affected by system clock correction
            val finishTime = System.currentTimeMillis()
            buildSession?.also {
                if (it.buildStartedTime != null) {
                    report(NumericalMetrics.GRADLE_BUILD_DURATION, finishTime - it.buildStartedTime)
                }
                report(NumericalMetrics.GRADLE_EXECUTION_DURATION, finishTime - it.projectEvaluatedTime)
                report(NumericalMetrics.BUILD_FINISH_TIME, finishTime)
                report(BooleanMetrics.BUILD_FAILED, buildFailed)
            }
            buildSession = null
        } finally {
            unlockJournalFile()
        }
    }

    @Synchronized
    private fun unlockJournalFile() {
        closeTrackingFile()
        clearOldFiles()
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)
}
