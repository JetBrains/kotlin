/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.logging

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File
import java.io.FileWriter

typealias Errors = List<String>
internal fun Errors.reportToIde(
    file: File,
    kotlinPluginVersion: String? = null,
    buildId: String? = null,
    logger: KotlinLogger
) {
    if (isEmpty()) return

    file.parentFile.mkdirs()
    FileWriter(file).use {
        kotlinPluginVersion?.also { version -> it.append("kotlin version: $version\n") }
        buildId?.also { id -> it.append("build id: $id\n") }
        for (error in this) {
            it.append("error message: $error\n\n")
        }
    }
    logger.debug("${count()} errors were stored into file ${file.absolutePath}")
}

internal fun Errors.reportToIde(
    files: Collection<File>,
    kotlinPluginVersion: String? = null,
    buildId: String? = null,
    logger: KotlinLogger
) {
    for (file in files) {
        reportToIde(file, kotlinPluginVersion, buildId, logger)
    }
}
