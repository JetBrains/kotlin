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
import org.jetbrains.jps.builders.FileProcessor
import org.jetbrains.jps.builders.impl.DirtyFilesHolderBase
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import java.io.File

/**
 * Holding kotlin dirty files list for particular build round.
 * Dirty and removed files set are initialized from [delegate].
 *
 * Probably should be merged with [FSOperationsHelper]
 */
class KotlinRoundDirtySourceFilesHolder(
    val chunk: ModuleChunk,
    val context: CompileContext,
    val fsOperations: FSOperationsHelper,
    delegate: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>
) : DirtyFilesHolderBase<JavaSourceRootDescriptor, ModuleBuildTarget>(context) {
    val byTarget: Map<ModuleBuildTarget, TargetFiles>

    inner class TargetFiles(val target: ModuleBuildTarget, val removed: Collection<String>) {
        val dirty: MutableSet<DirtyFile> = mutableSetOf()

        val dirtyOrRemovedFiles: Set<File>
            get() {
                val result = mutableSetOf<File>()
                dirty.forEach { result.add(it.file) }
                removed.forEach { result.add(File(it)) }
                return result
            }

        fun addComplementaryFiles(files: Collection<File>) {
            fsOperations.markComplementaryFiles(files)
            files.forEach {
                dirty.add(DirtyFile(it, null))
            }
        }
    }

    data class DirtyFile(val file: File, val root: JavaSourceRootDescriptor?)

    val hasRemovedFiles: Boolean

    override fun hasRemovedFiles(): Boolean = hasRemovedFiles

    val hasDirtyFiles: Boolean

    override fun hasDirtyFiles(): Boolean = hasDirtyFiles

    val hasDirtyOrRemovedFiles: Boolean
        get() = hasRemovedFiles || hasDirtyFiles

    init {
        val byTarget = mutableMapOf<ModuleBuildTarget, TargetFiles>()
        var hasDirtyFiles = false
        var hasRemovedFiles = false

        chunk.targets.forEach { target ->
            val removedFiles = delegate.getRemovedFiles(target)
            if (removedFiles.isNotEmpty()) {
                hasRemovedFiles = true
            }

            byTarget[target] = TargetFiles(target, removedFiles)
        }

        delegate.processDirtyFiles { target, file, root ->
            val targetInfo = byTarget[target]
                    ?: error("processDirtyFiles should callback only on chunk `$chunk` targets (`$target` is not)")

            if (file.isKotlinSourceFile) {
                targetInfo.dirty.add(DirtyFile(file, root))
                hasDirtyFiles = true
            }

            true
        }

        this.byTarget = byTarget
        this.hasRemovedFiles = hasRemovedFiles
        this.hasDirtyFiles = hasDirtyFiles
    }

    override fun processDirtyFiles(processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>) {
        for ((target, info) in byTarget) {
            info.dirty.forEach {
                if (!processor.apply(target, it.file, it.root)) return
            }
        }
    }

    fun getDirtyFiles(target: ModuleBuildTarget): Set<File> =
        byTarget[target]?.dirty?.mapTo(mutableSetOf()) { it.file } ?: setOf()

    fun getRemovedFilesSet(target: ModuleBuildTarget): Set<File> =
        byTarget[target]?.removed?.mapTo(mutableSetOf()) { File(it) } ?: setOf()

    override fun getRemovedFiles(target: ModuleBuildTarget): Collection<String> =
        byTarget.flatMap { it.value.removed }

    val removedFilesCount
        get() = byTarget.values.flatMapTo(mutableSetOf()) { it.removed }.size

    val dirtyFiles: Set<File>
        get() = byTarget.flatMapTo(mutableSetOf()) { it.value.dirty.map { it.file } }

    val dirtyOrRemovedFilesSet: Set<File>
        get() {
            val result = mutableSetOf<File>()
            byTarget.forEach {
                it.value.dirty.forEach { result.add(it.file) }
                it.value.removed.forEach { result.add(File(it)) }
            }
            return result
        }
}

val File.isKotlinSourceFile: Boolean
    get() = FileUtilRt.extensionEquals(name, "kt")
