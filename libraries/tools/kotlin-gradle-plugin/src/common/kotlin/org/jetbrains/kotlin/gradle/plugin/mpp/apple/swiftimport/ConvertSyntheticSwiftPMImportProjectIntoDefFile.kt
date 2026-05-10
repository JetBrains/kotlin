/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class ConvertSyntheticSwiftPMImportProjectIntoDefFile : DefaultTask() {

    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:Input
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val localPackageSources: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map {
            it.asFile.readLines().filter { line -> line.isNotEmpty() }.map { line -> File(line) }
        }

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resolvedPackagesState: ConfigurableFileCollection

    private val layout = project.layout

    @get:OutputDirectory
    protected val defFiles: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.defFilesRelativeDir(sdk))
    }

    @get:OutputDirectory
    protected val ldDump: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.ldDumpRelativeDir(sdk))
    }

    /**
     * Additional arguments to pass to `xcodebuild` when resolving SwiftPM dependencies.
     *
     * Generally used in test to:
     * To avoid cache collisions between test runs, we generate a unique package name (and therefore URL) for each execution.
     * e.g "Revision ... for TestPackageA version 1.0.0 does not match previously recorded value ..."
     * or
     * Optional SwiftPM repository cache override.
     * Passed to `xcodebuild` as:
     * -packageCachePath <dir>
     * Used in tests to avoid collisions with the global cache at `~/Library/Caches/org.swift.swiftpm/repositories`.
     */
    @get:Internal
    abstract val additionalXcodeArgs: ListProperty<String>

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:Internal
    val syntheticImportDd: Provider<Directory> =
        layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    private val cinteropNamespace = listOf(
        "swiftPMImport",
        project.group.toString(),
        if (project.path == ":") project.name else project.path.drop(1)
    ).filter {
        it.isNotEmpty()
    }.joinToString(".") {
        it.replace(Regex("[^a-zA-Z0-9_.]"), ".")
    }.replace(Regex("\\.{2,}"), ".")  // Replace multiple consecutive dots with single dot
        .trim('.')  // Remove leading/trailing dots

    @TaskAction
    fun generateDefFiles() {
        workerExecutor.noIsolation().submit(XcodebuildDefFileWorkAction::class.java) { params ->
            val sdk = xcodebuildSdk.get()
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.clangModules.set(clangModules)
            params.discoverModulesImplicitly.set(discoverModulesImplicitly)
            params.hasSwiftPMDependencies.set(hasSwiftPMDependencies)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.set(syntheticImportDd)
            params.defFilesOutputDir.set(defFiles)
            params.ldDumpOutputDir.set(ldDump)
            params.clangDumpIntermediatesDir.set(layout.buildDirectory.dir(XcodebuildDefFileUtils.clangDumpRelativeDir(sdk)))
            params.additionalXcodeArgs.set(additionalXcodeArgs)
            params.cinteropNamespace.set(cinteropNamespace)
        }
    }

    fun defFilePath(architecture: AppleArchitecture): Provider<RegularFile> =
        defFiles.map { directory -> directory.file(XcodebuildDefFileUtils.defFileName(architecture)) }

    /**
     * The difference between these is that for dynamic framework linkage we never want -filelist as we expect it to contain .o files
     * which are instead going to be exported through our SwiftPM dylib product.
     */
    fun ldFilePath(architecture: AppleArchitecture): Provider<RegularFile> =
        ldDump.map { directory -> directory.file(XcodebuildDefFileUtils.ldFileName(architecture)) }

    fun ldFileForFrameworkLinkagePath(architecture: AppleArchitecture): Provider<RegularFile> =
        ldDump.map { directory -> directory.file(XcodebuildDefFileUtils.frameworkLdFileName(architecture)) }

    fun ldFileFingerprintPath(architecture: AppleArchitecture): Provider<RegularFile> =
        ldDump.map { directory -> directory.file(XcodebuildDefFileUtils.ldFingerprintFileName(architecture)) }

    fun frameworkSearchpathFilePath(architecture: AppleArchitecture): Provider<RegularFile> =
        ldDump.map { directory -> directory.file(XcodebuildDefFileUtils.frameworkSearchpathFileName(architecture)) }

    fun librarySearchpathFilePath(architecture: AppleArchitecture): Provider<RegularFile> =
        ldDump.map { directory -> directory.file(XcodebuildDefFileUtils.librarySearchpathFileName(architecture)) }

    companion object {
        const val TASK_NAME = "convertSyntheticImportProjectIntoDefFile"
    }
}
