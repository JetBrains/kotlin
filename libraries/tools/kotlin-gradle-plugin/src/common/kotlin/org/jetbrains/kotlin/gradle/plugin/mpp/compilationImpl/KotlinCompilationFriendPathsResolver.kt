/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal interface KotlinCompilationFriendPathsResolver {
    fun resolveFriendPaths(compilation: InternalKotlinCompilation<*>): Iterable<FileCollection>
}

internal class DefaultKotlinCompilationFriendPathsResolver(
    private val friendArtifactResolver: FriendArtifactResolver = DefaultFriendArtifactResolver
) : KotlinCompilationFriendPathsResolver {


    override fun resolveFriendPaths(compilation: InternalKotlinCompilation<*>): Iterable<FileCollection> {
        return mutableListOf<FileCollection>().apply {
            compilation.allAssociatedCompilations.forEach {
                add(it.output.classesDirs)
                // Adding classes that could be produced to non-default destination for JVM target
                // Check KotlinSourceSetProcessor for details
                @Suppress("UNCHECKED_CAST")
                add(
                    compilation.project.files(
                        (it.compileTaskProvider as TaskProvider<KotlinCompileTool>).flatMap { task -> task.destinationDirectory }
                    )
                )
            }
            add(friendArtifactResolver.resolveFriendArtifacts(compilation))
        }
    }

    /* Resolution of friend artifacts */

    fun interface FriendArtifactResolver {
        fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection

        companion object {
            fun composite(vararg resolvers: FriendArtifactResolver?): FriendArtifactResolver {
                return CompositeFriendArtifactResolver(listOfNotNull(*resolvers))
            }
        }
    }

    class CompositeFriendArtifactResolver(
        private val resolvers: List<FriendArtifactResolver>
    ) : FriendArtifactResolver {
        override fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection {
            return compilation.project.files(resolvers.map { resolver -> resolver.resolveFriendArtifacts(compilation) })
        }
    }

    object DefaultFriendArtifactResolver : FriendArtifactResolver {
        override fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection {
            return with(compilation.project) {
                val friendArtifactsTaskProvider = resolveFriendArtifactsTask(compilation) ?: return files()
                filesProvider { friendArtifactsTaskProvider.flatMap { it.archiveFile } }
            }
        }

        private fun resolveFriendArtifactsTask(compilation: InternalKotlinCompilation<*>): TaskProvider<AbstractArchiveTask>? {
            if (compilation.allAssociatedCompilations.none { it.isMain() }) return null
            val archiveTasks = compilation.project.tasks.withType(AbstractArchiveTask::class.java)
            if (compilation.target.artifactsTaskName !in archiveTasks.names) return null
            return archiveTasks.named(compilation.target.artifactsTaskName)
        }
    }

    object AdditionalMetadataFriendArtifactResolver : FriendArtifactResolver {
        override fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection {
            val friendSourceSets = getVisibleSourceSetsFromAssociateCompilations(compilation.defaultSourceSet)
            return compilation.project.files(
                friendSourceSets.mapNotNull { compilation.target.compilations.findByName(it.name)?.output?.classesDirs }
            )
        }
    }

    object AdditionalAndroidFriendArtifactResolver : FriendArtifactResolver {
        override fun resolveFriendArtifacts(compilation: InternalKotlinCompilation<*>): FileCollection {
            return compilation.project.files((compilation.decoratedInstance as KotlinJvmAndroidCompilation).testedVariantArtifacts)
        }
    }
}
