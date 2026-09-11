/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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
 * Wraps the Kotlin static libraries of every Apple target into one static-library XCFramework, one slice per
 * [AppleTarget] platform group. Groups with several architectures (a simulator built for arm64 and x64, say)
 * are combined with `lipo` first, because an XCFramework holds one library per platform.
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

    /**
     * Where the per-platform (possibly `lipo`-combined) libraries are written before `xcodebuild` wraps them.
     * A declared output so that Gradle's stale-output cleanup removes the slices of a target that was dropped
     * from the build; it is a directory of its own under `build/SwiftPackage/<Configuration>`, disjoint from
     * [binaryDirectory].
     */
    @get:OutputDirectory
    abstract val fatLibrariesDirectory: DirectoryProperty

    /**
     * The directory the XCFramework is assembled into. Declared as the output rather than the XCFramework
     * itself, so that Gradle removes a stale `<Old>Kotlin.xcframework` left behind by a renamed module instead
     * of letting the export Sync copy it into the user's package.
     */
    @get:OutputDirectory
    abstract val binaryDirectory: DirectoryProperty

    @get:Internal
    val xcframework: Provider<Directory>
        get() = binaryDirectory.zip(xcframeworkName) { directory, name -> directory.dir(name) }

    /**
     * ABSOLUTE, like [org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask.inputFrameworkFiles]: this is
     * a flat collection of per-target link outputs, whose relative path degenerates to the file name, and every
     * target's Kotlin static library is named the same.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val libraries: ConfigurableFileCollection

    private val groupedLibraries: MutableMap<AppleTarget, MutableList<Provider<File>>> = linkedMapOf()

    /**
     * The platform groups the XCFramework gets a slice for, in the order they were added. [libraries] alone
     * doesn't describe the grouping, and the grouping decides the content of the XCFramework.
     */
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

        val slices = groupedLibraries.map { (appleTarget, providers) ->
            sliceLibrary(appleTarget, providers.map { it.get() })
        }

        val output = xcframework.getFile()
        if (output.exists()) output.deleteRecursively()
        output.parentFile.mkdirs()

        execOperations.exec { it.commandLine(xcodebuildArguments(slices, output)) }
    }

    private fun sliceLibrary(appleTarget: AppleTarget, inputs: List<File>): File {
        val sliceDirectory = fatLibrariesDirectory.getFile().resolve(appleTarget.targetName).apply { mkdirs() }
        val slice = sliceDirectory.resolve(libraryName.get())
        if (inputs.size == 1) {
            fileSystemOperations.copy { spec ->
                spec.from(inputs.single())
                spec.into(sliceDirectory)
                spec.rename { libraryName.get() }
            }
        } else {
            lipo(inputs, slice)
        }
        return slice
    }

    /**
     * The same call [org.jetbrains.kotlin.gradle.plugin.mpp.apple.LibraryTools.createFatLibrary] makes, run
     * through [ExecOperations]. `LibraryTools` is shared with the Xcode flow, which has no `ExecOperations`
     * at hand, so it is left alone.
     */
    private fun lipo(inputs: List<File>, output: File) {
        execOperations.exec {
            it.commandLine(listOf("lipo", "-create", "-output", output.absolutePath) + inputs.map { input -> input.absolutePath })
        }
    }

    companion object {
        fun xcodebuildArguments(libraries: List<File>, output: File): List<String> =
            listOf("xcodebuild", "-create-xcframework") +
                    libraries.flatMap { listOf("-library", it.path) } +
                    listOf("-output", output.path)
    }
}
