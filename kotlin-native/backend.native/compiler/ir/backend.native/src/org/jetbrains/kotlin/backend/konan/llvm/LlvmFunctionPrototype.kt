/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isThrowable

/**
 * Add attributes to LLVM function declaration and its invocation.
 */
interface LlvmFunctionAttributeProvider {
    fun addCallSiteAttributes(callSite: LLVMValueRef)

    fun addFunctionAttributes(function: LLVMValueRef)

    companion object {
        fun makeEmpty(): LlvmFunctionAttributeProvider =
                DummyLlvmFunctionAttributeProvider

        fun copyFromExternal(externalFunction: LLVMValueRef): LlvmFunctionAttributeProvider =
                LlvmFunctionAttributesCopier(externalFunction)
    }
}

private object DummyLlvmFunctionAttributeProvider : LlvmFunctionAttributeProvider {
    override fun addCallSiteAttributes(callSite: LLVMValueRef) {}

    override fun addFunctionAttributes(function: LLVMValueRef) {}
}

/**
 * Copies attributes from the given [externalFunction].
 * This is useful when the [externalFunction] is declared in the another LLVM module,
 * and we want to create an external declaration for it.
 */
private class LlvmFunctionAttributesCopier(private val externalFunction: LLVMValueRef) : LlvmFunctionAttributeProvider {

    private val paramsCount: Int by lazy { LLVMCountParams(externalFunction) }

    private val attributesForCallSite: List<List<LLVMAttributeRef>> by lazy {
        attributesForFunctionDeclaration.map {
            // We don't need attributes like correctly-rounded-divide-sqrt-fp-math or less-precise-fpmad at callsites.
            // So let's take only enum and integer attributes, should be enough to generate correct calls.
            it.filter {
                // This function is actually more like "is enum OR int attribute".
                LLVMIsEnumAttribute(it) != 0
            }
        }
    }

    private val attributesForFunctionDeclaration: List<List<LLVMAttributeRef>> by lazy {
        memScoped {
            val result = mutableListOf<List<LLVMAttributeRef>>()
            for (index in LLVMAttributeFunctionIndex..paramsCount) {
                val count = LLVMGetAttributeCountAtIndex(externalFunction, index)
                val attributesBuffer = allocArray<LLVMAttributeRefVar>(count)
                LLVMGetAttributesAtIndex(externalFunction, index, attributesBuffer)
                result += (0 until count).map { attributesBuffer[it]!! }
            }
            result
        }
    }

    override fun addCallSiteAttributes(callSite: LLVMValueRef) {
        attributesForCallSite.withIndex().forEach { (listIndex, attributeList) ->
            attributeList.forEach { attributeRef ->
                LLVMAddCallSiteAttribute(callSite, LLVMAttributeFunctionIndex + listIndex, attributeRef)
            }
        }
    }

    override fun addFunctionAttributes(function: LLVMValueRef) {
        attributesForFunctionDeclaration.withIndex().forEach { (listIndex, attributeList) ->
            attributeList.forEach { attributeRef ->
                LLVMAddAttributeAtIndex(function, LLVMAttributeFunctionIndex + listIndex, attributeRef)
            }
        }
    }
}

private fun addCallSiteAttributesAtIndex(context: LLVMContextRef, callSite: LLVMValueRef, index: Int, attributes: List<LlvmAttribute>) {
    attributes.forEach { attribute ->
        val llvmAttributeRef = createLlvmEnumAttribute(context, attribute.asAttributeKindId())
        LLVMAddCallSiteAttribute(callSite, index, llvmAttributeRef)
    }
}

private fun addDeclarationAttributesAtIndex(context: LLVMContextRef, function: LLVMValueRef, index: Int, attributes: List<LlvmAttribute>) {
    attributes.forEach { attribute ->
        val llvmAttributeRef = createLlvmEnumAttribute(context, attribute.asAttributeKindId())
        LLVMAddAttributeAtIndex(function, index, llvmAttributeRef)
    }
}

/**
 * LLVM function's signature, enriched with attributes.
 */
internal open class LlvmFunctionSignature(
        val returnType: LlvmRetType,
        val parameterTypes: List<LlvmParamType> = emptyList(),
        val isVararg: Boolean = false,
        val functionAttributes: List<LlvmFunctionAttribute> = emptyList(),
) : LlvmFunctionAttributeProvider {

    constructor(irFunction: IrFunction, contextUtils: ContextUtils) : this(
            returnType = contextUtils.getLlvmFunctionReturnType(irFunction),
            parameterTypes = contextUtils.getLlvmFunctionParameterTypes(irFunction),
            functionAttributes = inferFunctionAttributes(contextUtils, irFunction),
            isVararg = false,
    )

    val llvmFunctionType by lazy {
        functionType(returnType.llvmType, isVararg, parameterTypes.map { it.llvmType })
    }

    override fun addCallSiteAttributes(callSite: LLVMValueRef) {
        val caller = LLVMGetBasicBlockParent(LLVMGetInstructionParent(callSite))
        val llvmContext = LLVMGetModuleContext(LLVMGetGlobalParent(caller))!!
        addCallSiteAttributesAtIndex(llvmContext, callSite, LLVMAttributeFunctionIndex, functionAttributes)
        addCallSiteAttributesAtIndex(llvmContext, callSite, LLVMAttributeReturnIndex, returnType.attributes)
        repeat(parameterTypes.count()) {
            addCallSiteAttributesAtIndex(llvmContext, callSite, it + 1, parameterTypes[it].attributes)
        }
    }

    override fun addFunctionAttributes(function: LLVMValueRef) {
        val llvmContext = LLVMGetModuleContext(LLVMGetGlobalParent(function))!!
        addDeclarationAttributesAtIndex(llvmContext, function, LLVMAttributeFunctionIndex, functionAttributes)
        addDeclarationAttributesAtIndex(llvmContext, function, LLVMAttributeReturnIndex, returnType.attributes)
        repeat(parameterTypes.count()) {
            addDeclarationAttributesAtIndex(llvmContext, function, it + 1, parameterTypes[it].attributes)
        }
    }
}

sealed class FunctionOrigin {
    object FromNativeRuntime : FunctionOrigin()

    class OwnedBy(val declaration: IrDeclaration) : FunctionOrigin()
}


/**
 * Prototype of a LLVM function that is not tied to a specific LLVM module.
 */
internal class LlvmFunctionProto(
      val name: String,
      val signature: LlvmFunctionSignature,
      val origin: FunctionOrigin?,
      val linkage: LLVMLinkage,
      val independent: Boolean = false,
) {
    constructor(irFunction: IrFunction, symbolName: String, contextUtils: ContextUtils, linkage: LLVMLinkage) : this(
            name = symbolName,
            signature = LlvmFunctionSignature(irFunction, contextUtils),
            origin = FunctionOrigin.OwnedBy(irFunction),
            linkage = linkage,
            independent = irFunction.hasAnnotation(RuntimeNames.independent)
    )

    fun createLlvmFunction(context: Context, llvmModule: LLVMModuleRef): LlvmCallable {
        val function = LLVMAddFunction(llvmModule, name, signature.llvmFunctionType)!!
        addDefaultLlvmFunctionAttributes(context, function)
        addTargetCpuAndFeaturesAttributes(context, function)
        signature.addFunctionAttributes(function)
        LLVMSetLinkage(function, linkage)
        return LlvmCallable(function, signature)
    }
}

internal fun LlvmFunctionSignature.toProto(name: String, origin: FunctionOrigin?, linkage: LLVMLinkage, independent: Boolean = false) =
        LlvmFunctionProto(name, this, origin, linkage, independent)



private fun mustNotInline(context: Context, irFunction: IrFunction): Boolean {
    if (context.shouldContainLocationDebugInfo()) {
        if (irFunction is IrConstructor && irFunction.isPrimary && irFunction.returnType.isThrowable()) {
            // To simplify skipping this constructor when scanning call stack in Kotlin_getCurrentStackTrace.
            return true
        }
    }
    if (irFunction.symbol == context.ir.symbols.entryPoint) {
        return true
    }

    return false
}

private fun inferFunctionAttributes(contextUtils: ContextUtils, irFunction: IrFunction): List<LlvmFunctionAttribute> =
        mutableListOf<LlvmFunctionAttribute>().apply {
            if (irFunction.returnType.isNothing()) {
                require(!irFunction.isSuspend) { "Suspend functions should be lowered out at this point"}
                add(LlvmFunctionAttribute.NoReturn)
            }
            if (mustNotInline(contextUtils.context, irFunction)) {
                add(LlvmFunctionAttribute.NoInline)
            }
        }
