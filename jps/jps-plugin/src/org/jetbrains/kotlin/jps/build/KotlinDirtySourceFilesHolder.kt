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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import java.io.File

/**
 * Holding kotlin dirty files list for particular build round.
 * Dirty and removed files set are initialized from [delegate].
 *
 * Additional dirty files should be added only through [FSOperationsHelper.markFilesForCurrentRound]
 */
class KotlinDirtySourceFilesHolder(
    val chunk: ModuleChunk,
    val context: CompileContext,
    delegate: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>
) {
    val byTarget: Map<ModuleBuildTarget, TargetFiles>

    inner class TargetFiles(val target: ModuleBuildTarget, val removed: Collection<File>) {
        private val _dirty: MutableSet<File> = mutableSetOf()

        val dirty: Set<File>
            get() = _dirty

        /**
         * Should be called only from [FSOperationsHelper.markFilesForCurrentRound]
         * and during KotlinDirtySourceFilesHolder initialization.
         */
        internal fun _markDirty(file: File) {
            _dirty.add(file)
        }
    }

    val hasRemovedFiles: Boolean
        get() = byTarget.any { it.value.removed.isNotEmpty() }

    val hasDirtyFiles: Boolean
        get() = byTarget.any { it.value.dirty.isNotEmpty() }

    val hasDirtyOrRemovedFiles: Boolean
        get() = hasRemovedFiles || hasDirtyFiles

    init {
        val byTarget = mutableMapOf<ModuleBuildTarget, TargetFiles>()

        chunk.targets.forEach { target ->
            val removedFiles = delegate.getRemovedFiles(target).map { File(it) }
            byTarget[target] = TargetFiles(target, removedFiles)
        }

        delegate.processDirtyFiles { target, file, root ->
            val targetInfo = byTarget[target]
                    ?: error("processDirtyFiles should callback only on chunk `$chunk` targets (`$target` is not)")

            if (file.isKotlinSourceFile) {
                targetInfo._markDirty(file)
            }

            true
        }

        this.byTarget = byTarget
    }

    fun getDirtyFiles(target: ModuleBuildTarget): Set<File> =
        byTarget[target]?.dirty ?: setOf()

    fun getRemovedFiles(target: ModuleBuildTarget): Collection<File> =
        byTarget[target]?.removed ?: listOf()

    val allDirtyFiles: Set<File>
        get() = byTarget.flatMapTo(mutableSetOf()) { it.value.dirty }

    val allRemovedFilesFiles
        get() = byTarget.flatMapTo(mutableSetOf()) { it.value.removed }
}

val File.isKotlinSourceFile: Boolean
    get() = FileUtilRt.extensionEquals(name, "kt")
