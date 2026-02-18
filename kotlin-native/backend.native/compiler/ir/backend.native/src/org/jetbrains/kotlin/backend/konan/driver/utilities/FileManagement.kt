/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.CacheSupport
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.config.temporaryFilesDir
import java.io.File

internal fun createTempFiles(config: NativeSecondStageCompilationConfig, cacheDeserializationStrategy: CacheDeserializationStrategy?): TempFiles {
    val pathToTempDir = config.configuration.temporaryFilesDir?.let {
        val singleFileStrategy = cacheDeserializationStrategy as? CacheDeserializationStrategy.SingleFile
        if (singleFileStrategy == null)
            it
        else org.jetbrains.kotlin.konan.file.File(it, CacheSupport.cacheFileId(singleFileStrategy.fqName, singleFileStrategy.filePath)).path
    }
    return TempFiles(pathToTempDir)
}

/**
 * Files that are generated only during compilation of C dynamic/static library.
 *
 * TODO: At some point this class and it usages can be generalized to all possible compiler outputs,
 *  so instead of OutputFiles we will create a series of classes for each specific compiler output kit.
 */
internal class CExportFiles(
        val cppAdapter: File,
        val bitcodeAdapter: File,
        val header: File,
        val def: File?,
)