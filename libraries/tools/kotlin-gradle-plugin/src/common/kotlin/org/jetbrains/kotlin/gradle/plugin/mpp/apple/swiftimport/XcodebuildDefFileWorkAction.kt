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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.DUMP_FILE_ARGS_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.KOTLIN_CLANG_ARGS_DUMP_FILE_ENV
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XcodebuildDefFileUtils.KOTLIN_LD_ARGS_DUMP_FILE_ENV
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import java.io.File
import javax.inject.Inject

internal interface XcodebuildDefFileWorkParameters : WorkParameters {
    val xcodebuildPlatform: Property<String>
    val xcodebuildSdk: Property<String>
    val architectures: SetProperty<AppleArchitecture>
    val clangModules: SetProperty<String>
    val discoverModulesImplicitly: Property<Boolean>
    val hasSwiftPMDependencies: Property<Boolean>
    val syntheticImportProjectRoot: DirectoryProperty
    val swiftPMDependenciesCheckout: DirectoryProperty
    val syntheticImportDd: DirectoryProperty
    val defFilesOutputDir: DirectoryProperty
    val ldDumpOutputDir: DirectoryProperty
    val clangDumpIntermediatesDir: DirectoryProperty
    val additionalXcodeArgs: ListProperty<String>
    val cinteropNamespace: Property<String>
}

internal abstract class XcodebuildDefFileWorkAction @Inject constructor(
    private val execOps: ExecOperations,
) : WorkAction<XcodebuildDefFileWorkParameters> {

    override fun execute() {
        val sdk = parameters.xcodebuildSdk.get()
        val architectures = parameters.architectures.get()
        val cinteropNamespace = parameters.cinteropNamespace.get()
        val defFilesDir = parameters.defFilesOutputDir.getFile()
        val ldDumpDir = parameters.ldDumpOutputDir.getFile()

        if (!parameters.hasSwiftPMDependencies.get()) {
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
            return
        }

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
                // ScanDependencies explode with duplicate modules because it reads this env for some reason
                it.startsWith("OTHER_")
                        // Also some asset catalogs utility explodes
                        || it.startsWith("ASSETCATALOG_")
            }.forEach {
                exec.environment.remove(it)
            }
        }

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
