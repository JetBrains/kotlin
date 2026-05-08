/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.decodeFromString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
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
    abstract val xcodebuildSdk: Property<String>

    @get:Input
    abstract val architectures: SetProperty<AppleArchitecture>

    @get:Input
    abstract val clangModules: SetProperty<String>

    @get:Input
    abstract val discoverModulesImplicitly: Property<Boolean>

    @get:Input
    abstract val hasSwiftPMDependencies: Property<Boolean>

    private val layout = project.layout

    @get:OutputDirectory
    protected val defFiles: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.defFilesRelativeDir(sdk))
    }

    @get:OutputDirectory
    protected val ldDump: Provider<org.gradle.api.file.Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.ldDumpRelativeDir(sdk))
    }

    @get:Internal
    abstract val xcodeDumpLocationFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    protected val xcodeDumpLocationFileInput = hasSwiftPMDependencies.map {
        if (it) {
            listOf(xcodeDumpLocationFile.get().asFile)
        } else {
            emptyList()
        }
    }

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
        if (hasSwiftPMDependencies.get()) {
            workerExecutor.noIsolation().submit(XcodebuildDefFileWorkAction::class.java) { params ->
                params.architectures.set(architectures)
                params.clangModules.set(clangModules)
                params.discoverModulesImplicitly.set(discoverModulesImplicitly)
                params.defFilesOutputDir.set(defFiles)
                params.ldDumpOutputDir.set(ldDump)
                params.dumpedXcodeBuildArgsDir.fileValue(resolveDumpedXcodeBuildArgsDir())
                params.cinteropNamespace.set(cinteropNamespace)
            }
        } else {
            workerExecutor.noIsolation().submit(EmptySwiftPMDefFileWorkAction::class.java) { params ->
                params.architectures.set(architectures)
                params.defFilesOutputDir.set(defFiles)
                params.ldDumpOutputDir.set(ldDump)
                params.cinteropNamespace.set(cinteropNamespace)
            }
        }
    }

    private fun resolveDumpedXcodeBuildArgsDir(): File {
        val location = dumpTaskFingerprintJson.decodeFromString<XcodeDumpLocation>(
            xcodeDumpLocationFile.get().asFile.readText()
        )
        return File(location.dumpedXcodeBuildArgsDir)
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
