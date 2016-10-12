/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistry
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryImpl
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import org.jetbrains.kotlin.incremental.stackTraceStr
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import java.io.File

/**
 * "Global" cache holder. Should be created once per root project.
 */
internal class BuildCacheStorage(workingDir: File) : BasicMapsOwner(), ArtifactDifferenceRegistryProvider {
    companion object {
        private val OWN_VERSION = 0
        private val ARTIFACT_DIFFERENCE = "artifact-difference"
    }

    private val log = Logging.getLogger(this.javaClass)
    private val cachesDir: File = File(workingDir, "caches").apply { mkdirs() }
    private val versionFile = File(cachesDir, "version.txt")
    private val version = CacheVersion(
            OWN_VERSION,
            versionFile,
            whenVersionChanged = CacheVersion.Action.REBUILD_ALL_KOTLIN,
            whenTurnedOff = CacheVersion.Action.REBUILD_ALL_KOTLIN,
            whenTurnedOn = CacheVersion.Action.REBUILD_ALL_KOTLIN,
            // assume it's always enabled for simplicity (if IC is not enabled, just don't write to cache)
            isEnabled = { true })

    @Volatile
    private var artifactDifferenceRegistry: ArtifactDifferenceRegistryImpl? = null

    private val String.storageFile: File
        get() = File(cachesDir, this + "." + CACHE_EXTENSION)

    @Synchronized
    override fun <T> withRegistry(report: (String)->Unit, fn: (ArtifactDifferenceRegistry)->T): T? {
        try {
            if (artifactDifferenceRegistry == null) {
                artifactDifferenceRegistry = registerMap(ArtifactDifferenceRegistryImpl(ARTIFACT_DIFFERENCE.storageFile))
            }

            return fn(artifactDifferenceRegistry!!)
        }
        catch (e1: Throwable) {
            report("Error accessing artifact file difference registry: ${e1.stackTraceStr}}")
            report("Cleaning artifact difference storage and trying again")
            clean()

            try {
                artifactDifferenceRegistry = registerMap(ArtifactDifferenceRegistryImpl(ARTIFACT_DIFFERENCE.storageFile))
                return fn(artifactDifferenceRegistry!!)
            }
            catch (e2: Throwable) {
                report("Second error accessing artifact file difference registry: ${e2.stackTraceStr}}")
            }
        }

        return null
    }

    init {
        if (version.checkVersion() != CacheVersion.Action.DO_NOTHING) {
            log.kotlinDebug { "Cache version is not up-to-date" }
            clean()
        }
    }

    @Synchronized
    override fun clean() {
        try {
            close()
        }
        catch (e: Throwable) {
            log.kotlinDebug { "Exception while closing caches: ${e.stackTraceStr}" }
        }

        cachesDir.deleteRecursively()
        cachesDir.mkdirs()
        versionFile.delete()
    }

    override fun close() {
        super.close()
        version.saveIfNeeded()
    }
}
