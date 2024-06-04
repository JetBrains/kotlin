/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.util.*
import javax.inject.Inject

internal data class LibraryDefinition(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val library: File,

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val swiftModule: File
) : Serializable

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class SwiftExportXCFrameworkTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val fileSystem: FileSystemOperations,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:Input
    abstract val binaryName: Property<String>

    @get:Nested
    abstract val libraryDefinitions: SetProperty<LibraryDefinition>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: DirectoryProperty

    @get:OutputDirectory
    abstract val frameworkRoot: DirectoryProperty

    @get:Internal
    val headersPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(frameworkRoot.dir("Headers"))
    }

    private val frameworkRootPath get() = frameworkRoot.getFile()
    private val frameworkName get() = binaryName.map { "${it}.xcframework" }.get()

    fun addLibrary(library: Provider<LibraryDefinition>) {
        libraryDefinitions.add(library)
    }

    @TaskAction
    fun assembleFramework() {
        createXCFramework()
        moveModules()
        cleanup()
    }

    private fun createXCFramework() {
        val frameworkPath = frameworkRootPath.resolve(frameworkName)
        frameworkPath.deleteRecursively()

        val inputLibs = libraryDefinitions.get().map { def ->
            listOf(
                "-library",
                def.library.relativeOrAbsolute(frameworkRootPath),
                "-headers",
                prepareHeaders(def.swiftModule).relativeOrAbsolute(frameworkRootPath)
            )
        }.flatten()

        val command = listOf(
            "xcodebuild",
            "-create-xcframework",
            "-allow-internal-distribution",
            "-output", frameworkName
        ) + inputLibs

        runCommand(
            command,
            logger = logger,
            processConfiguration = {
                directory(frameworkRootPath)
            }
        )
    }

    private fun prepareHeaders(swiftModule: File): File {
        val libHeaders = headersPath.getFile().resolve(UUID.randomUUID().toString()).apply { createDirectory() }
        val headers = includes.getFile().walkTopDown().filter { it.extension == "h" }.map { it }.toList()
        val modulemap = libHeaders.resolve("module.modulemap")
        headers.forEach { header ->
            modulemap.appendText(
                """
                |module "${header.parentFile.name}" {
                |   header "${header.name}"
                |   export *
                |}
                |
                """.trimMargin()
            )
        }

        fileSystem.copy {
            it.from(headers)
            it.into(libHeaders)
            it.includeEmptyDirs = false
        }

        fileSystem.copy {
            it.from(swiftModule)
            it.into(libHeaders)
            it.includeEmptyDirs = false
        }

        return libHeaders
    }

    private fun moveModules() {
        val frameworkPath = frameworkRootPath.resolve(frameworkName)

        frameworkPath.listFiles()?.let { arch ->
            arch.filter {
                it.isDirectory
            }.forEach { targetFramework ->
                val headers = targetFramework.resolve("Headers")
                val swiftModules = headers.walkTopDown().filter {
                    it.isDirectory && it.name.endsWith(".swiftmodule")
                }

                swiftModules.forEach { swiftModule ->
                    fileSystem.copy {
                        it.from(swiftModule)
                        it.into(targetFramework.resolve(swiftModule.name))
                    }

                    fileSystem.delete {
                        it.delete(swiftModule)
                    }
                }
            }
        }
    }

    private fun cleanup() {
        try {
            fileSystem.delete {
                it.delete(headersPath)
            }
        } catch (e: IOException) {
            logger.warn("Can't delete ${headersPath.getFile().absolutePath} folder", e)
        }
    }
}