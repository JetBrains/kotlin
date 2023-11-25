/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.patchObjCRuntimeModule
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File

/**
 * Supposed to be true for a single LLVM module within final binary.
 */
val KonanConfig.isFinalBinary: Boolean get() = when (this.produce) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC -> true
    CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE,
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
    CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
    CompilerOutputKind.TEST_BUNDLE -> true
    else -> error("not supported: ${this.produce}")
}

val CompilerOutputKind.isNativeLibrary: Boolean
    get() = this == CompilerOutputKind.DYNAMIC || this == CompilerOutputKind.STATIC

/**
 * Return true if compiler has to generate a C API for dynamic/static library.
 */
val KonanConfig.produceCInterface: Boolean
    get() = this.produce.isNativeLibrary && this.cInterfaceGenerationMode != CInterfaceGenerationMode.NONE

val CompilerOutputKind.involvesBitcodeGeneration: Boolean
    get() = this != CompilerOutputKind.LIBRARY

internal val CacheDeserializationStrategy?.containsKFunctionImpl: Boolean
    get() = this?.contains(KonanFqNames.internalPackageName, "KFunctionImpl.kt") != false

internal val NativeGenerationState.shouldDefineFunctionClasses: Boolean
    get() = producedLlvmModuleContainsStdlib && cacheDeserializationStrategy.containsKFunctionImpl

internal val NativeGenerationState.shouldDefineCachedBoxes: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            cacheDeserializationStrategy?.contains(KonanFqNames.internalPackageName, "Boxing.kt") != false

internal val CacheDeserializationStrategy?.containsRuntime: Boolean
    get() = this?.contains(KonanFqNames.internalPackageName, "Runtime.kt") != false

internal val NativeGenerationState.shouldLinkRuntimeNativeLibraries: Boolean
    get() = producedLlvmModuleContainsStdlib && cacheDeserializationStrategy.containsRuntime

val CompilerOutputKind.isCache: Boolean
    get() = this == CompilerOutputKind.STATIC_CACHE || this == CompilerOutputKind.DYNAMIC_CACHE

internal fun produceCStubs(generationState: NativeGenerationState) {
    generationState.cStubsManager.compile(
            generationState.config.clang,
            generationState.messageCollector,
            generationState.inVerbosePhase
    ).forEach {
        parseAndLinkBitcodeFile(generationState, generationState.llvm.module, it.absolutePath)
    }
}

private data class LlvmModules(
        val runtimeModules: List<LLVMModuleRef>,
        val additionalModules: List<LLVMModuleRef>
)

/**
 * Deserialize, generate, patch all bitcode dependencies and classify them into two sets:
 * - Runtime modules. These may be used as an input for a separate LTO (e.g. for debug builds).
 * - Everything else.
 */
private fun collectLlvmModules(generationState: NativeGenerationState, generatedBitcodeFiles: List<String>): LlvmModules {
    val config = generationState.config

    val (bitcodePartOfStdlib, bitcodeLibraries) = generationState.dependenciesTracker.bitcodeToLink
            .partition { it.isNativeStdlib && generationState.producedLlvmModuleContainsStdlib }
            .toList()
            .map { libraries ->
                libraries.flatMap { it.bitcodePaths }.filter { it.isBitcode }
            }

    val nativeLibraries = config.nativeLibraries + config.launcherNativeLibraries
            .takeIf { config.produce == CompilerOutputKind.PROGRAM }.orEmpty()
    val additionalBitcodeFilesToLink = generationState.llvm.additionalProducedBitcodeFiles
    val exceptionsSupportNativeLibrary = listOf(config.exceptionsSupportNativeLibrary)
            .takeIf { config.produce == CompilerOutputKind.DYNAMIC_CACHE }.orEmpty()
    val xcTestRunnerNativeLibrary = listOf(config.xcTestLauncherNativeLibrary)
            .takeIf { config.produce == CompilerOutputKind.TEST_BUNDLE }.orEmpty()
    val additionalBitcodeFiles = nativeLibraries +
            generatedBitcodeFiles +
            additionalBitcodeFilesToLink +
            bitcodeLibraries +
            exceptionsSupportNativeLibrary +
            xcTestRunnerNativeLibrary

    val runtimeNativeLibraries = config.runtimeNativeLibraries


    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(generationState.llvmContext, bitcodeFile)
        if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
            LLVMStripModuleDebugInfo(parsedModule)
        }
        parsedModule
    }

    val runtimeModules = parseBitcodeFiles(
            (runtimeNativeLibraries + bitcodePartOfStdlib)
                    .takeIf { generationState.shouldLinkRuntimeNativeLibraries }.orEmpty()
    )
    val additionalModules = parseBitcodeFiles(additionalBitcodeFiles)
    return LlvmModules(
            runtimeModules.ifNotEmpty { this + generationState.generateRuntimeConstantsModule() } ?: emptyList(),
            additionalModules + listOfNotNull(patchObjCRuntimeModule(generationState))
    )
}

private fun linkAllDependencies(generationState: NativeGenerationState, generatedBitcodeFiles: List<String>) {
    val (runtimeModules, additionalModules) = collectLlvmModules(generationState, generatedBitcodeFiles)
    // TODO: Possibly slow, maybe to a separate phase?
    val optimizedRuntimeModules = RuntimeLinkageStrategy.pick(generationState, runtimeModules).run()

    // When the main module `generationState.llvmModule` is very large it is much faster to
    // link all the auxiliary modules together first before linking with the main module.
    val linkedModules = (optimizedRuntimeModules + additionalModules).reduceOrNull { acc, module ->
        val failed = llvmLinkModules2(generationState, acc, module)
        if (failed != 0) {
            error("Failed to link ${module.getName()}")
        }
        return@reduceOrNull acc
    }
    linkedModules?.let {
        val failed = llvmLinkModules2(generationState, generationState.llvmModule, it)
        if (failed != 0) {
            error("Failed to link runtime and additional modules into main module")
        }
    }
}

internal fun insertAliasToEntryPoint(context: PhaseContext, module: LLVMModuleRef) {
    val config = context.config
    val nomain = config.configuration.get(KonanConfigKeys.NOMAIN) ?: false
    if (config.produce != CompilerOutputKind.PROGRAM || nomain)
        return
    val entryPointName = config.entryPointName
    val entryPoint = LLVMGetNamedFunction(module, entryPointName)
            ?: error("Module doesn't contain `$entryPointName`")
    val programAddressSpace = LLVMGetProgramAddressSpace(module)
    LLVMAddAlias2(module, getGlobalFunctionType(entryPoint), programAddressSpace, entryPoint, "main")
}

internal fun linkBitcodeDependencies(generationState: NativeGenerationState,
                                     generatedBitcodeFiles: List<File>) {
    val config = generationState.config
    val produce = config.produce

    if (produce == CompilerOutputKind.FRAMEWORK && config.produceStaticFramework) {
        embedAppleLinkerOptionsToBitcode(generationState.llvm, config)
    }
    linkAllDependencies(generationState, generatedBitcodeFiles.map { it.absoluteFile.normalize().path })

}

private fun parseAndLinkBitcodeFile(generationState: NativeGenerationState, llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(generationState.llvmContext, path)
    if (!generationState.shouldUseDebugInfoFromNativeLibs()) {
        LLVMStripModuleDebugInfo(parsedModule)
    }
    val failed = llvmLinkModules2(generationState, llvmModule, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $path")
    }
}

private fun embedAppleLinkerOptionsToBitcode(llvm: CodegenLlvmHelpers, config: KonanConfig) {
    fun findEmbeddableOptions(options: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val iterator = options.iterator()
        loop@while (iterator.hasNext()) {
            val option = iterator.next()
            result += when {
                option.startsWith("-l") -> listOf(option)
                option == "-framework" && iterator.hasNext() -> listOf(option, iterator.next())
                else -> break@loop // Ignore the rest.
            }
        }
        return result
    }

    val optionsToEmbed = findEmbeddableOptions(config.platform.configurables.linkerKonanFlags) +
            llvm.dependenciesTracker.allNativeDependencies.flatMap { findEmbeddableOptions(it.linkerOpts) }

    embedLlvmLinkOptions(llvm.llvmContext, llvm.module, optionsToEmbed)
}
