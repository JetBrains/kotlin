/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class CopySwiftExportIntermediatesForConsumer : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Inject
    abstract val providerFactory: ProviderFactory

    @get:InputDirectory
    abstract val includeBridgeDirectory: Property<File>

    @get:InputFile
    abstract val kotlinLibraryPath: Property<File>

    @get:InputFile
    abstract val syntheticLibraryPath: Property<File>

    @get:InputDirectory
    abstract val syntheticInterfacesPath: Property<File>

    @get:InputDirectory
    val builtProductsDirectory: Provider<String> get() = providerFactory.environmentVariable("BUILT_PRODUCTS_DIR")

    @get:OutputFile
    val syntheticLibraryDestinationPath: Provider<File> get() = builtProductsDirectory.map { File(it).resolve(syntheticLibraryPath.get().name) }

    @get:OutputFile
    val kotlinLibraryDestinationPath: Provider<File> get() = builtProductsDirectory.map { File(it).resolve(kotlinLibraryPath.get().name) }

    @get:OutputDirectory
    val syntheticInterfacesDestinationPath: Provider<File> get() = builtProductsDirectory.map { File(it).resolve("KotlinBridge") }

    @TaskAction
    fun copy() {
        fileSystem.copy {
            it.from(kotlinLibraryPath)
            it.from(syntheticInterfacesPath)
            it.from(syntheticLibraryPath)
            it.into(builtProductsDirectory)
        }
        fileSystem.copy {
            it.from(includeBridgeDirectory)
            it.into(syntheticInterfacesDestinationPath)
        }
    }
}