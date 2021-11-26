/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

abstract class YarnLockCopyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Internal
    abstract val outputDirectory: RegularFileProperty

    @get:Internal
    abstract val fileName: Property<String>

    @get:OutputFile
    val outputFile: Provider<File>
        get() = outputDirectory.map { regularFile ->
            regularFile.asFile.resolve(fileName.get())
        }

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.copy { copy ->
            copy.from(inputFile)
            copy.rename { fileName.get() }
            copy.into(outputDirectory)
        }
    }
}