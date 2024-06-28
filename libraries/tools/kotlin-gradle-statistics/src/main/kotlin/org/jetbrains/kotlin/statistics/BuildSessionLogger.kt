/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

class BuildSessionLogger(
    rootPath: File,
    private val maxProfileFiles: Int = DEFAULT_MAX_PROFILE_FILES,
    private val maxFileAge: Long = DEFAULT_MAX_FILE_AGE,
    forceValuesValidation: Boolean = false,
) : StatisticsValuesConsumer {

    companion object {
        const val PROFILE_FILE_NAME_SUFFIX = ".profile"
        const val STATISTICS_FOLDER_NAME = "kotlin-profile"
        val STATISTICS_FILE_NAME_PATTERN = "[\\w-]*$PROFILE_FILE_NAME_SUFFIX".toRegex()

        private const val DEFAULT_MAX_PROFILE_FILES = 1_000
        private const val DEFAULT_MAX_FILE_AGE = 30 * 24 * 3600 * 1000L //30 days

        fun listProfileFiles(statisticsFolder: File): List<File> {
            return Files.newDirectoryStream(statisticsFolder.toPath()).use { dirStream ->
                dirStream.map { it.toFile() }
                    .filter { it.name.matches(STATISTICS_FILE_NAME_PATTERN) }
                    .sortedBy { it.lastModified() }
            }
        }
    }

    private val statisticsFolder: File = File(
        rootPath,
        STATISTICS_FOLDER_NAME
    ).also { it.mkdirs() }

    private var buildSession: BuildSession? = null

    private val metricsContainer = MetricsContainer(forceValuesValidation)

    @Synchronized
    fun startBuildSession(buildUid: String) {
        buildSession = BuildSession(buildUid)
    }

    @Synchronized
    fun isBuildSessionStarted() = buildSession != null

    @Synchronized
    fun getActiveBuildId() = buildSession?.buildUid

    /**
     * Initializes a new build report file
     * The following contracts are implemented:
     * - each file contains metrics for one build
     * - any other process can add metrics to the file during build
     * - files with age (current time - last modified) more than maxFileAge should be deleted (if we trust lastModified returned by FS)
     */
    private fun storeMetricsIntoFile(buildId: String) {
        try {
            statisticsFolder.mkdirs()
            val file = File(statisticsFolder, buildId + PROFILE_FILE_NAME_SUFFIX)

            FileOutputStream(file, true).bufferedWriter().use {
                metricsContainer.flush(it)
            }
        } catch (_: IOException) {
            //ignore io exception
        }
    }

    private fun clearOldFiles() {
        // Get list of existing files. Try to create folder if possible, return from function if failed to create folder
        val fileCandidates = listProfileFiles(statisticsFolder)

        for ((index, file) in fileCandidates.withIndex()) {
            val toDelete = if (index < fileCandidates.size - maxProfileFiles)
                true
            else {
                val lastModified = file.lastModified()
                (lastModified > 0) && (System.currentTimeMillis() - maxFileAge > lastModified)
            }
            if (toDelete) {
                file.delete()
            }
        }
    }


    @Synchronized
    fun finishBuildSession() {
        buildSession?.also {
            storeMetricsIntoFile(it.buildUid)
        }
        buildSession = null
        clearOldFiles()
    }

    override fun report(metric: BooleanMetrics, value: Boolean, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)

    override fun report(metric: NumericalMetrics, value: Long, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)

    override fun report(metric: StringMetrics, value: String, subprojectName: String?, weight: Long?) =
        metricsContainer.report(metric, value, subprojectName, weight)
}
