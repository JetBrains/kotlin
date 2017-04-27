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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.BuildRootIndex
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.BuildTargetIndex
import org.jetbrains.jps.builders.java.dependencyView.Mappings
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.fs.CompilationRound
import java.io.File
import java.util.HashMap

class FSOperationsHelper(
        private val compileContext: CompileContext,
        private val chunk: ModuleChunk,
        private val log: Logger
) {
    private val moduleBasedFilter = ModulesBasedFileFilter(compileContext, chunk)

    internal var hasMarkedDirty = false
        private set

    private val buildLogger = compileContext.testingContext?.buildLogger

    fun markChunk(recursively: Boolean, kotlinOnly: Boolean, excludeFiles: Set<File> = setOf()) {
        fun shouldMark(file: File): Boolean {
            if (kotlinOnly && !KotlinSourceFileCollector.isKotlinSourceFile(file)) return false

            if (file in excludeFiles) return false

            hasMarkedDirty = true
            return true
        }

        if (recursively) {
            FSOperations.markDirtyRecursively(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
        else {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
    }

    fun markFiles(files: Iterable<File>) {
        markFilesImpl(files) { it.exists() }
    }

    fun markInChunkOrDependents(files: Iterable<File>, excludeFiles: Set<File>) {
        markFilesImpl(files) { it !in excludeFiles && it.exists() && moduleBasedFilter.accept(it) }
    }

    private inline fun markFilesImpl(files: Iterable<File>, shouldMark: (File)->Boolean) {
        val filesToMark = files.filterTo(HashSet(), shouldMark)

        if (filesToMark.isEmpty()) return

        for (fileToMark in filesToMark) {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, fileToMark)
        }

        log.debug("Mark dirty: $filesToMark")
        buildLogger?.markedAsDirty(filesToMark)
        hasMarkedDirty = true
    }

    // Based on `JavaBuilderUtil#ModulesBasedFileFilter` from Intellij
    private class ModulesBasedFileFilter(
            private val context: CompileContext,
            chunk: ModuleChunk
    ): Mappings.DependentFilesFilter {
        private val chunkTargets = chunk.targets
        private val buildRootIndex = context.projectDescriptor.buildRootIndex
        private val buildTargetIndex = context.projectDescriptor.buildTargetIndex
        private val cache = HashMap<BuildTarget<*>, Set<BuildTarget<*>>>()

        override fun accept(file: File): Boolean {
            val rd = buildRootIndex.findJavaRootDescriptor(context, file) ?: return true
            val target = rd.target
            if (target in chunkTargets) return true

            val targetOfFileWithDependencies = cache.getOrPut(target) { buildTargetIndex.getDependenciesRecursively(target, context) }
            return ContainerUtil.intersects(targetOfFileWithDependencies, chunkTargets)
        }

        override fun belongsToCurrentTargetChunk(file: File): Boolean {
            val rd = buildRootIndex.findJavaRootDescriptor(context, file)
            return rd != null && chunkTargets.contains(rd.target)
        }
    }
}
