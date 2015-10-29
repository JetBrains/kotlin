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

import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.jps.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.jps.incremental.storage.FileToIdMap
import org.jetbrains.kotlin.jps.incremental.storage.IdToFileMap
import org.jetbrains.kotlin.jps.incremental.storage.LookupMap
import java.io.File

object LOOKUP_TRACKER_STORAGE_PROVIDER : StorageProvider<LookupTrackerImpl>() {
    override fun createStorage(targetDataDir: File): LookupTrackerImpl = LookupTrackerImpl(targetDataDir)
}

class LookupTrackerImpl(private val targetDataDir: File) : BasicMapsOwner(), LookupTracker {

    companion object {
        private val DELETED_TO_SIZE_TRESHOLD = 0.5
        private val MINIMUM_GARBAGE_COLLECTIBLE_SIZE = 10000
    }

    private val String.storageFile: File
        get() = File(targetDataDir, this + IncrementalCacheImpl.CACHE_EXTENSION)

    private val countersFile = "counters".storageFile
    private val idToFile = registerMap(IdToFileMap("id-to-file".storageFile))
    private val fileToId = registerMap(FileToIdMap("file-to-id".storageFile))
    private val lookupMap = registerMap(LookupMap("lookups".storageFile))
    private var size: Int = 0
    private var deletedCount: Int = 0

    init {
        if (countersFile.exists()) {
            val lines = countersFile.readLines()
            size = lines[0].toInt()
            deletedCount = lines[1].toInt()
        }
    }

    override fun record(lookupContainingFile: String, lookupLine: Int?, lookupColumn: Int?, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        val file = File(lookupContainingFile)
        val fileId = fileToId[file] ?: addFile(file)
        lookupMap.add(name, scopeFqName, fileId)
    }

    fun removeLookupsFrom(file: File) {
        val id = fileToId[file] ?: return
        idToFile.remove(id)
        fileToId.remove(file)
        deletedCount++
    }

    override fun clean() {
        if (countersFile.exists()) {
            countersFile.delete()
        }

        size = 0
        deletedCount = 0

        super.clean()
    }

    override fun flush(memoryCachesOnly: Boolean) {
        try {
            removeGarbageIfNeeded()
            countersFile.writeText("$size\n$deletedCount")
        }
        finally {
            super.flush(memoryCachesOnly)
        }
    }

    private fun addFile(file: File): Int {
        val id = size++
        fileToId[file] = id
        idToFile[id] = file
        return id
    }

    private fun removeGarbageIfNeeded() {
        if (size <= MINIMUM_GARBAGE_COLLECTIBLE_SIZE && deletedCount.toDouble() / size <= DELETED_TO_SIZE_TRESHOLD) return

        for (hash in lookupMap.lookupHashes) {
            lookupMap[hash] = lookupMap[hash]!!.filter { it in idToFile }.toSet()
        }

        size = 0
        deletedCount = 0
        idToFile.clean()

        fileToId.files.forEach { addFile(it) }
    }
}

