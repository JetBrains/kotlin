/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.customCacheVersion
import java.io.File

internal const val GRADLE_CACHE_VERSION = 4
internal const val GRADLE_CACHE_VERSION_FILE_NAME = "gradle-format-version.txt"

internal fun gradleCacheVersion(dataRoot: File, enabled: Boolean): CacheVersion =
    customCacheVersion(
        GRADLE_CACHE_VERSION,
        GRADLE_CACHE_VERSION_FILE_NAME,
        dataRoot,
        enabled
    )
