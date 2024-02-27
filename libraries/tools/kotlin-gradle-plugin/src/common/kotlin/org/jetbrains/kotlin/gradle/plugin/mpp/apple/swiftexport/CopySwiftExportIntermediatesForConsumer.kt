/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "This task only copies files")
internal abstract class CopySwiftExportIntermediatesForConsumer @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    providerFactory: ProviderFactory,
    private val fileSystem: FileSystemOperations
) : DefaultTask() {
    companion object {
        private const val KOTLIN_RUNTIME = "KotlinRuntime"
        private const val KOTLIN_BRIDGE = "KotlinBridge"
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeBridgeDirectory: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeKotlinRuntimeDirectory: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinLibraryPath: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val syntheticLibraryPath: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val syntheticInterfacesPath: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val builtProductsDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
        projectLayout.dir(providerFactory.environmentVariable("BUILT_PRODUCTS_DIR").map {
            File(it)
        })
    )

    @get:OutputDirectory
    val syntheticInterfacesDestinationPath: DirectoryProperty = objectFactory.directoryProperty().convention(
        builtProductsDirectory.flatMap {
            projectLayout.dir(providerFactory.provider {
                it.asFile.resolve(KOTLIN_BRIDGE)
            })
        }
    )

    @get:OutputDirectory
    val kotlinRuntimeDestinationPath: DirectoryProperty = objectFactory.directoryProperty().convention(
        builtProductsDirectory.flatMap {
            projectLayout.dir(providerFactory.provider {
                it.asFile.resolve(KOTLIN_RUNTIME)
            })
        }
    )

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
        fileSystem.copy {
            it.from(includeKotlinRuntimeDirectory)
            it.into(kotlinRuntimeDestinationPath)
        }
    }
}