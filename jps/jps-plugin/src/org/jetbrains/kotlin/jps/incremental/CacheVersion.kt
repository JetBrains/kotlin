/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.jps.incremental.CacheVersion.Action
import org.jetbrains.kotlin.load.java.JvmAbi
import java.io.File

private val NORMAL_VERSION = 7
private val EXPERIMENTAL_VERSION = 2
private val DATA_CONTAINER_VERSION = 1

private val NORMAL_VERSION_FILE_NAME = "format-version.txt"
private val EXPERIMENTAL_VERSION_FILE_NAME = "experimental-format-version.txt"
private val DATA_CONTAINER_VERSION_FILE_NAME = "data-container-format-version.txt"

class CacheVersion(
        private val ownVersion: Int,
        private val versionFile: File,
        private val whenVersionChanged: CacheVersion.Action,
        private val whenTurnedOn: CacheVersion.Action,
        private val whenTurnedOff: CacheVersion.Action,
        isEnabled: ()->Boolean
) {
    private val isEnabled by lazy(isEnabled)

    private val actualVersion: Int
        get() = versionFile.readText().toInt()

    private val expectedVersion: Int
        get() = ownVersion * 1000000 + JvmAbi.VERSION.major * 1000 + JvmAbi.VERSION.minor

    fun checkVersion(): Action =
            when (versionFile.exists() to isEnabled) {
                true  to true -> if (actualVersion != expectedVersion) whenVersionChanged else Action.DO_NOTHING
                false to true -> whenTurnedOn
                true  to false -> whenTurnedOff
                else -> Action.DO_NOTHING
            }

    fun saveIfNeeded() {
        if (!isEnabled) return

        if (!versionFile.parentFile.exists()) {
            versionFile.parentFile.mkdirs()
        }

        versionFile.writeText(expectedVersion.toString())
    }

    fun clean() {
        versionFile.delete()
    }

    @TestOnly
    val formatVersionFile: File
        get() = versionFile

    // Order of entries is important, because actions are sorted in KotlinBuilder::checkVersions
    enum class Action(val isChunkRebuildRequired: Boolean = false) {
        REBUILD_ALL_KOTLIN(isChunkRebuildRequired = true),
        REBUILD_CHUNK(isChunkRebuildRequired = true),
        CLEAN_NORMAL_CACHES(),
        CLEAN_EXPERIMENTAL_CACHES(),
        CLEAN_DATA_CONTAINER(),
        DO_NOTHING()
    }
}

class CacheVersionProvider(private val paths: BuildDataPaths) {
    private val BuildTarget<*>.dataRoot: File
        get() = paths.getTargetDataRoot(this)

    fun normalVersion(target: ModuleBuildTarget): CacheVersion =
            CacheVersion(ownVersion = NORMAL_VERSION,
                         versionFile = File(target.dataRoot, NORMAL_VERSION_FILE_NAME),
                         whenVersionChanged = Action.REBUILD_CHUNK,
                         whenTurnedOn = Action.REBUILD_CHUNK,
                         whenTurnedOff = Action.CLEAN_NORMAL_CACHES,
                         isEnabled = { IncrementalCompilation.isEnabled() })

    fun experimentalVersion(target: ModuleBuildTarget): CacheVersion =
            CacheVersion(ownVersion = EXPERIMENTAL_VERSION,
                         versionFile = File(target.dataRoot, EXPERIMENTAL_VERSION_FILE_NAME),
                         whenVersionChanged = Action.REBUILD_CHUNK,
                         whenTurnedOn = Action.REBUILD_CHUNK,
                         whenTurnedOff = Action.CLEAN_EXPERIMENTAL_CACHES,
                         isEnabled = { IncrementalCompilation.isExperimental() })

    fun dataContainerVersion(): CacheVersion =
            CacheVersion(ownVersion = DATA_CONTAINER_VERSION,
                         versionFile = File(KotlinDataContainerTarget.dataRoot, DATA_CONTAINER_VERSION_FILE_NAME),
                         whenVersionChanged = Action.REBUILD_ALL_KOTLIN,
                         whenTurnedOn = Action.REBUILD_ALL_KOTLIN,
                         whenTurnedOff = Action.CLEAN_DATA_CONTAINER,
                         isEnabled = { IncrementalCompilation.isExperimental() })

    fun allVersions(targets: Iterable<ModuleBuildTarget>): Iterable<CacheVersion> {
        val versions = arrayListOf<CacheVersion>()
        versions.add(dataContainerVersion())

        for (target in targets) {
            versions.add(normalVersion(target))
            versions.add(experimentalVersion(target))
        }

        return versions
    }
}