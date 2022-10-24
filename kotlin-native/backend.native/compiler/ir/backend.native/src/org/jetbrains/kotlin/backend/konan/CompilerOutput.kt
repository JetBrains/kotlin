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
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * Supposed to be true for a single LLVM module within final binary.
 */
val KonanConfig.isFinalBinary: Boolean get() = when (this.produce) {
    CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
    CompilerOutputKind.STATIC -> true
    CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE,
    CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
    CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
    else -> error("not supported: ${this.produce}")
}

val CompilerOutputKind.isNativeLibrary: Boolean
    get() = this == CompilerOutputKind.DYNAMIC || this == CompilerOutputKind.STATIC

val CompilerOutputKind.involvesBitcodeGeneration: Boolean
    get() = this != CompilerOutputKind.LIBRARY

internal val Context.producedLlvmModuleContainsStdlib: Boolean
    get() = this.llvmModuleSpecification.containsModule(this.stdlibModule)

internal val Context.shouldDefineFunctionClasses: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.internalPackageName, "KFunctionImpl.kt") != false

internal val Context.shouldDefineCachedBoxes: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.internalPackageName, "Boxing.kt") != false

internal val Context.shouldLinkRuntimeNativeLibraries: Boolean
    get() = producedLlvmModuleContainsStdlib &&
            config.libraryToCache?.strategy?.contains(KonanFqNames.packageName, "Runtime.kt") != false

val KonanConfig.involvesLinkStage: Boolean
    get() = when (this.produce) {
        CompilerOutputKind.PROGRAM, CompilerOutputKind.DYNAMIC,
        CompilerOutputKind.DYNAMIC_CACHE, CompilerOutputKind.STATIC_CACHE,
        CompilerOutputKind.STATIC -> true
        CompilerOutputKind.LIBRARY, CompilerOutputKind.BITCODE -> false
        CompilerOutputKind.FRAMEWORK -> !omitFrameworkBinary
        else -> error("not supported: ${this.produce}")
    }

val CompilerOutputKind.isCache: Boolean
    get() = this == CompilerOutputKind.STATIC_CACHE || this == CompilerOutputKind.DYNAMIC_CACHE

val KonanConfig.involvesCodegen: Boolean
    get() = produce != CompilerOutputKind.LIBRARY && !omitFrameworkBinary

internal fun llvmIrDumpCallback(state: ActionState, module: IrModuleFragment, context: Context) {
    module.let{}
    if (state.beforeOrAfter == BeforeOrAfter.AFTER && state.phase.name in context.configuration.getList(KonanConfigKeys.SAVE_LLVM_IR)) {
        val moduleName: String = memScoped {
            val sizeVar = alloc<size_tVar>()
            LLVMGetModuleIdentifier(context.generationState.llvm.module, sizeVar.ptr)!!.toKStringFromUtf8()
        }
        val output = context.generationState.tempFiles.create("$moduleName.${state.phase.name}", ".ll")
        if (LLVMPrintModuleToFile(context.generationState.llvm.module, output.absolutePath, null) != 0) {
            error("Can't dump LLVM IR to ${output.absolutePath}")
        }
    }
}

internal fun produceCStubs(context: Context) {
    context.generationState.cStubsManager.compile(
            context.config.clang,
            context.messageCollector,
            context.inVerbosePhase
    ).forEach {
        parseAndLinkBitcodeFile(context, context.generationState.llvm.module, it.absolutePath)
    }
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
private fun collectLlvmModules(context: Context, generatedBitcodeFiles: List<String>): LlvmModules {
    val config = context.config

    val (bitcodePartOfStdlib, bitcodeLibraries) = context.generationState.llvm.bitcodeToLink
            .partition { it.isStdlib && context.producedLlvmModuleContainsStdlib }
            .toList()
            .map { libraries ->
                libraries.flatMap { it.bitcodePaths }.filter { it.isBitcode }
            }

    val nativeLibraries = config.nativeLibraries + config.launcherNativeLibraries
            .takeIf { config.produce == CompilerOutputKind.PROGRAM }.orEmpty()
    val additionalBitcodeFilesToLink = context.generationState.llvm.additionalProducedBitcodeFiles
    val exceptionsSupportNativeLibrary = listOf(config.exceptionsSupportNativeLibrary)
            .takeIf { config.produce == CompilerOutputKind.DYNAMIC_CACHE }.orEmpty()
    val additionalBitcodeFiles = nativeLibraries +
            generatedBitcodeFiles +
            additionalBitcodeFilesToLink +
            bitcodeLibraries +
            exceptionsSupportNativeLibrary

    val runtimeNativeLibraries = context.config.runtimeNativeLibraries


    fun parseBitcodeFiles(files: List<String>): List<LLVMModuleRef> = files.map { bitcodeFile ->
        val parsedModule = parseBitcodeFile(context.generationState.llvmContext, bitcodeFile)
        if (!context.shouldUseDebugInfoFromNativeLibs()) {
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
            runtimeModules.ifNotEmpty { this + context.generateRuntimeConstantsModule() } ?: emptyList(),
            additionalModules + listOfNotNull(patchObjCRuntimeModule(context))
    )
}

private fun linkAllDependencies(context: Context, generatedBitcodeFiles: List<String>) {
    val (runtimeModules, additionalModules) = collectLlvmModules(context, generatedBitcodeFiles)
    // TODO: Possibly slow, maybe to a separate phase?
    val optimizedRuntimeModules = RuntimeLinkageStrategy.pick(context, runtimeModules).run()

    (optimizedRuntimeModules + additionalModules).forEach {
        val failed = llvmLinkModules2(context, context.generationState.llvm.module, it)
        if (failed != 0) {
            error("Failed to link ${it.getName()}")
        }
    }
}

private fun insertAliasToEntryPoint(context: Context) {
    val nomain = context.config.configuration.get(KonanConfigKeys.NOMAIN) ?: false
    if (context.config.produce != CompilerOutputKind.PROGRAM || nomain)
        return
    val module = context.generationState.llvm.module
    val entryPointName = context.config.entryPointName
    val entryPoint = LLVMGetNamedFunction(module, entryPointName)
            ?: error("Module doesn't contain `$entryPointName`")
    LLVMAddAlias(module, LLVMTypeOf(entryPoint)!!, entryPoint, "main")
}

internal fun linkBitcodeDependencies(context: Context) {
    val config = context.config.configuration
    val tempFiles = context.generationState.tempFiles
    val produce = config.get(KonanConfigKeys.PRODUCE)

    val generatedBitcodeFiles =
            if (produce == CompilerOutputKind.DYNAMIC || produce == CompilerOutputKind.STATIC) {
                produceCAdapterBitcode(
                        context.config.clang,
                        tempFiles.cAdapterCppName,
                        tempFiles.cAdapterBitcodeName)
                listOf(tempFiles.cAdapterBitcodeName)
            } else emptyList()
    if (produce == CompilerOutputKind.FRAMEWORK && context.config.produceStaticFramework) {
        embedAppleLinkerOptionsToBitcode(context.generationState.llvm, context.config)
    }
    linkAllDependencies(context, generatedBitcodeFiles)

}

internal fun produceOutput(context: Context) {

    val config = context.config.configuration
    val tempFiles = context.generationState.tempFiles
    val produce = context.config.produce
    if (produce == CompilerOutputKind.FRAMEWORK) {
        context.objCExport.produceFrameworkInterface()
        if (context.config.omitFrameworkBinary) {
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
            insertAliasToEntryPoint(context)
            LLVMWriteBitcodeToFile(context.generationState.llvm.module, output)
        }
        CompilerOutputKind.LIBRARY -> {
            val nopack = config.getBoolean(KonanConfigKeys.NOPACK)
            val output = context.generationState.outputFiles.klibOutputFileName(!nopack)
            val libraryName = context.config.moduleId
            val shortLibraryName = context.config.shortModuleName
            val neededLibraries = context.librariesWithDependencies
            val abiVersion = KotlinAbiVersion.CURRENT
            val compilerVersion = CompilerVersion.CURRENT.toString()
            val libraryVersion = config.get(KonanConfigKeys.LIBRARY_VERSION)
            val metadataVersion = KlibMetadataVersion.INSTANCE.toString()
            val irVersion = KlibIrVersion.INSTANCE.toString()
            val versions = KotlinLibraryVersioning(
                abiVersion = abiVersion,
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                metadataVersion = metadataVersion,
                irVersion = irVersion
            )
            val target = context.config.target
            val manifestProperties = context.config.manifestProperties

            if (!nopack) {
                val suffix = context.config.produce.suffix(target)
                if (!output.endsWith(suffix)) {
                    error("please specify correct output: packed: ${!nopack}, $output$suffix")
                }
            }

            val library = buildLibrary(
                    context.config.nativeLibraries,
                    context.config.includeBinaries,
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

            context.bitcodeFileName = library.mainBitcodeFileName
        }
        CompilerOutputKind.BITCODE -> {
            val output = context.generationState.outputFile
            context.bitcodeFileName = output
            LLVMWriteBitcodeToFile(context.generationState.llvm.module, output)
        }
        else -> error("not supported: $produce")
    }
}

internal fun parseAndLinkBitcodeFile(context: Context, llvmModule: LLVMModuleRef, path: String) {
    val parsedModule = parseBitcodeFile(context.generationState.llvmContext, path)
    if (!context.shouldUseDebugInfoFromNativeLibs()) {
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

    embedLlvmLinkOptions(llvm.llvmContext, llvm.module, optionsToEmbed)
}
