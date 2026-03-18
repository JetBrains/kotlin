/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.contentEquals
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class SyncPackageSwiftLockFileToProjectDirectory : DefaultTask() {

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    @get:Optional
    @get:OutputFile
    val projectDirLockFile = project.layout.projectDirectory.file("Package.resolved")

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val syntheticProjectLockFile  = syntheticImportProjectRoot.file("Package.resolved")

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun syncPackageSwiftSyntheticLockFileToProjectDirectory() {
        if (!syntheticProjectLockFile.isPresent) return

        val src = syntheticProjectLockFile.getFile()
        val dest = projectDirLockFile.asFile

        if (hasSameContent(dest, src)) return

        copySwiftLockFile(fs, src, dest)
    }

    companion object Companion {
        const val TASK_NAME = "syncPackageSwiftLockFileToRoot"
    }
}


@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class SyncPackageSwiftLockFileToSynthetic : DefaultTask() {

    @get:Internal
    val syntheticImportProjectRoot: DirectoryProperty = project.objects.directoryProperty()

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val projectDirLockFile = project.layout.projectDirectory.file("Package.resolved")

    @get:OutputFile
    val syntheticProjectLockFile  = syntheticImportProjectRoot.file("Package.resolved")

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun syncPackageSwiftLockFileToSyntheticProject() {
        if(!projectDirLockFile.asFile.exists()) return

        val dest = syntheticProjectLockFile.getFile()
        val src = projectDirLockFile.asFile

        if (hasSameContent(src, dest)) return

        dest.parentFile.mkdirs()

        copySwiftLockFile(fs, src, dest)
    }

    companion object{
        const val TASK_NAME = "syncPackageSwiftLockFileToSyntheticProject"
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

private fun hasSameContent(dest: File, src: File): Boolean =
    dest.exists() && src.exists() && contentEquals(src, dest)
