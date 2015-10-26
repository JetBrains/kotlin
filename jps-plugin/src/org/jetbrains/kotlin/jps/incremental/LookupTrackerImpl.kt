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

import com.intellij.util.io.KeyDescriptor
import org.jetbrains.jps.builders.storage.StorageProvider
import org.jetbrains.jps.incremental.storage.StorageOwner
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.jps.incremental.storage.BasicMap
import java.io.DataInput
import java.io.DataOutput
import java.io.File

object LOOKUP_TRACKER_STORAGE_PROVIDER : StorageProvider<LookupTrackerImpl>() {
    override fun createStorage(targetDataDir: File): LookupTrackerImpl = LookupTrackerImpl(targetDataDir)
}

class LookupTrackerImpl(targetDataDir: File) : LookupTracker, StorageOwner {
    private val lookupMap = LookupMap(File(targetDataDir, "lookups.tab"))

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

private class LookupMap(file: File) : BasicMap<IntPair, Collection<String>>(file, INT_PAIR_KEY_DESCRIPTOR, PathCollectionExternalizer) {
    override fun dumpKey(key: IntPair): String = key.toString()

    override fun dumpValue(value: Collection<String>): String = value.toString()

    public fun add(name: String, scope: String, path: String) {
        storage.append(HashPair(name, scope)) { out -> out.writeUTF(path) }
    }

    public fun get(name: String, scope: String): Collection<String>? = storage[HashPair(name, scope)]
}

private data class IntPair(val first: Int, val second: Int) : Comparable<IntPair> {
    override fun compareTo(other: IntPair): Int {
        val firstCmp = first.compareTo(other.first)

        if (firstCmp != 0) return firstCmp

        return second.compareTo(other.second)
    }
}

private fun HashPair(a: Any, b: Any): IntPair = IntPair(a.hashCode(), b.hashCode())

private object INT_PAIR_KEY_DESCRIPTOR : KeyDescriptor<IntPair> {
    override fun read(`in`: DataInput): IntPair {
        val first = `in`.readInt()
        val second = `in`.readInt()
        return IntPair(first, second)
    }

    override fun save(out: DataOutput, value: IntPair?) {
        if (value == null) return

        out.writeInt(value.first)
        out.writeInt(value.second)
    }

    override fun getHashCode(value: IntPair?): Int = value?.hashCode() ?: 0

    override fun isEqual(val1: IntPair?, val2: IntPair?): Boolean = val1 == val2
}
