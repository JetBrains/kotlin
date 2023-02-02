/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import org.jetbrains.kotlin.backend.konan.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.CacheSupport
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.konan.TempFiles

internal fun createTempFiles(config: KonanConfig, cacheDeserializationStrategy: CacheDeserializationStrategy?): TempFiles {
    val pathToTempDir = config.configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR)?.let {
        val singleFileStrategy = cacheDeserializationStrategy as? CacheDeserializationStrategy.SingleFile
        if (singleFileStrategy == null)
            it
        else org.jetbrains.kotlin.konan.file.File(it, CacheSupport.cacheFileId(singleFileStrategy.fqName, singleFileStrategy.filePath)).path
    }
    return TempFiles(pathToTempDir)
}