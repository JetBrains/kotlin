/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.gradle.plugin.mpp.ComposeResources
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File
import java.nio.file.Path

internal fun KotlinCompilation<*>.registerCopyComposeResourcesTasks(
    targetNamePrefix: String,
    resources: List<ComposeResources>,
    // FIXME: This task should probably create its own copy directory because it may otherwise do unexpected cleanup
    resourcesBaseCopyDirectory: Provider<Directory>,
): List<TaskProvider<*>> {
    // FIXME: This is a weird API that makes this method invokable only once
    // FIXME: This breaks up-to-date checks
//    val cleanupTask = project.registerTask<Delete>("${targetNamePrefix}CleanBaseDirectory") {
//        it.delete(resourcesBaseCopyDirectory)
//    }

    return resources.map { resource ->
        registerCopyComposeResourcesTask(
            targetNamePrefix,
            resource,
            resourcesBaseCopyDirectory
        ).also {
//            it.dependsOn(cleanupTask)
        }
    }
}

// FIXME: This function should suspend until source set graph is finalized
private fun KotlinCompilation<*>.registerCopyComposeResourcesTask(
    targetNamePrefix: String,
    resources: ComposeResources,
    resourcesBaseCopyDirectory: Provider<Directory>,
): TaskProvider<*> {
    val directoriesToCopy = findDirectoriesToCopy(
        resources = resources,
        rootSourceSets = kotlinSourceSets.toList(),
    )

    val copyTask = project.registerTask<Copy>("${targetNamePrefix}CopyResourcesFor${resources.resourceTypeTaskNameSuffix}") { copy ->
        directoriesToCopy.forEach {
            copy.from(it)
        }
        copy.into(resourcesBaseCopyDirectory.flatMap { baseDir -> baseDir.dir(resources.resourcePlacementPathRelativeToPublicationRoot.map { it.toString() }) })
        copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    return copyTask
}

private fun findDirectoriesToCopy(
    resources: ComposeResources,
    rootSourceSets: List<KotlinSourceSet>,
): List<File> {
    // 1. Is case of a collision between levels, files seen at next levels overwrite files from previous levels. E.g. iosMain/res/foo resources overwrites commonMain/res/foo
    val directoriesToCopy: MutableList<File> = mutableListOf()
    val relativePathsSeenAtPreviousLevels: MutableSet<String> = mutableSetOf()
    // Walk each source set only once
    val visitedSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    var sourceSetsAtThisLevel: MutableList<KotlinSourceSet> = rootSourceSets.toMutableList()

    while (sourceSetsAtThisLevel.isNotEmpty()) {
        val sourceSetsAtNextLevel: MutableList<KotlinSourceSet> = mutableListOf()
        // 2. Collision on a single level throws an exception
        val relativePathsSeenAtThisLevel: MutableMap<String, File> = mutableMapOf()
        while (sourceSetsAtThisLevel.isNotEmpty()) {
            val sourceSet = sourceSetsAtThisLevel.pop()
            if (sourceSet in visitedSourceSets) {
                continue
            }
            visitedSourceSets.add(sourceSet)

            discoverAndAppendResourceDirectory(
                sourceSet = sourceSet,
                resourceDirectoryPathRelativeToSourceSet = resources.resourceDirectoryPathRelativeToSourceSet,
                relativePathsSeenAtPreviousLevels = relativePathsSeenAtPreviousLevels,
                relativePathsSeenAtThisLevel = relativePathsSeenAtThisLevel,
                directoriesToCopy = directoriesToCopy,
            )

            // FIXME: dependsOn is a Set with non-deterministic ordering
            sourceSetsAtNextLevel.addAll(sourceSet.dependsOn)
        }
        relativePathsSeenAtPreviousLevels.addAll(relativePathsSeenAtThisLevel.keys)
        sourceSetsAtThisLevel = sourceSetsAtNextLevel
    }

    // See 1
    return directoriesToCopy.reversed()
}

private fun discoverAndAppendResourceDirectory(
    sourceSet: KotlinSourceSet,
    resourceDirectoryPathRelativeToSourceSet: Provider<Path>,
    // FIXME: Should this be a Set<File>?
    relativePathsSeenAtPreviousLevels: Set<String>,
    relativePathsSeenAtThisLevel: MutableMap<String, File>,
    directoriesToCopy: MutableList<File>,
) {
    sourceSet.kotlin.srcDirs.forEach {
        // FIXME: Source set path is not well-defined. Should there be another source set level API? (see defaultSourceFolder()) How do we walk source set graph elsewhere?
        // FIXME: resourceDirectoryPathRelativeToSourceSet.get() -> could this be a task or must the directories be task inputs, so it doesn't make sense to compute them at execution time
        val resourcesDirectory = it.parentFile.resolve(resourceDirectoryPathRelativeToSourceSet.get().toFile())
        if (resourcesDirectory.exists()) {
            // FIXME: Validate this actually works
            // FIXME: Is resourcesDirectory.walk() deterministic?
            resourcesDirectory.walk().forEach walkChildren@{ child ->
                val relativePath = child.toRelativeString(resourcesDirectory)
                if (relativePath in relativePathsSeenAtPreviousLevels) {
                    return@walkChildren
                }
                if (relativePath in relativePathsSeenAtThisLevel.keys) {
                    // FIXME: Aggregate all errors and then throw
                    throw IllegalStateException("There is a duplicate resource in a source set level:\n${child.canonicalPath}\n${relativePathsSeenAtThisLevel[relativePath]!!.canonicalPath}")
                }
                relativePathsSeenAtThisLevel[relativePath] = resourcesDirectory
            }
            directoriesToCopy.add(resourcesDirectory)
        }
    }
}