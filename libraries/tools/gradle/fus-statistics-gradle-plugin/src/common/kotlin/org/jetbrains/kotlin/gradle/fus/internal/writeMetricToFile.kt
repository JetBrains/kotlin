/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.fus.internal.InternalGradleBuildFusStatisticsService.Companion.FILE_NAME_BUILD_ID_PREFIX_SEPARATOR
import org.jetbrains.kotlin.gradle.fus.internal.InternalGradleBuildFusStatisticsService.Companion.PROFILE_FILE_NAME_SUFFIX
import java.io.File
import java.util.*


internal fun File.createReportFile(buildId: String, log: Logger): File? {
    val reportFile = resolve(
        buildId + FILE_NAME_BUILD_ID_PREFIX_SEPARATOR
                + Calendar.getInstance().timeInMillis + FILE_NAME_BUILD_ID_PREFIX_SEPARATOR
                + UUID.randomUUID() + PROFILE_FILE_NAME_SUFFIX
    )
    if (!reportFile.createNewFile()) {
        log.info("Failed to create report file ${reportFile.absolutePath}. FUS report for plugin won't be created.")
        return null
    }
    return reportFile
}