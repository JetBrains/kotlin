/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf8
import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.ActionState
import org.jetbrains.kotlin.backend.common.phaser.BeforeOrAfter
import org.jetbrains.kotlin.backend.common.serialization.KlibIrVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.patchObjCRuntimeModule
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.phases.KlibProducingContext
import org.jetbrains.kotlin.backend.konan.phases.LlvmCodegenContext
import org.jetbrains.kotlin.backend.konan.phases.LlvmModuleSpecificationComponent
import org.jetbrains.kotlin.backend.konan.phases.stdlibModule
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * Supposed to be true for a single LLVM module within final binary.
 */
val KonanConfig.isFinalBinary: Boolean get() = when (this.produce) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC -> true
    CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE, CompilerOutputKind.PRELIMINARY_CACHE,
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
    CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
}

val CompilerOutputKind.involvesBitcodeGeneration: Boolean
    get() = this != CompilerOutputKind.LIBRARY

internal val LlvmModuleSpecificationComponent.producedLlvmModuleContainsStdlib: Boolean
    get() = this.llvmModuleSpecification.containsModule(this.stdlibModule)

internal val LlvmModuleSpecificationComponent.shouldDefineFunctionClasses: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.internalPackageName, "KFunctionImpl.kt") != false

internal val LlvmModuleSpecificationComponent.shouldDefineCachedBoxes: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.internalPackageName, "Boxing.kt") != false

internal val LlvmModuleSpecificationComponent.shouldLinkRuntimeNativeLibraries: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.packageName, "Runtime.kt") != false

val KonanConfig.involvesLinkStage: Boolean
    get() = when (this.produce) {
        CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE,
        CompilerOutputKind.STATIC -> true
        CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE, CompilerOutputKind.PRELIMINARY_CACHE -> false
        CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
    }

val CompilerOutputKind.isCache: Boolean
    get() = this == CompilerOutputKind.STATIC_CACHE || this == CompilerOutputKind.DYNAMIC_CACHE
            || this == CompilerOutputKind.PRELIMINARY_CACHE

internal fun llvmIrDumpCallback(state: ActionState, module: IrModuleFragment, context: Context) {
    module.let{}
    if (state.beforeOrAfter == BeforeOrAfter.AFTER && state.phase.name in context.configuration.getList(KonanConfigKeys.SAVE_LLVM_IR)) {
        val moduleName: String = memScoped {
            val sizeVar = alloc<size_tVar>()
            LLVMGetModuleIdentifier(context.llvmModule, sizeVar.ptr)!!.toKStringFromUtf8()
        }
        val output = context.config.tempFiles.create("$moduleName.${state.phase.name}", ".ll")
        if (LLVMPrintModuleToFile(context.llvmModule, output.absolutePath, null) != 0) {
            error("Can't dump LLVM IR to ${output.absolutePath}")
        }
    }
}

internal fun produceCStubs(context: LlvmCodegenContext): List<String> {
    return context.cStubsManager.compile(
            context.config.clang,
            context.messageCollector,
            context.inVerbosePhase
    )
}

private val BaseKotlinLibrary.isStdlib: Boolean
    get() = uniqueName == KONAN_STDLIB_NAME


private data class LlvmModules(
        val runtimeModules: List<LLVMModuleRef>,
        val additionalModules: List<LLVMModuleRef>
)

/**
 * Deserialize, generate, patch all bitcode dependencies and classify them into two sets:
 * - Runtime modules. These may be used as an input for a separate LTO (e.g. for debug builds).
 * - Everything else.
 */
private fun collectLlvmModules(
        context: LlvmCodegenContext,
        config: KonanConfig,
        generatedBitcodeFiles: List<String>
): LlvmModules {

    val (bitcodePartOfStdlib, bitcodeLibraries) = context.llvm.bitcodeToLink
            .partition { it.isStdlib && context.producedLlvmModuleContainsStdlib }
            .toList()
            .map { libraries ->
                libraries.flatMap { it.bitcodePaths }.filter { it.isBitcode }
            }

    val nativeLibraries = config.nativeLibraries + config.launcherNativeLibraries
            .takeIf { config.produce == CompilerOutputKind.PROGRAM }.orEmpty()
    val additionalBitcodeFilesToLink = context.llvm.additionalProducedBitcodeFiles
    val exceptionsSupportNativeLibrary = listOf(config.exceptionsSupportNativeLibrary)
            .takeIf { config.produce == CompilerOutputKind.DYNAMIC_CACHE }.orEmpty()
    val additionalBitcodeFiles = nativeLibraries +
            generatedBitcodeFiles +
            additionalBitcodeFilesToLink +
            bitcodeLibraries +
            exceptionsSupportNativeLibrary

    val runtimeNativeLibraries = context.config.runtimeNativeLibraries


    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(bitcodeFile)
        if (!context.config.checks.shouldUseDebugInfoFromNativeLibs()) {
            LLVMStripModuleDebugInfo(parsedModule)
        }
        parsedModule
    }

    val runtimeModules = parseBitcodeFiles(
            (runtimeNativeLibraries + bitcodePartOfStdlib)
                    .takeIf { context.shouldLinkRuntimeNativeLibraries }.orEmpty()
    )
    val additionalModules = parseBitcodeFiles(additionalBitcodeFiles)
    return LlvmModules(
            runtimeModules.ifNotEmpty { this + generateRuntimeConstantsModule(context.llvm.runtime.dataLayout, config) } ?: emptyList(),
            additionalModules + listOfNotNull(patchObjCRuntimeModule(config, context))
    )
}

private fun linkAllDependencies(llvmModule: LLVMModuleRef, context: LlvmCodegenContext, generatedBitcodeFiles: List<String>) {
    val (runtimeModules, additionalModules) = collectLlvmModules(context, context.config, generatedBitcodeFiles)
    // TODO: Possibly slow, maybe to a separate phase?
    val optimizedRuntimeModules = RuntimeLinkageStrategy.pick(context.config, context, runtimeModules).run()

    (optimizedRuntimeModules + additionalModules).forEach {
        val failed = llvmLinkModules2(context, llvmModule, it)
        if (failed != 0) {
            error("Failed to link ${it.getName()}")
        }
    }
}

internal fun insertAliasToEntryPoint(module: LLVMModuleRef, config: KonanConfig) {
    val nomain = config.configuration.get(KonanConfigKeys.NOMAIN) ?: false
    if (config.produce != CompilerOutputKind.PROGRAM || nomain)
        return
    val entryPointName = config.entryPointName
    val entryPoint = LLVMGetNamedFunction(module, entryPointName)
            ?: error("Module doesn't contain `$entryPointName`")
    LLVMAddAlias(module, LLVMTypeOf(entryPoint)!!, entryPoint, "main")
}

internal fun linkBitcodeDependencies(context: LlvmCodegenContext, config: KonanConfig) {
    val tempFiles = config.tempFiles
    val produce = config.configuration.get(KonanConfigKeys.PRODUCE)

    val generatedBitcodeFiles =
            if (produce == CompilerOutputKind.DYNAMIC || produce == CompilerOutputKind.STATIC) {
                produceCAdapterBitcode(
                        config.clang,
                        tempFiles.cAdapterCppName,
                        tempFiles.cAdapterBitcodeName)
                listOf(tempFiles.cAdapterBitcodeName)
            } else emptyList()
    if (produce == CompilerOutputKind.FRAMEWORK && config.produceStaticFramework) {
        embedAppleLinkerOptionsToBitcode(context.llvm, config)
    }
    val cstubs = produceCStubs(context)
    linkAllDependencies(context.llvmModule!!, context, generatedBitcodeFiles + cstubs)

}

internal fun produceKlib(context: KlibProducingContext, config: KonanConfig) {
    val configuration = config.configuration
    val nopack = configuration.getBoolean(KonanConfigKeys.NOPACK)
    val output = config.outputFiles.klibOutputFileName(!nopack)
    val libraryName = config.moduleId
    val shortLibraryName = config.shortModuleName
    val neededLibraries = context.librariesWithDependencies
    val abiVersion = KotlinAbiVersion.CURRENT
    val compilerVersion = CompilerVersion.CURRENT.toString()
    val libraryVersion = configuration.get(KonanConfigKeys.LIBRARY_VERSION)
    val metadataVersion = KlibMetadataVersion.INSTANCE.toString()
    val irVersion = KlibIrVersion.INSTANCE.toString()
    val versions = KotlinLibraryVersioning(
            abiVersion = abiVersion,
            libraryVersion = libraryVersion,
            compilerVersion = compilerVersion,
            metadataVersion = metadataVersion,
            irVersion = irVersion
    )
    val target = config.target
    val manifestProperties = config.manifestProperties

    if (!nopack) {
        val suffix = config.outputFiles.produce.suffix(target)
        if (!output.endsWith(suffix)) {
            error("please specify correct output: packed: ${!nopack}, $output$suffix")
        }
    }

    buildLibrary(
            config.nativeLibraries,
            config.includeBinaries,
            neededLibraries,
            context.serializedMetadata!!,
            context.serializedIr,
            versions,
            target,
            output,
            libraryName,
            nopack,
            shortLibraryName,
            manifestProperties,
            context.dataFlowGraph)
}

internal fun produceFrameworkInterface(objcExport: ObjCExport?) {
    objcExport?.produceFrameworkInterface()
}

internal fun produceBitcode(config: KonanConfig, context: Context, llvmModule: LLVMModuleRef) {
    val output = config.outputFile
    context.bitcodeFileName = output
    LLVMWriteBitcodeToFile(llvmModule, output)
}

internal fun produceOutput(context: Context, config: KonanConfig) {

    val tempFiles = config.tempFiles
    val produce = config.produce
    if (produce == CompilerOutputKind.FRAMEWORK) {
        if (config.omitFrameworkBinary) {
            // Compiler does not compile anything in this mode, so return early.
            return
        }
    }
    when (produce) {
        CompilerOutputKind.STATIC,
        CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.FRAMEWORK,
        CompilerOutputKind.DYNAMIC_CACHE,
        CompilerOutputKind.STATIC_CACHE,
        CompilerOutputKind.PROGRAM -> {
            val output = tempFiles.nativeBinaryFileName
            context.bitcodeFileName = output
            // Insert `_main` after pipeline so we won't worry about optimizations
            // corrupting entry point.
            insertAliasToEntryPoint(context.llvmModule!!, context.config)
            LLVMWriteBitcodeToFile(context.llvmModule!!, output)
        }
        CompilerOutputKind.LIBRARY -> {}
        CompilerOutputKind.BITCODE -> {}
        CompilerOutputKind.PRELIMINARY_CACHE -> {}
    }
}

internal fun parseAndLinkBitcodeFile(context: LlvmCodegenContext, llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(path)
    if (!context.config.checks.shouldUseDebugInfoFromNativeLibs()) {
        LLVMStripModuleDebugInfo(parsedModule)
    }
    val failed = llvmLinkModules2(context, llvmModule, parsedModule)
    if (failed != 0) {
        throw Error("failed to link $path")
    }
}

private fun embedAppleLinkerOptionsToBitcode(llvm: Llvm, config: KonanConfig) {
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
            llvm.allNativeDependencies.flatMap { findEmbeddableOptions(it.linkerOpts) }

    embedLlvmLinkOptions(llvm.llvmModule, optionsToEmbed)
}
