/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases.split

import com.intellij.openapi.util.io.toNioPathOrNull
import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.CExportPaths
import org.jetbrains.kotlin.backend.konan.llvm.getName
import org.jetbrains.kotlin.backend.konan.llvm.objc.patchObjCRuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.split.createObjCExportConvertersModule
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModulesConfig
import org.jetbrains.kotlin.backend.konan.util.absoluteNormalizedPathString
import org.jetbrains.kotlin.config.nativeBinaryOptions.CCallMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import java.nio.file.Path
import kotlin.io.path.*

private const val HOST_MODULE_NAME: String = "split_host"

sealed interface SplitCompilationOutput {
    val bootstrapBitcodePath: Path
    val dependenciesTrackingResult: DependenciesTrackingResult
}

internal data class HostModuleSplitCompilationOutput(
        val hostBitcodePath: Path,
        override val bootstrapBitcodePath: Path,
        override val dependenciesTrackingResult: DependenciesTrackingResult,
) : SplitCompilationOutput

private fun buildKaldoLinkerFlagsFrom(configurables: Configurables): List<String> = buildList {
    add("-L${configurables.absoluteLlvmHome}/lib")
    addAll(configurables.kaldoLinkerFlags)
    add("-all_load") // We need to import all KN runtimes symbols
    add("-export_dynamic")
    addAll(listOf("-rpath", "${configurables.absoluteLlvmHome}/lib"))
}

/**
 * Deduplicates an `.a` archive by extracting members and re-archiving unique ones.
 * This is a hacky solution, duplicated objects **should not** exist within archives.
 *
 * If the archive has no duplicates, it returns the original path unchanged.
 */
private fun deduplicateArchive(
        archiveFilename: String,
        dedupDirPath: Path,
        config: NativeSecondStageCompilationConfig,
): String {
    // TODO(Gabriele): this function is a temporary fix, it needs to be removed. Symbol duplication should not happen.
    // TODO(Gabriele): Some cache archives (e.g. skiko) contain duplicate `.o` members which cause
    // TODO(Gabriele): "duplicate symbol" errors when used with `-force_load`.

    val archivePath = archiveFilename.toNioPathOrNull() ?: return archiveFilename

    val ar = "${config.platform.configurables.absoluteLlvmHome}/bin/llvm-ar"

    val listProcess = ProcessBuilder(ar, "t", archiveFilename)
            .redirectErrorStream(true).start()

    val members = listProcess.inputStream.bufferedReader().readLines()
    require(listProcess.waitFor() == 0) {
        "ar (listing) failed with exit code different from zero"
    }

    if (members.size == members.toSet().size) return archiveFilename

    val name = archivePath.nameWithoutExtension
    val extractDir = dedupDirPath.resolve(name).also { it.createDirectory() }
    val arExtractProcess = ProcessBuilder(ar, "x", archiveFilename)
            .directory(extractDir.toFile())
            .redirectErrorStream(true)
            .start()

    require(arExtractProcess.waitFor() == 0) {
        "ar (extraction) failed with exit code different from zero"
    }

    val dedupPath = dedupDirPath.resolve("${name}-dedup.a")
    val objectFiles = extractDir.listDirectoryEntries("*.o").map { it.name }
    val arCreateProcess = ProcessBuilder(listOf(ar, "rcs", dedupPath.absolutePathString()) + objectFiles)
            .directory(extractDir.toFile())
            .redirectErrorStream(true)
            .start()

    check(arCreateProcess.waitFor() == 0) {
        "ar (creation) failed with exit code different from zero"
    }

    return dedupPath.absolutePathString()
}

/**
 * Links library bitcode (interop stubs) into the bootstrap module for hot reload.
 */
private fun linkLibraryBitcodeForBootstrapObject(
        generationState: NativeGenerationState,
        generatedBitcodeFiles: List<Path>
) {
    // This function links only the library bitcode containing interop stubs (knifunptr_*, etc.)
    // without the C++ runtime. The runtime object code comes from caches (specifically, `kotlin.native.internal`).
    val config = generationState.config
    val additionalProducedBitcodeFiles = generationState.llvm.additionalProducedBitcodeFiles

    // This contains interop stubs (knifunptr_*) from platform libraries
    val bitcodeLibraries = generationState.dependenciesTracker.bitcodeToLink
            .filterNot { it.isCInteropLibrary() && config.cCallMode == CCallMode.Direct }
            .filterNot { it.isNativeStdlib } // Skip stdlib bitcode (it's in stdlib-cache.a)
            .flatMap { it.bitcode(config.target)?.bitcodeFilePaths.orEmpty() }
            .filter { it.isBitcode }

    val bitcodeFilesToLink = buildList {
        addAll(generatedBitcodeFiles.map { it.absoluteNormalizedPathString() })
        addAll(additionalProducedBitcodeFiles)
        addAll(bitcodeLibraries)
    }

    if (bitcodeFilesToLink.isEmpty()) {
        generationState.log { "No library bitcode to link into bootstrap" }
        return
    }

    // Parse and link each bitcode file into the bootstrap module
    val bootstrapModule = generationState.llvmModule
    bitcodeFilesToLink.forEach { bitcodeFile ->
        parseAndLinkBitcodeFile(generationState, bootstrapModule, bitcodeFile.toString())
    }
}

/**
 * Compile the module containing only user defined Kotlin program.
 */
internal fun PhaseEngine<NativeGenerationState>.generateGuestBitcode(
        userModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        guestBitcodePath: Path,
        cExportPaths: CExportPaths?,
) {
    val (generatedBitcodePaths) = runBackendCodegen(userModule, irBuiltIns, cExportPaths)
    linkLibraryBitcodeForBootstrapObject(context, generatedBitcodePaths)
    runPostCodegen()
    runAndMeasurePhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(context.llvm.module, guestBitcodePath))
}

/**
 * Generate the host bitcode file, containing the entry point to the program.
 */
internal fun PhaseEngine<NativeGenerationState>.generateHostBitcode(
        hostBitcodePath: Path
) {
    // TODO(Gabriele): At the of writing of this function, it is not easy to decouple the functions
    // TODO(Gabriele): needed to generate an isolated host-module. So, what we do is quite simple:
    // TODO(Gabriele): we only compile the Obj-C patch for host, and the Runtime+Stdlib will come from caches.

    // TODO(Gabriele): REMEMBER! The cache flavor should contain the hot-reload-enable runtime :)

    fun LLVMModuleRef.linkOther(module: LLVMModuleRef): LLVMModuleRef {
        val linkFailed = LLVMLinkModules2(this, module)
        if (linkFailed != 0) {
            error("failed to link module '${module.getName()}' into '${this.getName()}'")
        }
        return this
    }

    val runtimeModulesConfig = RuntimeModulesConfig(context.config)
    val hotReloadLauncherModule = parseBitcodeFile(
            context,
            context.diagnosticReporter,
            context.llvmContext,
            runtimeModulesConfig.absolutePathFor(RuntimeModule.HOT_RELOAD_LAUNCHER)
    )

    var hostModule = LLVMModuleCreateWithNameInContext(HOST_MODULE_NAME, context.llvmContext)!!
    hostModule = hostModule.linkOther(hotReloadLauncherModule)

    if (context.config.target.family.isAppleFamily) {
        val objcPatchModule = patchObjCRuntimeModule(context)!!
        hostModule = hostModule.linkOther(objcPatchModule)

        val convertersModule = createObjCExportConvertersModule(context.llvmContext).apply {
            LLVMSetDataLayout(this, context.runtime.dataLayout)
        }
        hostModule = hostModule.linkOther(convertersModule)
    }

    runAndMeasurePhase(InsertEntryPointAliasPhase, InsertEntryPointAliasInput(hostModule, context.config.entryPointName))
    runAndMeasurePhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(hostModule, hostBitcodePath))
}

internal fun PhaseEngine<NativeGenerationState>.compileAndLinkForSplitHost(
        splitCompilationOutput: HostModuleSplitCompilationOutput,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {
    // TODO(Gabriele): the linker use -filelist.
    // TODO(Gabriele): but this is interesting, on theory since we're using cache we don't need to create
    // TODO(Gabriele): an extra object for the bootstrap but we can load it directly from the cache.
    val configurables = context.config.platform.configurables
    val hostObjectFile = temporaryFiles.create(outputFiles.outputName, ".host.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(splitCompilationOutput.hostBitcodePath, hostObjectFile))

    val manifest = resolveBootstrapMetadata(splitCompilationOutput.dependenciesTrackingResult)
    val manifestObjectPath = generateManifestObject(manifest, temporaryFiles)

    val allLoadCaches = manifest.forceLoadCaches
    val resolvedCaches = manifest.resolvedCaches

    val dedupDir = temporaryFiles.create("dedup", "").also { it.createDirectory() }
    val allLoadList = allLoadCaches.map { archivePath ->
        deduplicateArchive(archivePath, dedupDir, context.config)
    }

    val kaldoLinkerFlags = buildKaldoLinkerFlagsFrom(configurables)

    val linkerOutputKind = determineLinkerOutput(context)
    val linkerPhaseInput = LinkerPhaseInput(
            outputFiles.nativeBinaryFile,
            linkerOutputKind,
            listOf(hostObjectFile.absolutePathString(), manifestObjectPath.absolutePathString()),
            splitCompilationOutput.dependenciesTrackingResult,
            outputFiles,
            temporaryFiles,
            ResolvedCacheBinaries(allLoadList, resolvedCaches.dynamic),
            kaldoLinkerFlags
    )

    runAndMeasurePhase(LinkerPhase, linkerPhaseInput)
}

internal fun PhaseEngine<NativeGenerationState>.compileAndLinkSplitFramework(
        moduleOutput: ModuleCompilationOutput,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {

    // The framework binary contains the full module (runtime + user code + ObjC stubs + class metadata).
    // The user code in the binary is dead weight, live execution goes through HotReload runtime module.
    val configurables = context.config.platform.configurables

    val manifest = resolveBootstrapMetadata(moduleOutput.dependenciesTrackingResult)
    val manifestObjectPath = generateManifestObject(manifest, temporaryFiles)

    val resolvedCaches = resolveCacheBinaries(context.config.cachedLibraries, moduleOutput.dependenciesTrackingResult)

    val kaldoLinkerFlags = buildKaldoLinkerFlagsFrom(configurables)

    val linkerOutputKind = determineLinkerOutput(context)
    val linkerPhaseInput = LinkerPhaseInput(
            outputFiles.mainFileName,
            linkerOutputKind,
            listOf(manifestObjectPath.absolutePathString()),
            moduleOutput.dependenciesTrackingResult,
            outputFiles,
            temporaryFiles,
            ResolvedCacheBinaries(resolvedCaches.static, resolvedCaches.dynamic),
            kaldoLinkerFlags,
    )
    runAndMeasurePhase(LinkerPhase, linkerPhaseInput)
}
