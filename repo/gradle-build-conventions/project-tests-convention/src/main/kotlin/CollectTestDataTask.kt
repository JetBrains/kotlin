/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.build.project.tests

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * A task that scans provided testData directories and writes a list of relative .kt files
 * into build/testDataInfo/testDataFilesList.txt
 */
abstract class CollectTestDataTask : DefaultTask() {
    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val rootDirPath: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testDataFiles: ListProperty<Directory>

    @get:OutputFile
    abstract val targetFile: RegularFileProperty

    @TaskAction
    fun run() {
        val directories = testDataFiles.get()
        if (directories.isEmpty()) {
            throw GradleException("No testData directories provided for ${projectName.get()}")
        }
        val rootDir = File(rootDirPath.get())
        val outFile = targetFile.get().asFile
        outFile.parentFile.mkdirs()

        val text = directories.flatMap { directory ->
            directory.asFileTree.matching { include("**/*.kt", "**/*.kt.can-freeze-ide") }.files
        }.sorted().joinToString("\n") {
            it.relativeTo(rootDir).path.replace('\\', '/')
        }

        outFile.writeText(text)
    }
}
