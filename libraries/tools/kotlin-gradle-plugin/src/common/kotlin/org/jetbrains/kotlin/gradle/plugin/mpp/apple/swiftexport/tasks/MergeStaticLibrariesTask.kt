/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.LibraryTools
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class MergeStaticLibrariesTask : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:SkipWhenEmpty
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraries: ConfigurableFileCollection

    @get:OutputFile
    abstract val library: RegularFileProperty

    private val libraryTools by lazy { LibraryTools(logger) }

    fun addLibrary(library: Provider<File>) {
        libraries.from(library)
    }

    @TaskAction
    fun mergeLibraries() {
        val inputLibs = libraries.files.toList()
        val outputLib = library.getFile()

        libraryTools.mergeLibraries(inputLibs, outputLib)
    }
}