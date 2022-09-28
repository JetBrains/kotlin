/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal interface KotlinCompilationFriendPathsResolver {
    fun resolveFriendPaths(compilation: InternalKotlinCompilation<*>): Iterable<FileCollection>
}

internal object DefaultKotlinCompilationFriendPathsResolver : KotlinCompilationFriendPathsResolver {
    override fun resolveFriendPaths(compilation: InternalKotlinCompilation<*>): Iterable<FileCollection> {
        return mutableListOf<FileCollection>().also { allCollections ->
            compilation.associateWithClosure.forEach { allCollections.add(it.output.classesDirs) }
            allCollections.add(resolveFriendArtifacts(compilation))
        }
    }

    private fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection {
        return with(compilation.project) {
            val friendArtifactsTaskProvider = resolveFriendArtifactsTask(compilation) ?: return files()
            filesProvider { friendArtifactsTaskProvider.flatMap { it.archiveFile } }
        }
    }

    private fun resolveFriendArtifactsTask(compilation: InternalKotlinCompilation<*>): TaskProvider<AbstractArchiveTask>? {
        if (compilation.associateWithClosure.none { it.isMain() }) return null
        val archiveTasks = compilation.project.tasks.withType(AbstractArchiveTask::class.java)
        if (compilation.target.artifactsTaskName !in archiveTasks.names) return null
        return archiveTasks.named(compilation.target.artifactsTaskName)
    }
}
