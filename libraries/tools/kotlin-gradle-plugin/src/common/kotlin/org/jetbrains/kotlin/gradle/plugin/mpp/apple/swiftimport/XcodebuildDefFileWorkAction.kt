/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import java.io.File

internal interface XcodebuildDefFileWorkParameters : WorkParameters {
    val architectures: SetProperty<AppleArchitecture>
    val clangModules: SetProperty<String>
    val discoverModulesImplicitly: Property<Boolean>
    val defFilesOutputDir: DirectoryProperty
    val ldDumpOutputDir: DirectoryProperty
    val clangDumpIntermediatesDir: DirectoryProperty
    val cinteropNamespace: Property<String>
}

internal abstract class XcodebuildDefFileWorkAction : WorkAction<XcodebuildDefFileWorkParameters> {

    override fun execute() {
        writeDefAndLinkerOutputs(
            architectures = parameters.architectures.get(),
            cinteropNamespace = parameters.cinteropNamespace.get(),
            defFilesDir = parameters.defFilesOutputDir.getFile(),
            ldDumpDir = parameters.ldDumpOutputDir.getFile(),
            clangDumpIntermediatesDir = parameters.clangDumpIntermediatesDir.getFile(),
        )
    }

    private fun writeDefAndLinkerOutputs(
        architectures: Set<AppleArchitecture>,
        cinteropNamespace: String,
        defFilesDir: File,
        ldDumpDir: File,
        clangDumpIntermediatesDir: File,
    ) {
        val clangArgsDump = clangDumpIntermediatesDir.resolve("clang_args_dump")
        val ldArgsDump = clangDumpIntermediatesDir.resolve("ld_args_dump")
        val discoverModulesImplicitly = parameters.discoverModulesImplicitly.get()
        val clangModulesFromParams = parameters.clangModules.get()

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
}
