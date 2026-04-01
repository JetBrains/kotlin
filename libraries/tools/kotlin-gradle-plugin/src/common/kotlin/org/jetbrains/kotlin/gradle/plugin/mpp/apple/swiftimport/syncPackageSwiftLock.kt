/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.contentEquals
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class SyncPackageResolvedTask : DefaultTask() {

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun sync() {
        if (!sourceFile.isPresent) return

        val src = sourceFile.get().asFile
        val dest = destinationFile.get().asFile

        if (hasSameContent(src, dest)) return

        if (!dest.parentFile.exists()) dest.parentFile.mkdirs()

        copySwiftLockFile(fs, src, dest)
    }

    companion object {
        const val TASK_NAME = "syncPackageSwiftLockFile"
        const val SYNC_SYNTHETIC_TO_PROJECT_DIRECTORY_TASK_NAME = "syncPackageSwiftLockFileToProjectDirectory"
        const val SYNC_PROJECT_DIRECTORY_TO_SYNTHETIC_TASK_NAME = "syncPackageSwiftLockFileToSyntheticSwiftPMPackage"
    }
}

private fun copySwiftLockFile(
    fs: FileSystemOperations,
    src: File,
    dest: File,
) {
    fs.copy { spec ->
        spec.from(src)
        spec.into(dest.parentFile)
        spec.rename { dest.name }
    }
}

private fun hasSameContent(dest: File, src: File): Boolean = dest.exists() && src.exists() && contentEquals(src, dest)
