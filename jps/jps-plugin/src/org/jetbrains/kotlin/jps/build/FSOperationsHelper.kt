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
import org.jetbrains.jps.builders.BuildRootDescriptor
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.BuildTargetIndex
import org.jetbrains.jps.builders.FileProcessor
import org.jetbrains.jps.builders.impl.DirtyFilesHolderBase
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.builders.java.dependencyView.Mappings
import org.jetbrains.jps.incremental.BuildOperations
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.fs.CompilationRound
import java.io.File
import kotlin.collections.*

/**
 * Entry point for safely marking files as dirty.
 */
class FSOperationsHelper(
    private val compileContext: CompileContext,
    private val chunk: ModuleChunk,
    private val dirtyFilesHolder: KotlinDirtySourceFilesHolder,
    private val log: Logger
) {
    private val moduleBasedFilter = ModulesBasedFileFilter(compileContext, chunk)

    internal var hasMarkedDirty = false
        private set

    private val buildLogger = compileContext.testingContext?.buildLogger

    fun markChunk(recursively: Boolean, kotlinOnly: Boolean, excludeFiles: Set<File> = setOf()) {
        fun shouldMark(file: File): Boolean {
            if (kotlinOnly && !file.isKotlinSourceFile) return false

            if (file in excludeFiles) return false

            hasMarkedDirty = true
            return true
        }

        if (recursively) {
            FSOperations.markDirtyRecursively(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        } else {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
    }

    internal fun markFilesForCurrentRound(files: Iterable<File>) {
        files.forEach {
            val root = compileContext.projectDescriptor.buildRootIndex.findJavaRootDescriptor(compileContext, it)
            if (root != null) dirtyFilesHolder.byTarget[root.target]?._markDirty(it, root)
        }

        markFilesImpl(files, currentRound = true) { it.exists() && moduleBasedFilter.accept(it) }
    }

    /**
     * Marks given [files] as dirty for current round and given [target] of [chunk].
     */
    fun markFilesForCurrentRound(target: ModuleBuildTarget, files: Collection<File>) {
        require(target in chunk.targets)

        val targetDirtyFiles = dirtyFilesHolder.byTarget.getValue(target)
        val dirtyFileToRoot = HashMap<File, JavaSourceRootDescriptor>()
        files.forEach { file ->
            val root = compileContext.projectDescriptor.buildRootIndex
                .findAllParentDescriptors<BuildRootDescriptor>(file, compileContext)
                .single { sourceRoot -> sourceRoot.target == target }

            targetDirtyFiles._markDirty(file, root as JavaSourceRootDescriptor)
            dirtyFileToRoot[file] = root
        }

        markFilesImpl(files, currentRound = true) { it.exists() }
        cleanOutputsForNewDirtyFilesInCurrentRound(target, dirtyFileToRoot)
    }

    private fun cleanOutputsForNewDirtyFilesInCurrentRound(target: ModuleBuildTarget, dirtyFiles: Map<File, JavaSourceRootDescriptor>) {
        val dirtyFilesHolder = object : DirtyFilesHolderBase<JavaSourceRootDescriptor, ModuleBuildTarget>(compileContext) {
            override fun processDirtyFiles(processor: FileProcessor<JavaSourceRootDescriptor, ModuleBuildTarget>) {
                dirtyFiles.forEach { (file, root) -> processor.apply(target, file, root) }
            }

            override fun hasDirtyFiles(): Boolean = dirtyFiles.isNotEmpty()
        }
        BuildOperations.cleanOutputsCorrespondingToChangedFiles(compileContext, dirtyFilesHolder)
    }

    fun markFiles(files: Iterable<File>) {
        markFilesImpl(files, currentRound = false) { it.exists() }
    }

    fun markInChunkOrDependents(files: Iterable<File>, excludeFiles: Set<File>) {
        markFilesImpl(files, currentRound = false) {
            it !in excludeFiles && it.exists() && moduleBasedFilter.accept(it)
        }
    }

    private inline fun markFilesImpl(
        files: Iterable<File>,
        currentRound: Boolean,
        shouldMark: (File) -> Boolean
    ) {
        val filesToMark = files.filterTo(HashSet(), shouldMark)
        if (filesToMark.isEmpty()) return

        val compilationRound = if (currentRound) {
            buildLogger?.markedAsDirtyBeforeRound(filesToMark)
            CompilationRound.CURRENT
        } else {
            buildLogger?.markedAsDirtyAfterRound(filesToMark)
            hasMarkedDirty = true
            CompilationRound.NEXT
        }

        for (fileToMark in filesToMark) {
            FSOperations.markDirty(compileContext, compilationRound, fileToMark)
        }
        log.debug("Mark dirty: $filesToMark ($compilationRound)")
    }

    // Based on `JavaBuilderUtil#ModulesBasedFileFilter` from Intellij
    private class ModulesBasedFileFilter(
        private val context: CompileContext,
        chunk: ModuleChunk
    ) : Mappings.DependentFilesFilter {
        private val chunkTargets = chunk.targets
        private val buildRootIndex = context.projectDescriptor.buildRootIndex
        private val buildTargetIndex = context.projectDescriptor.buildTargetIndex
        private val cache = HashMap<BuildTarget<*>, Set<BuildTarget<*>>>()

        override fun accept(file: File): Boolean {
            val rd = buildRootIndex.findJavaRootDescriptor(context, file) ?: return true
            val target = rd.target
            if (target in chunkTargets) return true

            val targetOfFileWithDependencies = cache.getOrPut(target) { buildTargetIndex.myGetDependenciesRecursively(target, context) }
            return ContainerUtil.intersects(targetOfFileWithDependencies, chunkTargets)
        }

        // Copy-pasted from Intellij's deprecated method org.jetbrains.jps.builders.impl.BuildTargetIndexImpl.getDependenciesRecursively
        private fun BuildTargetIndex.myGetDependenciesRecursively(target: BuildTarget<*>, context: CompileContext): Set<BuildTarget<*>> {
            fun BuildTargetIndex.collectDependenciesRecursively(
                target: BuildTarget<*>,
                result: java.util.LinkedHashSet<in BuildTarget<*>>
            ) {
                if (result.add(target)) {
                    for (dep in getDependencies(target, context)) {
                        collectDependenciesRecursively(dep, result)
                    }
                }
            }

            val result = LinkedHashSet<BuildTarget<*>>()
            for (dep in getDependencies(target, context)) {
                collectDependenciesRecursively(dep, result)
            }
            return result
        }

        override fun belongsToCurrentTargetChunk(file: File): Boolean {
            val rd = buildRootIndex.findJavaRootDescriptor(context, file)
            return rd != null && chunkTargets.contains(rd.target)
        }
    }
}
