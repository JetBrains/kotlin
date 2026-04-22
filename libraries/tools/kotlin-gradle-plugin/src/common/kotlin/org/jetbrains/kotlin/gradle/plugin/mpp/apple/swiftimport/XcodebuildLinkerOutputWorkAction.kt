/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.stableFingerprint
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.writeTextIfChanged
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty

internal interface XcodebuildLinkerOutputWorkParameters : WorkParameters {
    val architectures: SetProperty<AppleArchitecture>
    val ldDumpOutputDir: DirectoryProperty
    val ldDumpIntermediatesDir: DirectoryProperty
}

internal abstract class XcodebuildLinkerOutputWorkAction : WorkAction<XcodebuildLinkerOutputWorkParameters> {
    override fun execute() {
        val ldArgsDump = parameters.ldDumpIntermediatesDir.getFile().resolve("ld_args_dump")
        val ldDumpDir = parameters.ldDumpOutputDir.getFile()

        parameters.architectures.get().forEach { architecture ->
            val clangArchitecture = architecture.clangArch
            val architectureSpecificProductLdCalls = ldArgsDump.listFilesOrEmpty().filter {
                it.isFile
            }.filter {
                val ldArgs = it.readLines().single()
                ("@rpath/lib${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.dylib" in ldArgs || "@rpath/${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.framework" in ldArgs)
                        && "-target${DUMP_FILE_ARGS_SEPARATOR}${clangArchitecture}-apple" in ldArgs
            }

            val parsedLdCall = XcodebuildDefFileUtils.parseLdCall(architectureSpecificProductLdCalls.single())
            val ldArgsContent = parsedLdCall.ldArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR)
            val frameworkLdArgsContent = parsedLdCall.frameworkLdArgs.joinToString(DUMP_FILE_ARGS_SEPARATOR)
            val frameworkSearchPathsContent = parsedLdCall.linkTimeFrameworkSearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR)
            val librarySearchPathsContent = parsedLdCall.librarySearchPaths.joinToString(DUMP_FILE_ARGS_SEPARATOR)
            val fingerprintContent = stableFingerprint(
                listOf(
                    ldArgsContent,
                    frameworkLdArgsContent,
                    frameworkSearchPathsContent,
                    librarySearchPathsContent,
                ).joinToString("\n")
            )

            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFileName(architecture))
                .writeTextIfChanged(ldArgsContent)
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkLdFileName(architecture))
                .writeTextIfChanged(frameworkLdArgsContent)
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFingerprintFileName(architecture))
                .writeTextIfChanged(fingerprintContent)
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkSearchpathFileName(architecture))
                .writeTextIfChanged(frameworkSearchPathsContent)
            ldDumpDir.resolve(XcodebuildDefFileUtils.librarySearchpathFileName(architecture))
                .writeTextIfChanged(librarySearchPathsContent)
        }
    }
}
