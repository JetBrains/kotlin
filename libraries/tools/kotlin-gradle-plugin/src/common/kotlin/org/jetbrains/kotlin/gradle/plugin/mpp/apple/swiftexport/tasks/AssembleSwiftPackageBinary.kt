/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

/**
 * Puts the Kotlin static libraries of the Apple targets into one XCFramework. An XCFramework has one library
 * per [AppleTarget], so the libraries of targets that only differ in architecture are combined with `lipo`.
 */
@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class AssembleSwiftPackageBinary @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    /** File name of the library inside every slice, `lib<Module>Kotlin.a`. */
    @get:Input
    abstract val libraryName: Property<String>

    /** Directory name of the XCFramework, `<Module>Kotlin.xcframework`. */
    @get:Input
    abstract val xcframeworkName: Property<String>

    /** The libraries `xcodebuild` takes, one per [AppleTarget]. */
    @get:OutputDirectory
    abstract val fatLibrariesDirectory: DirectoryProperty

    /** The directory of the XCFramework. The export task copies all of it. */
    @get:OutputDirectory
    abstract val binaryDirectory: DirectoryProperty

    /** The libraries of all targets have the same file name, so only the absolute path tells them apart. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val libraries: ConfigurableFileCollection

    private val groupedLibraries: MutableMap<AppleTarget, MutableList<Provider<File>>> = linkedMapOf()

    /** How [libraries] are grouped, which [libraries] itself doesn't tell. */
    @get:Input
    val sliceNames: List<String>
        get() = groupedLibraries.keys.map { it.targetName }

    fun addLibrary(appleTarget: AppleTarget, library: Provider<File>) {
        groupedLibraries.getOrPut(appleTarget) { mutableListOf() }.add(library)
        libraries.from(library)
    }

    @TaskAction
    fun assemble() {
        if (groupedLibraries.isEmpty()) error("No Kotlin static libraries were added to $path")

        // Gradle keeps what a previous run left in an output directory, such as the XCFramework of a renamed module.
        fileSystemOperations.delete { it.delete(fatLibrariesDirectory, binaryDirectory) }

        val slices = groupedLibraries.map { (appleTarget, providers) ->
            sliceLibrary(appleTarget, providers.map { it.get() })
        }

        val xcframework = binaryDirectory.getFile().apply { mkdirs() }.resolve(xcframeworkName.get())
        execOperations.exec { spec ->
            spec.commandLine("xcodebuild", "-create-xcframework")
            slices.forEach { spec.args("-library", it.path) }
            spec.args("-output", xcframework.path)
        }
    }

    private fun sliceLibrary(appleTarget: AppleTarget, inputs: List<File>): File {
        val sliceDirectory = fatLibrariesDirectory.getFile().resolve(appleTarget.targetName).apply { mkdirs() }
        val slice = sliceDirectory.resolve(libraryName.get())
        if (inputs.size == 1) {
            inputs.single().copyTo(slice)
        } else {
            execOperations.exec { spec ->
                spec.commandLine("lipo", "-create", "-output", slice.path)
                spec.args(inputs.map { it.path })
            }
        }
        return slice
    }
}
