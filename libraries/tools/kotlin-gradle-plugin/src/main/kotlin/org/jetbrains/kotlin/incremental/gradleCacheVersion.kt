package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.config.IncrementalCompilation
import java.io.File

internal const val GRADLE_CACHE_VERSION = 2
internal const val GRADLE_CACHE_VERSION_FILE_NAME = "gradle-format-version.txt"

internal fun gradleCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = GRADLE_CACHE_VERSION,
                versionFile = File(dataRoot, GRADLE_CACHE_VERSION_FILE_NAME),
                whenVersionChanged = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOn = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOff = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                isEnabled = { IncrementalCompilation.isExperimental() })
