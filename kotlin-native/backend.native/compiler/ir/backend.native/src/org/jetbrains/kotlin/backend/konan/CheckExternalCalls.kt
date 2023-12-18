/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.toCValues
import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.*

private fun LLVMValueRef.isFunctionCall() = LLVMIsACallInst(this) != null || LLVMIsAInvokeInst(this) != null

private fun LLVMValueRef.isExternalFunction() = LLVMGetFirstBasicBlock(this) == null


private fun LLVMValueRef.isLLVMBuiltin(): Boolean {
    val name = this.name ?: return false
    return name.startsWith("llvm.")
}


private class CallsChecker(generationState: NativeGenerationState, goodFunctions: List<String>) {
    private val llvm = generationState.llvm
    private val context = generationState.context
    private val goodFunctionsExact = goodFunctions.filterNot { it.endsWith("*") }.toSet()
    private val goodFunctionsByPrefix = goodFunctions.filter { it.endsWith("*") }.map { it.substring(0, it.length - 1) }.sorted()

    private fun isGoodFunction(name: String) : Boolean {
        if (name in goodFunctionsExact) return true
        val insertionPoint = goodFunctionsByPrefix.binarySearch(name).let { if (it < 0) it.inv() else it }
        if (insertionPoint < goodFunctionsByPrefix.size && name.startsWith(goodFunctionsByPrefix[insertionPoint])) return true
        if (insertionPoint > 0 && name.startsWith(goodFunctionsByPrefix[insertionPoint - 1])) return true
        return false
    }

    private fun moduleFunction(name: String) =
            LLVMGetNamedFunction(llvm.module, name) ?: throw IllegalStateException("$name function is not available")

    val getMethodImpl = llvm.externalNativeRuntimeFunction(
            "class_getMethodImplementation",
            LlvmRetType(pointerType(functionType(llvm.voidType, false))),
            listOf(LlvmParamType(llvm.int8PtrType), LlvmParamType(llvm.int8PtrType))
    )

    val getClass = llvm.externalNativeRuntimeFunction(
            "object_getClass",
            LlvmRetType(llvm.int8PtrType),
            listOf(LlvmParamType(llvm.int8PtrType))
    )

    val getSuperClass = llvm.externalNativeRuntimeFunction(
            "class_getSuperclass",
            LlvmRetType(llvm.int8PtrType),
            listOf(LlvmParamType(llvm.int8PtrType))
    )

    val checkerFunction = llvm.externalNativeRuntimeFunction(
            "Kotlin_mm_checkStateAtExternalFunctionCall",
            LlvmRetType(llvm.voidType),
            listOf(LlvmParamType(llvm.int8PtrType), LlvmParamType(llvm.int8PtrType), LlvmParamType(llvm.int8PtrType))
    )

    private data class ExternalCallInfo(val name: String?, val calledPtr: LLVMValueRef)

    private fun LLVMValueRef.getPossiblyExternalCalledFunction(): ExternalCallInfo? {
        fun isIndirectCallArgument(value: LLVMValueRef) = LLVMIsALoadInst(value) != null || LLVMIsAArgument(value) != null ||
                LLVMIsAPHINode(value) != null || LLVMIsASelectInst(value) != null || LLVMIsACallInst(value) != null || LLVMIsAExtractElementInst(value) != null

        fun cleanCalledFunction(value: LLVMValueRef): ExternalCallInfo? {
            return when {
                LLVMIsAFunction(value) != null -> {
                    val valueOrSpecial = value.takeIf { !it.isLLVMBuiltin() }
                            ?: LLVMConstIntToPtr(llvm.int64(CALLED_LLVM_BUILTIN), llvm.int8PtrType)!!
                    ExternalCallInfo(value.name!!, valueOrSpecial).takeIf { value.isExternalFunction() }
                }
                LLVMIsACastInst(value) != null -> cleanCalledFunction(LLVMGetOperand(value, 0)!!)
                isIndirectCallArgument(value) -> ExternalCallInfo(null, value) // this is a callback call
                LLVMIsAInlineAsm(value) != null -> null // this is inline assembly call
                LLVMIsAConstantExpr(value) != null -> {
                    when (LLVMGetConstOpcode(value)) {
                        LLVMOpcode.LLVMBitCast -> cleanCalledFunction(LLVMGetOperand(value, 0)!!)
                        else -> TODO("not implemented constant type in call")
                    }
                }
                LLVMIsAGlobalAlias(value) != null -> cleanCalledFunction(LLVMAliasGetAliasee(value)!!)
                else -> {
                    TODO("not implemented call argument ${llvm2string(value)} called in ${llvm2string(this)}")
                }
            }
        }

        return cleanCalledFunction(LLVMGetCalledValue(this)!!)
    }

    private fun processBasicBlock(functionName: String, block: LLVMBasicBlockRef) {
        val calls = getInstructions(block)
                .filter { it.isFunctionCall() }
                .toList()
        val builder = LLVMCreateBuilderInContext(llvm.llvmContext)!!

        for (call in calls) {
            val calleeInfo = call.getPossiblyExternalCalledFunction() ?: continue
            if (calleeInfo.name != null && isGoodFunction(calleeInfo.name)) continue
            LLVMPositionBuilderBefore(builder, call)
            LLVMBuilderResetDebugLocation(builder)
            val callSiteDescription: String
            val calledName: String?
            val calledPtrLlvm: LLVMValueRef
            when (calleeInfo.name) {
                "objc_msgSend" -> {
                    // objc_msgSend has wrong declaration in header, so generated wrapper is strange, Let's just skip it
                    if (LLVMGetNumArgOperands(call) < 2) continue
                    callSiteDescription = "$functionName (over objc_msgSend)"
                    calledName = null
                    val firstArgI8Ptr = LLVMBuildBitCast(builder, LLVMGetArgOperand(call, 0), llvm.int8PtrType, "")!!
                    val firstArgClassPtr = getClass.buildCall(builder, listOf(firstArgI8Ptr))
                    val isNil = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ, firstArgI8Ptr, LLVMConstNull(llvm.int8PtrType), "")
                    val selector = LLVMGetArgOperand(call, 1)!!
                    val calledPtrLlvmIfNotNilFunPtr = getMethodImpl.buildCall(builder, listOf(firstArgClassPtr, selector))
                    val calledPtrLlvmIfNotNil = LLVMBuildBitCast(builder, calledPtrLlvmIfNotNilFunPtr, llvm.int8PtrType, "")
                    val calledPtrLlvmIfNil = LLVMConstIntToPtr(llvm.int64(MSG_SEND_TO_NULL), llvm.int8PtrType)
                    calledPtrLlvm = LLVMBuildSelect(builder, isNil, calledPtrLlvmIfNil, calledPtrLlvmIfNotNil, "")!!
                }
                "objc_msgSendSuper2" -> {
                    if (LLVMGetNumArgOperands(call) < 2) continue
                    callSiteDescription = "$functionName (over objc_msgSendSuper2)"
                    calledName = null
                    // This is https://developer.apple.com/documentation/objectivec/objc_super?language=objc
                    // We don't want to look this type up, so let's just use our own struct.
                    // TODO: Do we need this with fresh LLVM?
                    val superStructType = llvm.structType(llvm.int8PtrType, llvm.int8PtrType)
                    val superStruct = LLVMGetArgOperand(call, 0).run {
                        LLVMBuildBitCast(builder, this, pointerType(superStructType), "")!!
                    }
                    val superClassPtrPtr = LLVMBuildStructGEP2(builder, superStructType, superStruct, 1, "")
                    val superClassPtr = LLVMBuildLoad2(builder, llvm.int8PtrType, superClassPtrPtr, "")!!
                    val classPtr = getSuperClass.buildCall(builder, listOf(superClassPtr))
                    val calledPtrLlvmFunPtr = getMethodImpl.buildCall(builder, listOf(classPtr, LLVMGetArgOperand(call, 1)!!))
                    calledPtrLlvm = LLVMBuildBitCast(builder, calledPtrLlvmFunPtr, llvm.int8PtrType, "")!!
                }
                else -> {
                    callSiteDescription = functionName
                    calledName = calleeInfo.name
                    calledPtrLlvm = when (val typeKind = LLVMGetTypeKind(calleeInfo.calledPtr.type)) {
                        LLVMTypeKind.LLVMPointerTypeKind -> LLVMBuildBitCast(builder, calleeInfo.calledPtr, llvm.int8PtrType, "")!!
                        LLVMTypeKind.LLVMIntegerTypeKind -> LLVMBuildIntToPtr(builder, calleeInfo.calledPtr, llvm.int8PtrType, "")!!
                        else -> TODO("Unsupported typeKind=${typeKind} of calledPtr=${llvm2string(calleeInfo.calledPtr)}")
                    }
                }
            }
            val callSiteDescriptionLlvm = llvm.staticData.cStringLiteral(callSiteDescription).llvm
            val calledNameLlvm = if (calledName == null) LLVMConstNull(llvm.int8PtrType)!! else llvm.staticData.cStringLiteral(calledName).llvm
            checkerFunction.buildCall(builder, listOf(callSiteDescriptionLlvm, calledNameLlvm, calledPtrLlvm))
        }
        LLVMDisposeBuilder(builder)
    }

    fun processFunction(function: LLVMValueRef) {
        if (function.name == checkerFunction.name) return
        getBasicBlocks(function).forEach {
            processBasicBlock(function.name!!, it)
        }
    }

    companion object {
        const val MSG_SEND_TO_NULL: Long = -1
        const val CALLED_LLVM_BUILTIN: Long = -2
    }
}

private const val functionListGlobal = "Kotlin_callsCheckerKnownFunctions"
private const val functionListSizeGlobal = "Kotlin_callsCheckerKnownFunctionsCount"

internal fun checkLlvmModuleExternalCalls(generationState: NativeGenerationState) {
    val llvm = generationState.llvm
    val staticData = llvm.staticData


    val ignoredFunctions = (llvm.runtimeAnnotationMap["no_external_calls_check"] ?: emptyList())

    val goodFunctions = staticData.getGlobal("Kotlin_callsCheckerGoodFunctionNames")?.getInitializer()?.run {
        getOperands(this).map {
            LLVMGetInitializer(LLVMGetOperand(it, 0))!!.getAsCString()
        }.toList()
    } ?: emptyList()

    val checker = CallsChecker(generationState, goodFunctions)
    getFunctions(llvm.module)
            .filter { !it.isExternalFunction() && it !in ignoredFunctions }
            .forEach(checker::processFunction)
    // otherwise optimiser can inline it
    staticData.getGlobal(functionListGlobal)?.setExternallyInitialized(true)
    staticData.getGlobal(functionListSizeGlobal)?.setExternallyInitialized(true)
    verifyModule(llvm.module)
}

// this should be a separate pass, to handle DCE correctly
internal fun addFunctionsListSymbolForChecker(generationState: NativeGenerationState) {
    val llvm = generationState.llvm
    val staticData = llvm.staticData

    val functions = getFunctions(llvm.module)
            .filter { !it.isExternalFunction() }
            .map { constPointer(it).bitcast(llvm.int8PtrType) }
            .toList()
    val functionsArray = staticData.placeGlobalConstArray("", llvm.int8PtrType, functions)
    staticData.getGlobal(functionListGlobal)
            ?.setInitializer(functionsArray)
            ?: throw IllegalStateException("$functionListGlobal global not found")
    staticData.getGlobal(functionListSizeGlobal)
            ?.setInitializer(llvm.constInt32(functions.size))
            ?: throw IllegalStateException("$functionListSizeGlobal global not found")
    verifyModule(llvm.module)
}
