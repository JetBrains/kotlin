/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class BcvApiDumpTask
@Inject
constructor(
    private val fs: FileSystemOperations,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apiDumpFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val apiDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        fs.sync {
            from(apiDumpFiles) {
                include("**/*.api")
            }
            into(apiDirectory)
        }
    }
}
