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
import org.jetbrains.kotlin.jps.incremental.storage.LookupMap
import java.io.File

object LOOKUP_TRACKER_STORAGE_PROVIDER : StorageProvider<LookupTrackerImpl>() {
    override fun createStorage(targetDataDir: File): LookupTrackerImpl = LookupTrackerImpl(targetDataDir)
}

class LookupTrackerImpl(private val targetDataDir: File) : BasicMapsOwner(), LookupTracker {
    private val String.storageFile: File
        get() = File(targetDataDir, this + IncrementalCacheImpl.CACHE_EXTENSION)

    private val lookupMap = registerMap(LookupMap("lookups".storageFile))

    override fun record(lookupContainingFile: String, lookupLine: Int?, lookupColumn: Int?, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        lookupMap.add(name, scopeFqName, lookupContainingFile)
    }

    override fun clean() {
        lookupMap.clean()
    }

    override fun close() {
        lookupMap.close()
    }

    override fun flush(memoryCachesOnly: Boolean) {
        lookupMap.flush(memoryCachesOnly)
    }
}

