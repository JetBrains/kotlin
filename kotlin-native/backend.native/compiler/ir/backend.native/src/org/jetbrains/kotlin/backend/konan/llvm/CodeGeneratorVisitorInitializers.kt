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

internal fun CodeGeneratorVisitor.createInitBody(state: ScopeInitializersGenerationState): RuntimeInitializer {
    return generateRuntimeInitializer {
        using(FunctionScope(function, this)) {
            val bbInit = basicBlock("init", null)
            val bbLocalInit = basicBlock("local_init", null)
            val bbLocalAlloc = basicBlock("local_alloc", null)
            val bbDefault = basicBlock("default", null) {
                unreachable()
            }

            switch(function.param(0),
                    listOf(
                            llvm.int32(INIT_GLOBALS) to bbInit,
                            llvm.int32(INIT_THREAD_LOCAL_GLOBALS) to bbLocalInit,
                            llvm.int32(ALLOC_THREAD_LOCAL_GLOBALS) to bbLocalAlloc
                    ),
                    bbDefault)

            // Globals initializers may contain accesses to objects, so visit them first.
            appendingTo(bbInit) {
                state.globalEagerInitFunction?.let { call(it, listOf(), exceptionHandler = ExceptionHandler.Caller) }
                ret(null)
            }

            appendingTo(bbLocalInit) {
                state.threadLocalEagerInitFunction?.let { call(it, listOf(), exceptionHandler = ExceptionHandler.Caller) }
                ret(null)
            }

            appendingTo(bbLocalAlloc) {
                if (llvm.tlsCount > 0) {
                    val memory = function.param(1)
                    call(llvm.addTLSRecord, listOf(memory, llvm.tlsKey, llvm.int32(llvm.tlsCount)))
                }
                ret(null)
            }
        }
    }
}

internal fun CodeGeneratorVisitor.mergeRuntimeInitializers(runtimeInitializers: List<RuntimeInitializer>): RuntimeInitializer? {
    if (runtimeInitializers.size <= 1) return runtimeInitializers.singleOrNull()

    // It would be natural to generate a single runtime initializer function
    // and call all the initializers from it.
    // However, right now we can have quite many initializers (see e.g. KT-74774).
    // So, this natural solution can lead to generating huge LLVM functions triggering slow compilation.
    // Apply a cheap trick -- merge them by chunks recursively.

    val chunkInitializers = runtimeInitializers.chunked(100) { chunk ->
        generateRuntimeInitializer {
            chunk.forEach {
                this.call(it.llvmCallable, listOf(param(0), param(1)), exceptionHandler = ExceptionHandler.Caller)
            }
            ret(null)
        }
    }

    return mergeRuntimeInitializers(chunkInitializers)
}

internal fun CodeGeneratorVisitor.generateRuntimeInitializer(block: FunctionGenerationContext.() -> Unit): RuntimeInitializer {
    val initFunctionProto = kInitFuncType.toProto("", null, LLVMLinkage.LLVMPrivateLinkage)
    return RuntimeInitializer(generateFunction(codegen, initFunctionProto, code = block))
}

//-------------------------------------------------------------------------//
// Creates static struct InitNode $nodeName = {$initName, NULL};

internal fun CodeGeneratorVisitor.createInitNode(runtimeInitializer: RuntimeInitializer): LLVMValueRef {
    val initFunction = runtimeInitializer.llvmCallable
    val nextInitNode = llvm.kNull
    val argList = cValuesOf(initFunction.toConstPointer().llvm, nextInitNode)
    // Create static object of class InitNode.
    val initNode = LLVMConstNamedStruct(kNodeInitType, argList, 2)!!
    // Create global variable with init record data.
    return codegen.staticData.placeGlobal("init_node", constPointer(initNode), isExported = false).llvmGlobal
}

//-------------------------------------------------------------------------//

internal fun CodeGeneratorVisitor.createInitCtor(initNodePtr: LLVMValueRef): LlvmCallable {
    val ctorProto = ctorFunctionSignature.toProto("", null, LLVMLinkage.LLVMPrivateLinkage)
    val ctor = generateFunctionNoRuntime(codegen, ctorProto) {
        call(llvm.appendToInitalizersTail, listOf(initNodePtr))
        ret(null)
    }
    return ctor
}

