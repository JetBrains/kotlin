/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleSdk
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.KOTLIN_CLANG_ARGS_DUMP_FILE_ENV
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.KOTLIN_LD_ARGS_DUMP_FILE_ENV
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

internal interface XcodebuildArgsDumpWorkParameters : WorkParameters {
    val xcodebuildPlatform: Property<String>
    val xcodebuildSdk: Property<String>
    val architectures: SetProperty<AppleArchitecture>
    val syntheticImportProjectRoot: DirectoryProperty
    val swiftPMDependenciesCheckout: DirectoryProperty
    val syntheticImportDd: DirectoryProperty
    val clangDumpIntermediatesDir: DirectoryProperty
    val additionalXcodeArgs: ListProperty<String>
}

internal abstract class XcodebuildArgsDumpWorkAction @Inject constructor(
    private val execOps: ExecOperations,
) : WorkAction<XcodebuildArgsDumpWorkParameters> {

    override fun execute() {
        val dumpIntermediates = parameters.clangDumpIntermediatesDir.getFile().also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdirs()
        }

        val clangArgsDumpScript = dumpIntermediates.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(XcodebuildDefFileUtils.clangArgsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = dumpIntermediates.resolve("clang_args_dump")
        clangArgsDump.mkdirs()

        val ldArgsDumpScript = dumpIntermediates.resolve("ldDump.sh")
        ldArgsDumpScript.writeText(XcodebuildDefFileUtils.ldArgsDumpScript())
        ldArgsDumpScript.setExecutable(true)
        val ldArgsDump = dumpIntermediates.resolve("ld_args_dump")
        ldArgsDump.mkdirs()

        runXcodebuildAndDumpArgs(
            architectures = parameters.architectures.get(),
            clangArgsDumpScript = clangArgsDumpScript,
            clangArgsDump = clangArgsDump,
            ldArgsDumpScript = ldArgsDumpScript,
            ldArgsDump = ldArgsDump,
        )
    }

    private fun runXcodebuildAndDumpArgs(
        architectures: Set<AppleArchitecture>,
        clangArgsDumpScript: File,
        clangArgsDump: File,
        ldArgsDumpScript: File,
        ldArgsDump: File,
    ) {
        val sdk = parameters.xcodebuildSdk.get()
        val targetArchitectures = architectures.map { it.xcodebuildArch }
        val projectRoot = parameters.syntheticImportProjectRoot.getFile()

        /**
         * For some reason reusing dd in parallel xcodebuild calls explodes something in the build system, so we need a
         * separate dd per "-destination" platform that can run in parallel
         */
        val dd = parameters.syntheticImportDd.getFile().resolve("dd_$sdk")

        // FIXME: KT-84809 - This is not great, but we can't remove entire DD on incremental runs
        val forceClangToReexecute =
            dd.resolve("Build/Intermediates.noindex/${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.build")
        if (forceClangToReexecute.exists()) {
            forceClangToReexecute.deleteRecursively()
        }

        execOps.exec { exec ->
            exec.workingDir(projectRoot)
            val args = mutableListOf(
                "xcodebuild", "build",
                "-scheme", GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME,
                "-destination", "generic/platform=${parameters.xcodebuildPlatform.get()}",
                "-derivedDataPath", dd.path,
                FetchSyntheticImportProjectPackages.XCODEBUILD_SWIFTPM_CHECKOUT_PATH_PARAMETER,
                parameters.swiftPMDependenciesCheckout.getFile().path,
                "CC=${clangArgsDumpScript.path}",
                "LD=${ldArgsDumpScript.path}",
                "ARCHS=${targetArchitectures.joinToString(" ")}",
                "CODE_SIGN_IDENTITY=",
                "COMPILER_INDEX_STORE_ENABLE=NO",
                "SWIFT_INDEX_STORE_ENABLE=NO",
            )

            args.addAll(parameters.additionalXcodeArgs.get())

            exec.commandLine(args)

            exec.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)
            exec.environment(KOTLIN_LD_ARGS_DUMP_FILE_ENV, ldArgsDump)

            val environmentToFilter = listOf(
                "EMBED_PACKAGE_RESOURCE_BUNDLE_NAMES",
            ) + AppleSdk.xcodeEnvironmentDebugDylibVars
            environmentToFilter.forEach {
                if (exec.environment.containsKey(it)) {
                    exec.environment.remove(it)
                }
            }
            exec.environment.keys.filter {
                it.startsWith("OTHER_") || it.startsWith("ASSETCATALOG_")
            }.forEach {
                exec.environment.remove(it)
            }
        }
    }
}
