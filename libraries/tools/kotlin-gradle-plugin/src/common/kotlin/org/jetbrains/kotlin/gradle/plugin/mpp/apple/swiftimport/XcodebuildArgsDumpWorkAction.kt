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
    /** Synthetic SwiftPM project that xcodebuild builds only to expose compiler/linker invocations. */
    val syntheticImportProjectRoot: DirectoryProperty
    /** SwiftPM checkout directory used by xcodebuild package resolution. */
    val swiftPMDependenciesCheckout: DirectoryProperty
    /** DerivedData root selected by the owning dump task/bucket. */
    val syntheticImportDd: DirectoryProperty
    /** Directory where wrapper scripts and captured clang/ld argument files are written. */
    val dumpedXcodeBuildArgsDir: DirectoryProperty
    val additionalXcodeArgs: ListProperty<String>
}

/**
 * Runs xcodebuild with custom CC/LD wrappers to record the compiler and linker arguments for the synthetic SwiftPM
 * project. The wrappers do not produce the final K/N def file directly; they capture enough information for
 * [XcodebuildDefFileWorkAction] to derive cinterop compiler/linker options later.
 */
internal abstract class XcodebuildArgsDumpWorkAction @Inject constructor(
    private val execOps: ExecOperations,
) : WorkAction<XcodebuildArgsDumpWorkParameters> {

    override fun execute() {
        // Start from an empty dump directory. xcodebuild can skip parts of the build on incremental runs, so keeping
        // stale captured invocations would make the def-file phase see calls from a previous package graph.
        val dumpedXcodeBuildArgsDir = parameters.dumpedXcodeBuildArgsDir.getFile().also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdirs()
        }

        val clangArgsDumpScript = dumpedXcodeBuildArgsDir.resolve("clangDump.sh")
        clangArgsDumpScript.writeText(XcodebuildDefFileUtils.clangArgsDumpScript())
        clangArgsDumpScript.setExecutable(true)
        val clangArgsDump = dumpedXcodeBuildArgsDir.resolve("clang_args_dump")
        clangArgsDump.mkdirs()

        val ldArgsDumpScript = dumpedXcodeBuildArgsDir.resolve("ldDump.sh")
        ldArgsDumpScript.writeText(XcodebuildDefFileUtils.ldArgsDumpScript())
        ldArgsDumpScript.setExecutable(true)
        val ldArgsDump = dumpedXcodeBuildArgsDir.resolve("ld_args_dump")
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
         * DerivedData must be SDK-specific because Gradle can run iphoneos and iphonesimulator dump tasks in parallel.
         * Sharing one DerivedData directory across destinations makes xcodebuild race on its internal build database.
         */
        val dd = parameters.syntheticImportDd.getFile().resolve("dd_$sdk")

        // FIXME: KT-84809 - This is not great, but we can't remove entire DD on incremental runs.
        // We delete only the synthetic dylib target intermediates to force xcodebuild to call the wrapper scripts again.
        // Other DerivedData contents are kept because SwiftPM/Xcode may need products and module maps from dependency
        // targets, and deleting the whole directory is too expensive for incremental dump runs.
        val forceClangToReexecute =
            dd.resolve("Build/Intermediates.noindex/${GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_DYLIB}.build")
        if (forceClangToReexecute.exists()) {
            forceClangToReexecute.deleteRecursively()
        }

        execOps.exec { exec ->
            exec.workingDir(projectRoot)
            // Building the synthetic package is intentional: xcodebuild computes the same clang/ld invocations that the
            // real SwiftPM package integration would use, including module maps, framework search paths, and products.
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

            // The wrapper scripts use these environment variables to know where to write captured invocations.
            exec.environment(KOTLIN_CLANG_ARGS_DUMP_FILE_ENV, clangArgsDump)
            exec.environment(KOTLIN_LD_ARGS_DUMP_FILE_ENV, ldArgsDump)

            // Some environment variables are injected by the surrounding Gradle/Xcode test environment and can change
            // how xcodebuild links the synthetic package. The dump should represent the SwiftPM graph itself, not the
            // app embedding context that happened to launch Gradle.
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
