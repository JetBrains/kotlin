/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases.split

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
    val configurables = context.config.platform.configurables
    val outputName = Path(outputFiles.outputName).nameWithoutExtension
    val hostObjectFile = temporaryFiles.create(outputName, ".host.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(splitCompilationOutput.hostBitcodePath, hostObjectFile))

    val bootstrapObjectPath = temporaryFiles.create(outputName, ".bootstrap.o")
    runAndMeasurePhase(
            ObjectFilesPhase,
            ObjectFilesPhaseInput(splitCompilationOutput.bootstrapBitcodePath, bootstrapObjectPath),
    )

    val manifest = resolveBootstrapMetadata(splitCompilationOutput.dependenciesTrackingResult, bootstrapObjectPath)
    val manifestObjectPath = generateManifestObject(manifest, temporaryFiles)

    val kaldoLinkerFlags = buildKaldoLinkerFlagsFrom(configurables)

    val linkerOutputKind = determineLinkerOutput(context)
    val linkerPhaseInput = LinkerPhaseInput(
            outputFiles.nativeBinaryFile,
            linkerOutputKind,
            listOf(hostObjectFile.absolutePathString(), manifestObjectPath.absolutePathString()),
            splitCompilationOutput.dependenciesTrackingResult,
            outputFiles,
            temporaryFiles,
            ResolvedCacheBinaries(manifest.forceLoadCaches, manifest.resolvedCaches.dynamicLibraries),
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

    val bootstrapObjectPath = temporaryFiles.create(outputFiles.outputName, ".bootstrap.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(moduleOutput.bitcodePath, bootstrapObjectPath))

    val manifest = resolveBootstrapMetadata(moduleOutput.dependenciesTrackingResult, bootstrapObjectPath)
    val manifestObjectPath = generateManifestObject(manifest, temporaryFiles)

    val kaldoLinkerFlags = buildKaldoLinkerFlagsFrom(configurables)

    val linkerOutputKind = determineLinkerOutput(context)
    val linkerPhaseInput = LinkerPhaseInput(
            outputFiles.mainFileName,
            linkerOutputKind,
            listOf(bootstrapObjectPath.absolutePathString(), manifestObjectPath.absolutePathString()),
            moduleOutput.dependenciesTrackingResult,
            outputFiles,
            temporaryFiles,
            manifest.resolvedCaches,
            kaldoLinkerFlags,
    )
    runAndMeasurePhase(LinkerPhase, linkerPhaseInput)
}
