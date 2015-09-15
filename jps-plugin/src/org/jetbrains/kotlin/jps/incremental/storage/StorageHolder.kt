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

package org.jetbrains.kotlin.jps.incremental.storage

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import java.io.File
import java.io.IOException


internal class StorageHolder<K, V>(
        private val storageFile: File,
        private val keyDescriptor: KeyDescriptor<K>,
        private val valueExternalizer: DataExternalizer<V>
) {
    private var storage: PersistentHashMap<K, V>? = null

    public fun getStorageIfExists(): PersistentHashMap<K, V>? {
        if (storage != null) return storage

        if (storageFile.exists()) {
            storage = createMap()
            return storage
        }

        return null
    }

    public fun getStorageOrCreateNew(): PersistentHashMap<K, V> {
        if (storage == null) {
            storage = createMap()
        }

        return storage!!
    }

    public fun clean() {
        try {
            storage?.close()
        }
        catch (ignored: IOException) {
        }

        PersistentHashMap.deleteFilesStartingWith(storageFile)
        storage = null
    }

    public fun flush(memoryCachesOnly: Boolean) {
        val existingStorage = storage ?: return

        if (memoryCachesOnly) {
            if (existingStorage.isDirty) {
                existingStorage.dropMemoryCaches()
            }
        }
        else {
            existingStorage.force()
        }
    }

    public fun close() {
        storage?.close()
    }

    private fun createMap(): PersistentHashMap<K, V> =
            PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
}
