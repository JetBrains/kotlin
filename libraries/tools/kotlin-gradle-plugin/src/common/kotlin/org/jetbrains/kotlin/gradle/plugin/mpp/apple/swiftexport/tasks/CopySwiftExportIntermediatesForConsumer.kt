/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.LibraryTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportConstants
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CopySwiftExportIntermediatesForConsumer @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    providerFactory: ProviderFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeBridgeDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeKotlinRuntimeDirectory: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val builtProductsDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
        projectLayout.dir(providerFactory.environmentVariable("BUILT_PRODUCTS_DIR").map {
            File(it)
        })
    )

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val libraries: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val interfaces: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    val syntheticInterfacesDestinationPath: DirectoryProperty = objectFactory.directoryProperty().convention(
        builtProductsDirectory.dir(SwiftExportConstants.KOTLIN_BRIDGE)
    )

    @get:Internal
    val kotlinRuntimeDestinationPath: DirectoryProperty = objectFactory.directoryProperty().convention(
        builtProductsDirectory.dir(SwiftExportConstants.KOTLIN_RUNTIME)
    )

    fun addLibrary(library: Provider<File>) {
        libraries.from(library)
    }

    fun addInterface(swiftInterface: Provider<File>) {
        interfaces.from(swiftInterface)
    }

    private val libraryTools by lazy { LibraryTools(logger) }

    @TaskAction
    fun copy() {
        mergeAndCopyLibrary()
        copyInterfaces()
        copyOtherIncludes()
    }

    private fun mergeAndCopyLibrary() {
        val libsInput = libraries.files.toList()
        if (libsInput.count() > 1) {
            val output = builtProductsDirectory.map { it.asFile.resolve(libraryName.get()) }.get()
            libraryTools.createFatLibrary(libsInput, output)
        } else {
            fileSystem.copy {
                it.from(libsInput.single())
                it.into(builtProductsDirectory)
                it.rename {
                    libraryName.get()
                }
            }
        }
    }

    private fun copyInterfaces() {
        interfaces.files.forEach { swiftInterface ->
            fileSystem.copy {
                it.from(swiftInterface)
                it.into(builtProductsDirectory)
                it.includeEmptyDirs = false
            }
        }
    }

    private fun copyOtherIncludes() {
        fileSystem.copy {
            it.from(includeBridgeDirectory)
            it.into(syntheticInterfacesDestinationPath)
        }

        fileSystem.copy {
            it.from(includeKotlinRuntimeDirectory)
            it.into(kotlinRuntimeDestinationPath)
        }
    }
}