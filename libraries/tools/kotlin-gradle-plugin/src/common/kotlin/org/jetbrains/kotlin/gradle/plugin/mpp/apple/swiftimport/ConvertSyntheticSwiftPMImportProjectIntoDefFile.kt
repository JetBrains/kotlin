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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
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

    @get:Internal
    abstract val xcodebuildPlatform: Property<String>

    @get:Internal
    abstract val xcodebuildSdk: Property<String>

    @get:Internal
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Internal
    abstract val clangModules: SetProperty<String>

    @get:Internal
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Internal
    abstract val hasSwiftPMDependencies: Property<Boolean>

    @get:Internal
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:Internal
    abstract val resolvedPackagesState: ConfigurableFileCollection

    @get:Internal
    abstract val additionalXcodeArgs: ListProperty<String>

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    private val layout = project.layout

    @get:Internal
    val syntheticImportDd: Provider<Directory> =
        layout.buildDirectory.dir(XcodebuildDefFileUtils.SYNTHETIC_IMPORT_DD_DIR)

    private val defFiles: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.defFilesRelativeDir(sdk))
    }

    private val ldDump: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.ldDumpRelativeDir(sdk))
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
        const val RUN_XCODEBUILD_TASK_NAME = "runXcodebuildForSwiftPMDefFile"
        const val WRITE_DEF_FILE_TASK_NAME = "writeSwiftPMDefFileFromXcodebuildDumps"
        const val WRITE_EMPTY_OUTPUTS_TASK_NAME = "writeEmptySwiftPMDefFileOutputs"
    }
}

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class RunXcodebuildForSwiftPMDefFile : DefaultTask() {

    @get:Input
    abstract val xcodebuildPlatform: Property<String>

    @get:Input
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

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

    @get:Optional
    @get:Input
    abstract val additionalXcodeArgs: ListProperty<String>

    @get:Internal
    abstract val swiftPMDependenciesCheckout: DirectoryProperty

    @get:Internal
    abstract val syntheticImportProjectRoot: DirectoryProperty

    @get:LocalState
    abstract val syntheticImportDd: DirectoryProperty

    @get:OutputDirectory
    abstract val clangDumpIntermediatesDir: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun runXcodebuild() {
        workerExecutor.noIsolation().submit(XcodebuildArgsDumpWorkAction::class.java) { params ->
            params.xcodebuildPlatform.set(xcodebuildPlatform)
            params.xcodebuildSdk.set(xcodebuildSdk)
            params.architectures.set(architectures)
            params.syntheticImportProjectRoot.set(syntheticImportProjectRoot)
            params.swiftPMDependenciesCheckout.set(swiftPMDependenciesCheckout)
            params.syntheticImportDd.set(syntheticImportDd)
            params.clangDumpIntermediatesDir.set(clangDumpIntermediatesDir)
            params.additionalXcodeArgs.set(additionalXcodeArgs)
        }
    }
}

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class WriteSwiftPMDefFileFromXcodebuildDumps : DefaultTask() {

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:Input
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Input
    abstract val cinteropNamespace: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clangDumpIntermediatesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val defFilesOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val ldDumpOutputDir: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun writeOutputs() {
        workerExecutor.noIsolation().submit(XcodebuildDefFileWorkAction::class.java) { params ->
            params.architectures.set(architectures)
            params.clangModules.set(clangModules)
            params.discoverModulesImplicitly.set(discoverModulesImplicitly)
            params.defFilesOutputDir.set(defFilesOutputDir)
            params.ldDumpOutputDir.set(ldDumpOutputDir)
            params.clangDumpIntermediatesDir.set(clangDumpIntermediatesDir)
            params.cinteropNamespace.set(cinteropNamespace)
        }
    }
}

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class WriteEmptySwiftPMDefFileOutputs : DefaultTask() {

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val cinteropNamespace: Property<String>

    @get:OutputDirectory
    abstract val defFilesOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val ldDumpOutputDir: DirectoryProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun writeOutputs() {
        workerExecutor.noIsolation().submit(EmptySwiftPMDefFileWorkAction::class.java) { params ->
            params.architectures.set(architectures)
            params.defFilesOutputDir.set(defFilesOutputDir)
            params.ldDumpOutputDir.set(ldDumpOutputDir)
            params.cinteropNamespace.set(cinteropNamespace)
        }
    }
}
