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

package org.jetbrains.kotlin.incremental.multiproject

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.name.FqName
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.util.*

internal class ArtifactDifferenceRegistryImpl(
        storageFile: File
) : ArtifactDifferenceRegistry, BasicStringMap<Collection<ArtifactDifference>>(
        storageFile,
        ArtifactDifferenceCollectionExternalizer
) {
    companion object {
        private val MAX_BUILDS_PER_ARTIFACT = 10
    }

    override fun get(artifact: File): Iterable<ArtifactDifference>? =
            storage[artifact.canonicalPath]

    override fun add(artifact: File, difference: ArtifactDifference) {
        val key = artifact.canonicalPath
        val oldVal = storage[key] ?: emptyList()
        val newVal = ArrayList(oldVal)
        newVal.add(difference)
        newVal.sortBy { it.buildTS }
        storage[key] = newVal.takeLast(MAX_BUILDS_PER_ARTIFACT)
    }

    override fun remove(artifact: File) {
        storage.remove(artifact.canonicalPath)
    }

    override fun dumpValue(value: Collection<ArtifactDifference>): String =
            value.sortedBy { it.buildTS }.joinToString(separator = ",\n\t") { diff ->
                "{ " +
                        "timestamp: ${diff.buildTS}, " +
                        "lookup symbols: [${diff.dirtyData.dirtyLookupSymbols.dumpLookupSymbols()}], " +
                        "fq names: [${diff.dirtyData.dirtyClassesFqNames.dumpFqNames()}]"
                "}"
            }
}

private fun <T> Collection<T>.takeLast(n: Int): Collection<T> =
        drop(Math.max(size - n, 0))

private fun Collection<LookupSymbol>.dumpLookupSymbols(): String =
        map { "${it.scope}#${it.name}" }.sorted().joinToString()

private fun Collection<FqName>.dumpFqNames(): String =
        map(FqName::asString).sorted().joinToString()

private object ArtifactDifferenceCollectionExternalizer : DataExternalizer<Collection<ArtifactDifference>> {
    override fun read(input: DataInput): Collection<ArtifactDifference> =
            input.readCollectionTo(ArrayList()) {
                val buildTS = readLong()
                val dirtyLookupSymbols = readCollectionTo(HashSet()) {
                    val scope = readUTF()
                    val name = readUTF()
                    LookupSymbol(name, scope)
                }
                val dirtyFqNames = readCollectionTo(HashSet()) {
                    FqName(readUTF())
                }
                ArtifactDifference(buildTS, DirtyData(dirtyLookupSymbols, dirtyFqNames))
            }

    override fun save(output: DataOutput, value: Collection<ArtifactDifference>) {
        output.writeCollection(value) { diff ->
            writeLong(diff.buildTS)
            writeCollection(diff.dirtyData.dirtyLookupSymbols) {
                writeUTF(it.scope)
                writeUTF(it.name)
            }
            writeCollection(diff.dirtyData.dirtyClassesFqNames) {
                writeUTF(it.asString())
            }
        }
    }
}

private inline fun <T> DataInput.readCollectionTo(col: MutableCollection<T>, readT: DataInput.() -> T): Collection<T> {
    val size = readInt()

    repeat(size) {
        col.add(readT())
    }

    return col
}

private inline fun <T> DataOutput.writeCollection(col: Collection<T>, writeT: DataOutput.(T) -> Unit) {
    writeInt(col.size)
    col.forEach { writeT(it) }
}