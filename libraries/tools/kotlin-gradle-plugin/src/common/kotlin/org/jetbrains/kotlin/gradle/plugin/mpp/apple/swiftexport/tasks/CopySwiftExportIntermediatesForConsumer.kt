/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CopySwiftExportIntermediatesForConsumer @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    private val providerFactory: ProviderFactory,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val libraryName: Property<String>

    /**
     * Xcode's products directory. Not an input: Xcode writes there too, and the task's outputs are [copiedFiles].
     */
    @get:Internal
    val builtProductsDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
        projectLayout.dir(providerFactory.environmentVariable("BUILT_PRODUCTS_DIR").map {
            File(it)
        })
    )

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val library: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val interfaces: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    val filterInterfacesToOwnModules: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val swiftModulesFile: RegularFileProperty

    /**
     * The files the task writes into [builtProductsDirectory]. Computed from the inputs: Gradle reads a task's
     * outputs only after its producers ran, so the inputs exist by then.
     */
    @get:OutputFiles
    val copiedFiles: Provider<List<File>>
        get() = providerFactory.provider { copies().map { it.second } }

    fun addInterface(swiftInterface: Provider<File>) {
        interfaces.from(swiftInterface)
    }

    @TaskAction
    fun copy() {
        copies().forEach { (source, destination) -> source.copyTo(destination, overwrite = true) }
    }

    /** Source to destination, for every file the task copies. */
    private fun copies(): List<Pair<File, File>> {
        val destination = builtProductsDirectory.getFile()
        val ownModules = ownSwiftModuleNames()

        fun FileCollection.filesWithRelativePaths() = files.asSequence().flatMap { root ->
            root.walkTopDown().filter { it.isFile }.map { it to it.relativeTo(root) }
        }

        val interfaces = interfaces.filesWithRelativePaths().filter { (_, relative) ->
            ownModules == null || ownModules.any { relative.startsWith("$it.swiftmodule") }
        }
        val copied = (interfaces + includes.filesWithRelativePaths()).map { (file, relative) -> file to destination.resolve(relative) }
        return listOf(library.getFile() to destination.resolve(libraryName.get())) + copied
    }

    private fun ownSwiftModuleNames(): List<String>? {
        if (!filterInterfacesToOwnModules.get()) return null
        val modulesFile = swiftModulesFile.orNull?.asFile
            ?: error("swiftModulesFile must be set when filterInterfacesToOwnModules is enabled")
        return SerializationTools.readFromJson(modulesFile.readText()).modules.map { it.name }
    }
}
