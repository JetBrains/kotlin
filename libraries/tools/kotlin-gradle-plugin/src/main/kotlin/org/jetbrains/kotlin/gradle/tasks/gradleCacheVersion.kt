package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.CacheVersion
import java.io.File

const val GRADLE_CACHE_VERSION = 1
const val GRADLE_CACHE_VERSION_FILE_NAME = "gradle-format-version.txt"

fun gradleCacheVersion(dataRoot: File): CacheVersion =
        CacheVersion(ownVersion = GRADLE_CACHE_VERSION,
                versionFile = File(dataRoot, GRADLE_CACHE_VERSION_FILE_NAME),
                whenVersionChanged = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOn = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                whenTurnedOff = CacheVersion.Action.REBUILD_ALL_KOTLIN,
                isEnabled = { IncrementalCompilation.isExperimental() })
