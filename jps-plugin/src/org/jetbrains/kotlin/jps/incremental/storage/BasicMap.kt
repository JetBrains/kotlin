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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.utils.Printer
import java.io.DataOutput
import java.io.File

public abstract class BasicMap<K : Comparable<K>, V>(
        storageFile: File,
        keyDescriptor: KeyDescriptor<K>,
        valueExternalizer: DataExternalizer<V>
) {
    private val storageHolder = StorageHolder(storageFile, keyDescriptor, valueExternalizer)

    public fun clean() {
        storageHolder.clean()
    }

    public fun flush(memoryCachesOnly: Boolean) {
        storageHolder.flush(memoryCachesOnly)
    }

    public fun close() {
        storageHolder.close()
    }

    TestOnly
    public fun dump(): String {
        return with(StringBuilder()) {
            with(Printer(this)) {
                println(this@BasicMap.javaClass.getSimpleName())
                pushIndent()

                for (key in allKeysExistingInStorage.sort()) {
                    println("${dumpKey(key)} -> ${dumpValue(getFromStorage(key)!!)}")
                }

                popIndent()
            }

            this
        }.toString()
    }

    protected abstract fun dumpKey(key: K): String
    protected abstract fun dumpValue(value: V): String

    protected val allKeysExistingInStorage: Collection<K>
        get() = storageHolder.getStorageIfExists()?.allKeysWithExistingMapping ?: listOf()

    protected fun storageContains(key: K): Boolean =
            storageHolder.getStorageIfExists()?.containsMapping(key) ?: false

    protected fun getFromStorage(key: K): V? =
            storageHolder.getStorageIfExists()?.get(key)

    protected fun putToStorage(key: K, value: V) {
        storageHolder.getStorageOrCreateNew().put(key, value)
    }

    protected fun removeFromStorage(key: K) {
        storageHolder.getStorageIfExists()?.remove(key)
    }

    protected fun appendDataToStorage(key: K, append: (DataOutput)->Unit) {
        storageHolder.getStorageOrCreateNew().appendData(key, append)
    }
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