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
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CopySwiftExportIntermediatesForConsumer @Inject constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    val builtProductsDirectory: Property<String> = objectFactory.property(String::class.java).convention(
        providerFactory.environmentVariable("BUILT_PRODUCTS_DIR")
    )

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val library: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val interfaces: ConfigurableFileCollection = objectFactory.fileCollection()

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
            spec.into(builtProductsDirectory.get())
            spec.rename {
                libraryName.get()
            }
        }
    }

    private fun copyInterfaces() {
        interfaces.files.forEach { swiftInterface ->
            fileSystem.copy { spec ->
                spec.from(swiftInterface)
                spec.into(builtProductsDirectory.get())
                spec.includeEmptyDirs = false
            }
        }
    }

    private fun copyOtherIncludes() {
        includes.forEach { include ->
            fileSystem.copy { spec ->
                spec.from(include)
                spec.into(builtProductsDirectory.get())
            }
        }
    }
}