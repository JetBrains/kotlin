/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
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

    @get:Internal
    abstract val xcodeDumpsDir: DirectoryProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val localPackageSourcesDumpMarker: RegularFileProperty

    private val layout = project.layout

    @get:OutputDirectory
    protected val defFiles: Provider<Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.defFilesRelativeDir(sdk))
    }

    @get:OutputDirectory
    protected val ldDump: Provider<Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.ldDumpRelativeDir(sdk))
    }

    @get:Internal
    val syntheticDumpDir: Provider<Directory> = xcodebuildSdk.flatMap { sdk ->
        layout.buildDirectory.dir(XcodebuildDefFileUtils.clangDumpRelativeDir(sdk))
    }

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xcodebuildFingerprint: RegularFileProperty

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
        val architectures = architectures.get()
        val defFiles = defFiles.getFile()
        val ldDump = ldDump.getFile()

        if (!hasSwiftPMDependencies.get()) {
            writeEmptyDefAndLinkerOutputs(
                architectures,
                cinteropNamespace,
                defFiles,
                ldDump,
            )
            return
        }

        val dumpedXcodeBuildArgsDir = if (xcodebuildFingerprint.isPresent) {
            resolveDumpedXcodeBuildArgsDir()
        } else {
            syntheticDumpDir.get().asFile
        }

        writeDefAndLinkerOutputs(
            architectures,
            cinteropNamespace,
            defFiles,
            ldDump,
            dumpedXcodeBuildArgsDir,
        )
    }

    private fun writeEmptyDefAndLinkerOutputs(
        architectures: Set<AppleArchitecture>,
        cinteropNamespace: String,
        defFilesDir: File,
        ldDumpDir: File,
    ) {
        architectures.forEach { architecture ->
            defFilesDir.resolve(XcodebuildDefFileUtils.defFileName(architecture)).writeText(
                """
                    language = Objective-C
                    package = $cinteropNamespace
                """.trimIndent()
            )
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkLdFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFingerprintFileName(architecture)).writeText("0")
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkSearchpathFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.librarySearchpathFileName(architecture)).writeText("\n")
        }
    }

    private fun writeDefAndLinkerOutputs(
        architectures: Set<AppleArchitecture>,
        cinteropNamespace: String,
        defFilesDir: File,
        ldDumpDir: File,
        dumpedXcodeBuildArgsDir: File,
    ) {
        val clangArgsDump = dumpedXcodeBuildArgsDir.resolve("clang_args_dump")
        val ldArgsDump = dumpedXcodeBuildArgsDir.resolve("ld_args_dump")
        val discoverModulesImplicitly = discoverModulesImplicitly.get()
        val clangModulesFromParams = clangModules.get()

        architectures.forEach { architecture ->
            val clangArchitecture = architecture.clangArch
            val architectureSpecificProductClangCalls = mutableListOf<File>()

            clangArgsDump.listFilesOrEmpty().filter {
                it.isFile
            }.forEach {
                val clangArgs = it.readLines().single()
                val isArchitectureSpecificProductClangCall =
                    "-fmodule-name=${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}" in clangArgs
                            && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in clangArgs
                if (isArchitectureSpecificProductClangCall) {
                    architectureSpecificProductClangCalls.add(it)
                }
            }

            val parsedClangCall = XcodebuildDefFileUtils.parseClangCall(architectureSpecificProductClangCalls.single())

            val clangModules = if (discoverModulesImplicitly) {
                XcodebuildDefFileUtils.discoverClangModules(parsedClangCall)
            } else clangModulesFromParams

            XcodebuildDefFileUtils.writeDefFile(
                parsedClangCall = parsedClangCall,
                clangModules = clangModules,
                architecture = architecture,
                defFilesDir = defFilesDir,
                cinteropNamespace = cinteropNamespace,
                discoverModulesImplicitly = discoverModulesImplicitly,
            )

            val architectureSpecificProductLdCalls = ldArgsDump.listFilesOrEmpty().filter {
                it.isFile
            }.filter {
                val ldArgs = it.readLines().single()
                ("@rpath/lib${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.dylib" in ldArgs || "@rpath/${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.framework" in ldArgs)
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in ldArgs
            }

            val parsedLdCall = XcodebuildDefFileUtils.parseLdCall(architectureSpecificProductLdCalls.single())


            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFileName(architecture))
                .writeText(parsedLdCall.ldArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkLdFileName(architecture))
                .writeText(parsedLdCall.frameworkLdArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFingerprintFileName(architecture))
                .writeText(System.currentTimeMillis().toString())
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkSearchpathFileName(architecture))
                .writeText(parsedLdCall.linkTimeFrameworkSearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
            ldDumpDir.resolve(XcodebuildDefFileUtils.librarySearchpathFileName(architecture))
                .writeText(parsedLdCall.librarySearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR))
        }
    }

    private fun resolveDumpedXcodeBuildArgsDir(): File {
        val hash = xcodebuildFingerprint.get().asFile.readText().trim()

        return xcodeDumpsDir.get().asFile.resolve("$hash/swiftImportClangDump/${xcodebuildSdk.get()}")
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
