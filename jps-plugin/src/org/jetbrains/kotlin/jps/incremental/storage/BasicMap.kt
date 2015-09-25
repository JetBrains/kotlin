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
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.io.IOException

public abstract class BasicMap<K : Comparable<K>, V>(
        private val storageFile: File,
        private val keyDescriptor: KeyDescriptor<K>,
        private val valueExternalizer: DataExternalizer<V>
) {
    protected var storage: PersistentHashMap<K, V> = createMap()

    public fun contains(key: K): Boolean = storage.containsMapping(key)

    public fun clean() {
        try {
            storage.close()
        }
        catch (ignored: IOException) {
        }

        PersistentHashMap.deleteFilesStartingWith(storage.getBaseFile()!!)
        try {
            storage = createMap()
        }
        catch (ignored: IOException) {
        }
    }

    public fun flush(memoryCachesOnly: Boolean) {
        if (memoryCachesOnly) {
            if (storage.isDirty()) {
                storage.dropMemoryCaches()
            }
        }
        else {
            storage.force()
        }
    }

    public fun close() {
        storage.close()
    }

    @TestOnly
    public fun dump(): String {
        return with(StringBuilder()) {
            with(Printer(this)) {
                println(this@BasicMap.javaClass.getSimpleName())
                pushIndent()

                for (key in storage.getAllKeysWithExistingMapping().sort()) {
                    println("${dumpKey(key)} -> ${dumpValue(storage[key])}")
                }

                popIndent()
            }

            this
        }.toString()
    }

    protected abstract fun dumpKey(key: K): String
    protected abstract fun dumpValue(value: V): String

    private fun createMap(): PersistentHashMap<K, V> =
            PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
}

public abstract class BasicStringMap<V>(
        storageFile: File,
        keyDescriptor: KeyDescriptor<String>,
        valueExternalizer: DataExternalizer<V>
) : BasicMap<String, V>(storageFile, keyDescriptor, valueExternalizer) {
    public constructor(
            storageFile: File,
            valueExternalizer: DataExternalizer<V>
    ) : this(storageFile, EnumeratorStringDescriptor.INSTANCE, valueExternalizer)

    override fun dumpKey(key: String): String = key
}