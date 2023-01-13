/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import kotlinx.cinterop.cValuesOf
import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.ContextUtils
import org.jetbrains.kotlin.backend.konan.llvm.ExceptionHandler
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionWithDefaultAttributes
import org.jetbrains.kotlin.backend.konan.llvm.generateFunction
import org.jetbrains.kotlin.backend.konan.lower.getObjectClassInstanceFunction
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.resolve.DescriptorUtils

/**
 * Second phase of C Export: build bitcode bridges from C wrappers to Kotlin functions.
 */
internal class CAdapterCodegen(
    private val codegen: CodeGenerator,
    override val generationState: NativeGenerationState,
) : ContextUtils {

    fun buildAllAdaptersRecursively(elements: CAdapterExportedElements) {
        val top = elements.scopes.single()
        assert(top.kind == ScopeKind.TOP)
        top.generateCAdapters(this::buildCAdapter)
    }

    private fun ExportedElementScope.generateCAdapters(builder: (ExportedElement) -> Unit) {
        this.elements.forEach { builder(it) }
        this.scopes.forEach { it.generateCAdapters(builder) }
    }

    private fun buildCAdapter(exportedElement: ExportedElement): Unit = with(exportedElement) {
        when {
            isFunction -> {
                val function = declaration as FunctionDescriptor
                val irFunction = irSymbol.owner as IrFunction
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                val llvmCallable = codegen.llvmFunction(irFunction)
                // If function is virtual, we need to resolve receiver properly.
                val bridge = generateFunction(codegen, llvmCallable.functionType, cname) {
                    val callee = if (!DescriptorUtils.isTopLevelDeclaration(function) &&
                        irFunction.isOverridable
                    ) {
                        val receiver = param(0)
                        lookupVirtualImpl(receiver, irFunction)
                    } else {
                        // KT-45468: Alias insertion may not be handled by LLVM properly, in case callee is in the cache.
                        // Hence, insert not an alias but a wrapper, hoping it will be optimized out later.
                        llvmCallable
                    }

                    val numParams = LLVMCountParams(llvmCallable.llvmValue)
                    val args = (0 until numParams).map { index -> param(index) }
                    callee.attributeProvider.addFunctionAttributes(this.function)
                    val result = call(callee, args, exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                    ret(result)
                }
                LLVMSetLinkage(bridge, LLVMLinkage.LLVMExternalLinkage)
            }
            isClass -> {
                val irClass = irSymbol.owner as IrClass
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                // Produce type getter.
                val getTypeFunction = addLlvmFunctionWithDefaultAttributes(
                    context,
                    llvm.module,
                    "${cname}_type",
                    kGetTypeFuncType
                )
                val builder = LLVMCreateBuilderInContext(llvm.llvmContext)!!
                val bb = LLVMAppendBasicBlockInContext(llvm.llvmContext, getTypeFunction, "")!!
                LLVMPositionBuilderAtEnd(builder, bb)
                LLVMBuildRet(builder, irClass.typeInfoPtr.llvm)
                LLVMDisposeBuilder(builder)
                // Produce instance getter if needed.
                if (isSingletonObject) {
                    generateFunction(codegen, kGetObjectFuncType, "${cname}_instance") {
                        val value = call(
                            codegen.llvmFunction(context.getObjectClassInstanceFunction(irClass)),
                            emptyList(),
                            Lifetime.GLOBAL,
                            ExceptionHandler.Caller,
                            false,
                            returnSlot
                        )
                        ret(value)
                    }
                }
            }
            isEnumEntry -> {
                // Produce entry getter.
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                generateFunction(codegen, kGetObjectFuncType, cname) {
                    val irEnumEntry = irSymbol.owner as IrEnumEntry
                    val value = getEnumEntry(irEnumEntry, ExceptionHandler.Caller)
                    ret(value)
                }
            }
        }
    }

    private val kGetTypeFuncType = LLVMFunctionType(codegen.kTypeInfoPtr, null, 0, 0)!!

    // Abstraction leak for slot :(.
    private val kGetObjectFuncType = LLVMFunctionType(codegen.kObjHeaderPtr, cValuesOf(codegen.kObjHeaderPtrPtr), 1, 0)!!
}