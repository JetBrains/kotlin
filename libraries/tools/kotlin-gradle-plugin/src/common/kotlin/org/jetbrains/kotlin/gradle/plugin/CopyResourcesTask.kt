/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.gradle.plugin.mpp.ComposeResources
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File

internal fun KotlinCompilation<*>.registerCopyResourcesTasks(
    prefix: String,
    resources: List<ComposeResources>,
    // FIXME: This task should probably create its own copy directory because it may otherwise do unexpected cleanup
    resourcesBaseCopyDirectory: Provider<Directory>,
): List<TaskProvider<Copy>> {
    // FIXME: This is a weird API that makes this method invokable only once
    // FIXME: This breaks build cache
    val cleanupTask = project.registerTask<Delete>("cleanBaseDirectoryFor${prefix}") {
        it.delete(resourcesBaseCopyDirectory)
    }

    return resources.map { resource ->
        registerCopyResourcesTask<Copy>(
            prefix,
            resource,
            resourcesBaseCopyDirectory
        ).also { it.dependsOn(cleanupTask) }
    }
}


// FIXME: Drop type parameter
private inline fun <reified T: AbstractCopyTask> KotlinCompilation<*>.registerCopyResourcesTask(
    prefix: String,
    resources: ComposeResources,
    resourcesBaseCopyDirectory: Provider<Directory>,
): TaskProvider<T> {
    val directoriesToCopy: MutableList<File> = mutableListOf()
    // Ignore files seen at lower levels
    val filesSeenAtLowerSourceSetLevels: MutableSet<String> = mutableSetOf()
    // Walk each source set only once
    val visitedSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    var sourceSetQueue: MutableList<KotlinSourceSet> = this.kotlinSourceSets.toMutableList()

    while (sourceSetQueue.isNotEmpty()) {
        val nextSourceSetQueue: MutableList<KotlinSourceSet> = mutableListOf()
        val relativePathsAtLevel: MutableMap<String, File> = mutableMapOf()
        while (sourceSetQueue.isNotEmpty()) {
            val sourceSet = sourceSetQueue.pop()
            if (sourceSet in visitedSourceSets) {
                continue
            }
            visitedSourceSets.add(sourceSet)

            sourceSet.kotlin.srcDirs.forEach {
                // FIXME: Source set path is not well-defined. Should there be another source set level API? (see defaultSourceFolder())
                // FIXME: resourceDirectoryName.get() -> move all of this to a task; actually maybe the directories must be task inputs, so it doesn't make sense to compute them at execution time
                val resourcesDirectory = it.parentFile.resolve(resources.resourceDirectoryName.get().toFile())
                if (resourcesDirectory.exists()) {
                    // Validate
                    resourcesDirectory.walk().forEach walkChildren@{ child ->
                        val relativePath = child.toRelativeString(resourcesDirectory)
                        if (relativePath in filesSeenAtLowerSourceSetLevels) {
                            return@walkChildren
                        }
                        if (relativePath in relativePathsAtLevel.keys) {
                            throw IllegalStateException("Duplicate resource found: \n${child.canonicalPath}\n${relativePathsAtLevel[relativePath]!!.canonicalPath}")
                        }
                        relativePathsAtLevel[relativePath] = resourcesDirectory
                    }
                    directoriesToCopy.add(resourcesDirectory)
                }
            }
            nextSourceSetQueue.addAll(sourceSet.dependsOn)
        }
        filesSeenAtLowerSourceSetLevels.addAll(relativePathsAtLevel.keys)
        sourceSetQueue = nextSourceSetQueue
    }

    val taskId = "copy${prefix}From${resources.taskName}"
    val copyTask = project.registerTask<T>(taskId) { copy ->
        // Reverse for a proper duplication strategy behavior
        directoriesToCopy.reversed().forEach {
            copy.from(it)
        }
        // FIXME: This needs to be a proper Provider
        copy.into(resourcesBaseCopyDirectory.flatMap { baseDir -> baseDir.dir(resources.resourceIdentity.map { it.toString() }) })
        copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    return copyTask
}
