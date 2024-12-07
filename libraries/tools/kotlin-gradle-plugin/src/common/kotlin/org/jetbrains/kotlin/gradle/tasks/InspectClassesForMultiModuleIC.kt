/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class InspectClassesForMultiModuleIC @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @get:Input
    internal abstract val archivePath: Property<String>

    @get:Input
    internal abstract val sourceSetName: Property<String>

    @get:OutputFile
    internal abstract val classesListFile: RegularFileProperty

    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    @get:NormalizeLineEndings
    internal abstract val sourceSetOutputClassesDir: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    internal val classFiles: FileCollection = objects.fileCollection()
        .from({ sourceSetOutputClassesDir.asFileTree.matching { it.include("**/*.class") } })

    @TaskAction
    fun run() {
        with(classesListFile.get().asFile) {
            parentFile.mkdirs()
            writeText(
                classFiles.map { it.absolutePath }.sorted().joinToString(File.pathSeparator)
            )
        }
    }
}
