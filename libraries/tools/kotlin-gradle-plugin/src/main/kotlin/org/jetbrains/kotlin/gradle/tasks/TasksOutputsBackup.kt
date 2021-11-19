/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import java.io.File

internal class TaskOutputsBackup(
    val fileSystemOperations: FileSystemOperations,
    val buildDirectory: DirectoryProperty,
    val snapshotsDir: Provider<Directory>,

    allOutputs: List<File>,

    /**
     * Task outputs that we don't want to back up for performance reasons (e.g., if (1) they are too big, and (2) they are usually updated
     * only at the end of the task execution--in a failed task run, they are usually unchanged and therefore don't need to be restored).
     *
     * NOTE: In `IncrementalCompilerRunner`, if incremental compilation fails, it will try again by cleaning all the outputs and perform
     * non-incremental compilation. It is important that `IncrementalCompilerRunner` do not clean [outputsToExclude] immediately but only
     * right before [outputsToExclude] are updated (which is usually at the end of the task execution). This is so that if the fallback
     * compilation fails, [outputsToExclude] will remain unchanged and the other outputs will be restored, and the next task run can be
     * incremental.
     */
    outputsToExclude: List<File> = emptyList()
) {
    /** The outputs to back up and restore. Note that this may be a subset of all the outputs of a task (see `outputsToExclude`). */
    val outputs: List<File> = allOutputs - outputsToExclude.toSet()

    fun createSnapshot() {
        // Kotlin JS compilation task declares one file from 'destinationDirectory' output as task `@OutputFile'
        // property. To avoid snapshot sync collisions, each snapshot output directory has also 'index' as prefix.
        outputs.toSortedSet().forEachIndexed { index, outputPath ->
            val pathInSnapshot = "$index${File.separator}${outputPath.pathRelativeToBuildDirectory}"
            if (outputPath.isDirectory) {
                fileSystemOperations.sync { spec ->
                    spec.from(outputPath)
                    spec.into(snapshotsDir.map { it.dir(pathInSnapshot) })
                }
            } else {
                fileSystemOperations.copy { spec ->
                    spec.from(outputPath)
                    spec.into(snapshotsDir.map { it.file(pathInSnapshot).asFile.parentFile })
                }
            }
        }
    }

    fun restoreOutputs() {
        fileSystemOperations.delete {
            it.delete(outputs)
        }

        outputs.toSortedSet().forEachIndexed { index, outputPath ->
            val pathInSnapshot = "$index${File.separator}${outputPath.pathRelativeToBuildDirectory}"
            val fileInSnapshot = snapshotsDir.get().file(pathInSnapshot).asFile
            if (fileInSnapshot.isDirectory) {
                fileSystemOperations.sync { spec ->
                    spec.from(snapshotsDir.map { it.dir(pathInSnapshot) })
                    spec.into(outputPath)
                }
            } else {
                fileSystemOperations.copy { spec ->
                    spec.from(snapshotsDir.map { it.file(pathInSnapshot).asFile.parentFile })
                    spec.into(outputPath.parentFile)
                }
            }
        }
    }

    fun deleteSnapshot() {
        fileSystemOperations.delete { it.delete(snapshotsDir) }
    }

    private val File.pathRelativeToBuildDirectory: String
        get() {
            val buildDir = buildDirectory.get().asFile
            return relativeTo(buildDir).path
        }
}
