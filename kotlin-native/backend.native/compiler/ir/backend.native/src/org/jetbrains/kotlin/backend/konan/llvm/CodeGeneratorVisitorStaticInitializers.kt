/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import hair.compilation.FunctionCompilation
import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.hair.HairToBitcode
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.processBindClassToObjCNameAnnotations
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ReifiedFunctionLowering.Companion.isReifiedInline
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.SourceInfoType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal fun CodeGeneratorVisitor.appendLlvmUsed(name: String, args: List<LLVMValueRef>) {
    if (args.isEmpty()) return

    val llvmUsedGlobal = codegen.staticData.placeGlobalArray(name, llvm.pointerType, args.map { constPointer(it) })

    LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
    LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
}

// Globals set this way cannot be const, but are overridable when producing final executable.
internal fun CodeGeneratorVisitor.overrideRuntimeGlobal(name: String, value: ConstValue) =
        codegen.replaceExternalWeakOrCommonGlobalFromNativeRuntime(name, value)

internal fun CodeGeneratorVisitor.overrideRuntimeGlobals() {
    if (!context.config.isFinalBinary)
        return

    overrideRuntimeGlobal("Kotlin_gcMutatorsCooperate", llvm.constInt32(if (context.config.gcMutatorsCooperate) 1 else 0))
    overrideRuntimeGlobal("Kotlin_auxGCThreads", llvm.constInt32(context.config.auxGCThreads.toInt()))
    overrideRuntimeGlobal("Kotlin_concurrentMarkMaxIterations", llvm.constInt32(context.config.concurrentMarkMaxIterations.toInt()))
    overrideRuntimeGlobal("Kotlin_suspendFunctionsFromAnyThreadFromObjC", llvm.constInt32(if (context.config.suspendFunctionsFromAnyThreadFromObjC) 1 else 0))
    val getSourceInfoFunctionName = when (context.config.sourceInfoType) {
        SourceInfoType.NOOP -> null
        SourceInfoType.LIBBACKTRACE -> "Kotlin_getSourceInfo_libbacktrace"
        SourceInfoType.CORESYMBOLICATION -> "Kotlin_getSourceInfo_core_symbolication"
    }
    if (getSourceInfoFunctionName != null) {
        val getSourceInfoFunction = LLVMGetNamedFunction(llvm.module, getSourceInfoFunctionName)
                ?: LLVMAddFunction(llvm.module, getSourceInfoFunctionName,
                        functionType(llvm.int32Type, false, llvm.pointerType, llvm.pointerType, llvm.int32Type))
        overrideRuntimeGlobal("Kotlin_getSourceInfo_Function", constValue(getSourceInfoFunction!!))
    }
    overrideRuntimeGlobal("Kotlin_CoreSymbolication_useOnlyKotlinImage",
            llvm.constInt32(if (context.config.coreSymbolicationUseOnlyKotlinImage) 1 else 0))
    if (context.config.target.family == Family.ANDROID && context.config.produce == CompilerOutputKind.PROGRAM) {
        val configuration = context.config.configuration
        val programType = configuration.get(BinaryOptions.androidProgramType) ?: AndroidProgramType.Default
        overrideRuntimeGlobal("Kotlin_printToAndroidLogcat", llvm.constInt32(if (programType.consolePrintsToLogcat) 1 else 0))
    }
    overrideRuntimeGlobal("Kotlin_appStateTracking", llvm.constInt32(context.config.appStateTracking.value))
    overrideRuntimeGlobal("Kotlin_objcDisposeOnMain", llvm.constInt32(if (context.config.objcDisposeOnMain) 1 else 0))
    overrideRuntimeGlobal("Kotlin_objcDisposeWithRunLoop", llvm.constInt32(if (context.config.objcDisposeWithRunLoop) 1 else 0))
    overrideRuntimeGlobal("Kotlin_enableSafepointSignposts", llvm.constInt32(if (context.config.enableSafepointSignposts) 1 else 0))
    overrideRuntimeGlobal("Kotlin_globalDataLazyInit", llvm.constInt32(if (context.config.globalDataLazyInit) 1 else 0))
    overrideRuntimeGlobal("Kotlin_swiftExport", llvm.constInt32(if (context.config.swiftExport) 1 else 0))
    overrideRuntimeGlobal("Kotlin_latin1Strings", llvm.constInt32(if (context.config.latin1Strings) 1 else 0))
    overrideRuntimeGlobal("Kotlin_mmapTag", llvm.constUInt8(context.config.mmapTag))
    val minidumpLocation = context.config.minidumpLocation?.let {
        llvm.staticData.cStringLiteral(it)
    } ?: llvm.nullPointer
    overrideRuntimeGlobal("Kotlin_minidumpLocation", minidumpLocation)
    overrideRuntimeGlobal("Kotlin_minidumpOnSIGTERM", llvm.constInt32(if (context.config.minidumpOnSIGTERM) 1 else 0))
}

//-------------------------------------------------------------------------//
// Create object { i32, void ()*, i8* } { i32 1, void ()* @ctorFunction, i8* null }

internal fun CodeGeneratorVisitor.createGlobalCtor(ctorFunction: LlvmCallable): ConstPointer {
    val priority = if (context.config.target.family == Family.MINGW) {
        // Workaround MinGW bug. Using this value makes the compiler generate
        // '.ctors' section instead of '.ctors.XXXXX', which can't be recognized by ld
        // when string table is too long.
        // More details: https://youtrack.jetbrains.com/issue/KT-39548
        llvm.int32(65535)
        // Note: this difference in priorities doesn't actually make initializers
        // platform-dependent, because handling priorities for initializers
        // from different object files is platform-dependent anyway.
    } else {
        llvm.kImmInt32One
    }
    val data = llvm.kNull
    val argList = cValuesOf(priority, ctorFunction.toConstPointer().llvm, data)
    val ctorItem = LLVMConstNamedStruct(kCtorType, argList, 3)!!
    return constPointer(ctorItem)
}

//-------------------------------------------------------------------------//
internal fun CodeGeneratorVisitor.appendStaticInitializers() {
    // Note: the list of libraries is topologically sorted (in order for initializers to be called correctly).
    val dependencies = (generationState.dependenciesTracker.allBitcodeDependencies + listOf(null)/* Null for "current" non-library module */)

    val libraryToInitializers = dependencies.associate { it?.library to mutableListOf<RuntimeInitializer>() }

    llvm.irStaticInitializers.forEach {
        val library = it.konanLibrary
        val initializers = libraryToInitializers[library]
                ?: error("initializer for not included library ${library?.libraryFile}")

        initializers.add(it.runtimeInitializer)
    }

    fun fileCtorName(libraryName: String, fileName: String) = "$libraryName:$fileName".moduleConstructorName

    fun ctorProto(ctorName: String): LlvmFunctionProto {
        return ctorFunctionSignature.toProto(ctorName, null, LLVMLinkage.LLVMExternalLinkage)
    }

    val ctorFunctions = dependencies.flatMap { dependency ->
        val library = dependency?.library
        val initializer = mergeRuntimeInitializers(libraryToInitializers.getValue(library))
                ?.let { createInitCtor(createInitNode(it)) }

        val ctorName = when {
            // TODO: Try to not use moduleId.
            library == null -> (if (context.config.produce.isCache) generationState.outputFiles.cacheFileName else context.config.moduleId).moduleConstructorName
            library == context.config.libraryToCache?.klib
                    && context.config.producePerFileCache ->
                fileCtorName(library.uniqueName, generationState.outputFiles.perFileCacheFileName)
            else -> library.moduleConstructorName
        }

        if (library == null || generationState.llvmModuleSpecification.containsLibrary(library)) {
            val otherInitializers = llvm.otherStaticInitializers.takeIf { library == null }.orEmpty()

            listOf(
                appendStaticInitializers(ctorProto(ctorName), listOfNotNull(initializer) + otherInitializers)
            )
        } else {
            // A cached library.
            check(initializer == null) {
                "found initializer from ${library.libraryFile}, which is not included into compilation"
            }

            val cache = context.config.cachedLibraries.getLibraryCache(library)
                    ?: error("Library ${library.libraryFile} is expected to be cached")

            when (cache) {
                is CachedLibraries.Cache.Monolithic -> listOf(ctorProto(ctorName))
                is CachedLibraries.Cache.PerFile -> {
                    val files = when (dependency.kind) {
                        is DependenciesTracker.DependencyKind.WholeModule -> {
                            val fileIdProvider: FileIdProvider = context.moduleDeserializerProvider.getDeserializerOrNull(library)
                                    ?.let { FileIdProvider(it) }
                                    ?: error("Can't find deserializer for ${library.libraryFile}")
                            fileIdProvider.sortedFileIds
                        }
                        is DependenciesTracker.DependencyKind.CertainFiles ->
                            dependency.kind.files.map { it.name }
                    }
                    files.map { ctorProto(fileCtorName(library.uniqueName, it)) }
                }
            }.map {
                codegen.addFunction(it)
            }
        }
    }

    appendGlobalCtors(ctorFunctions)
}

internal fun CodeGeneratorVisitor.appendStaticInitializers(ctorCallableProto: LlvmFunctionProto, initializers: List<LlvmCallable>) : LlvmCallable {
    return generateFunctionNoRuntime(codegen, ctorCallableProto) {
        val initGuardName = function.name.orEmpty() + "_guard"
        val initGuard = LLVMAddGlobal(llvm.module, llvm.int32Type, initGuardName)
        LLVMSetInitializer(initGuard, llvm.kImmInt32Zero)
        LLVMSetLinkage(initGuard, LLVMLinkage.LLVMPrivateLinkage)
        val bbInited = basicBlock("inited", null)
        val bbNeedInit = basicBlock("need_init", null)


        val value = LLVMBuildLoad2(builder, llvm.int32Type, initGuard, "")!!
        condBr(icmpEq(value, llvm.kImmInt32Zero), bbNeedInit, bbInited)

        appendingTo(bbInited) {
            ret(null)
        }

        appendingTo(bbNeedInit) {
            LLVMBuildStore(builder, llvm.kImmInt32One, initGuard)

            // TODO: shall we put that into the try block?
            initializers.forEach {
                call(it, emptyList(), Lifetime.IRRELEVANT,
                        exceptionHandler = ExceptionHandler.Caller, verbatim = true)
            }
            ret(null)
        }
    }
}

internal fun CodeGeneratorVisitor.appendGlobalCtors(ctorFunctions: List<LlvmCallable>) {
    if (context.config.isFinalBinary) {
        // Generate function calling all [ctorFunctions].
        val ctorProto = ctorFunctionSignature.toProto(
                name = "_Konan_constructors",
                origin = null,
                linkage = if (context.config.produce == CompilerOutputKind.PROGRAM) LLVMLinkage.LLVMExternalLinkage else LLVMLinkage.LLVMPrivateLinkage
        )
        val globalCtorCallable = generateFunctionNoRuntime(codegen, ctorProto) {
            ctorFunctions.forEach {
                call(it, emptyList(), Lifetime.IRRELEVANT,
                        exceptionHandler = ExceptionHandler.Caller, verbatim = true)
            }
            ret(null)
        }

        // Append initializers of global variables in "llvm.global_ctors" array.
        val globalCtors = codegen.staticData.placeGlobalArray("llvm.global_ctors", kCtorType,
                listOf(createGlobalCtor(globalCtorCallable)))
        LLVMSetLinkage(globalCtors.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
    }
}
