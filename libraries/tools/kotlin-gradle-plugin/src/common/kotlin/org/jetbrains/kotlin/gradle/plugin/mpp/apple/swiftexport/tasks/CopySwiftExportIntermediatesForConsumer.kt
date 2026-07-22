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
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CopySwiftExportIntermediatesForConsumer @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    providerFactory: ProviderFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val libraryName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
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

    fun addInterface(swiftInterface: Provider<File>) {
        interfaces.from(swiftInterface)
    }

    @TaskAction
    fun copy() {
        copyLibrary()
        copyInterfaces()
        copyOtherIncludes()
    }

    private fun copyLibrary() {
        fileSystem.copy { spec ->
            spec.from(library)
            spec.into(builtProductsDirectory)
            spec.rename {
                libraryName.get()
            }
        }
    }

    private fun copyInterfaces() {
        val ownModulePatterns = ownSwiftModuleNames()?.map { "$it.swiftmodule/**" }
        interfaces.files.forEach { swiftInterface ->
            fileSystem.copy { spec ->
                spec.from(swiftInterface)
                spec.into(builtProductsDirectory)
                spec.includeEmptyDirs = false
                ownModulePatterns?.forEach { spec.include(it) }
            }
        }
    }

    private fun ownSwiftModuleNames(): List<String>? {
        if (!filterInterfacesToOwnModules.get()) return null
        val modulesFile = swiftModulesFile.orNull?.asFile
            ?: error("swiftModulesFile must be set when filterInterfacesToOwnModules is enabled")
        return SerializationTools.readFromJson(modulesFile.readText()).modules.map { it.name }
    }

    private fun copyOtherIncludes() {
        includes.forEach { include ->
            fileSystem.copy { spec ->
                spec.from(include)
                spec.into(builtProductsDirectory)
            }
        }
    }
}
