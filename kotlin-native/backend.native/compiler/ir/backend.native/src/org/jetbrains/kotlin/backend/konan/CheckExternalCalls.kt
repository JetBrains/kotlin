/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.toCValues
import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.*

private fun getBasicBlocks(function: LLVMValueRef) =
        generateSequence(LLVMGetFirstBasicBlock(function)) { LLVMGetNextBasicBlock(it) }

private fun getInstructions(function: LLVMBasicBlockRef) =
        generateSequence(LLVMGetFirstInstruction(function)) { LLVMGetNextInstruction(it) }

private fun LLVMValueRef.isFunctionCall() = LLVMIsACallInst(this) != null || LLVMIsAInvokeInst(this) != null

private fun LLVMValueRef.isExternalFunction() = LLVMGetFirstBasicBlock(this) == null


private fun LLVMValueRef.isLLVMBuiltin(): Boolean {
    val name = this.name ?: return false
    return name.startsWith("llvm.")
}


private class CallsChecker(val context: Context, val llvmModule: LLVMModuleRef, goodFunctions: List<String>) {
    private val goodFunctionsExact = goodFunctions.filterNot { it.endsWith("*") }.toSet()
    private val goodFunctionsByPrefix = goodFunctions.filter { it.endsWith("*") }.map { it.substring(0, it.length - 1) }.sorted()

    private fun isGoodFunction(name: String) : Boolean {
        if (name in goodFunctionsExact) return true
        val insertionPoint = goodFunctionsByPrefix.binarySearch(name).let { if (it < 0) it.inv() else it }
        if (insertionPoint < goodFunctionsByPrefix.size && name.startsWith(goodFunctionsByPrefix[insertionPoint])) return true
        if (insertionPoint > 0 && name.startsWith(goodFunctionsByPrefix[insertionPoint - 1])) return true
        return false
    }

    private fun externalFunction(name: String, type: LLVMTypeRef) =
            moduleToLlvm.getValue(llvmModule).externalFunction(name, type, context.stdlibModule.llvmSymbolOrigin)

    private fun moduleFunction(name: String) =
            LLVMGetNamedFunction(llvmModule, name) ?: throw IllegalStateException("$name function is not available")

    val getMethodImpl = externalFunction("class_getMethodImplementation", functionType(pointerType(functionType(voidType, false)), false, int8TypePtr, int8TypePtr))
    val getClass = externalFunction("object_getClass", functionType(int8TypePtr, false, int8TypePtr))
    val getSuperClass = externalFunction("class_getSuperclass", functionType(int8TypePtr, false, int8TypePtr))
    val checkerFunction = moduleFunction("Kotlin_mm_checkStateAtExternalFunctionCall")

    private data class ExternalCallInfo(val name: String?, val calledPtr: LLVMValueRef)

    private fun LLVMValueRef.getPossiblyExternalCalledFunction(): ExternalCallInfo? {
        fun isIndirectCallArgument(value: LLVMValueRef) = LLVMIsALoadInst(value) != null || LLVMIsAArgument(value) != null ||
                LLVMIsAPHINode(value) != null || LLVMIsASelectInst(value) != null || LLVMIsACallInst(value) != null

        fun cleanCalledFunction(value: LLVMValueRef): ExternalCallInfo? {
            return when {
                LLVMIsAFunction(value) != null -> {
                    val valueOrSpecial = value.takeIf { !it.isLLVMBuiltin() }
                            ?: LLVMConstIntToPtr(Int64(CALLED_LLVM_BUILTIN).llvm, int8TypePtr)!!
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
        val builder = LLVMCreateBuilderInContext(llvmContext)

        for (call in calls) {
            val calleeInfo = call.getPossiblyExternalCalledFunction() ?: continue
            if (calleeInfo.name != null && isGoodFunction(calleeInfo.name)) continue
            LLVMPositionBuilderBefore(builder, call)
            LLVMBuilderResetDebugLocation(builder)
            val callSiteDescription: String
            val calledName: String?
            val calledPtrLlvm: LLVMValueRef?
            when (calleeInfo.name) {
                "objc_msgSend" -> {
                    // objc_msgSend has wrong declaration in header, so generated wrapper is strange, Let's just skip it
                    if (LLVMGetNumArgOperands(call) < 2) continue
                    callSiteDescription = "$functionName (over objc_msgSend)"
                    calledName = null
                    val firstArgI8Ptr = LLVMBuildBitCast(builder, LLVMGetArgOperand(call, 0), int8TypePtr, "")
                    val firstArgClassPtr = LLVMBuildCall(builder, getClass, listOf(firstArgI8Ptr).toCValues(), 1, "")
                    val isNil = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ, firstArgI8Ptr, LLVMConstNull(int8TypePtr), "")
                    val selector = LLVMGetArgOperand(call, 1)
                    val calledPtrLlvmIfNotNilFunPtr = LLVMBuildCall(builder, getMethodImpl, listOf(firstArgClassPtr, selector).toCValues(), 2, "")
                    val calledPtrLlvmIfNotNil = LLVMBuildBitCast(builder, calledPtrLlvmIfNotNilFunPtr, int8TypePtr, "")
                    val calledPtrLlvmIfNil = LLVMConstIntToPtr(Int64(MSG_SEND_TO_NULL).llvm, int8TypePtr)
                    calledPtrLlvm = LLVMBuildSelect(builder, isNil, calledPtrLlvmIfNil, calledPtrLlvmIfNotNil, "")
                }
                "objc_msgSendSuper2" -> {
                    if (LLVMGetNumArgOperands(call) < 2) continue
                    callSiteDescription = "$functionName (over objc_msgSendSuper2)"
                    calledName = null
                    val superStruct = LLVMGetArgOperand(call, 0)
                    val superClassPtrPtr = LLVMBuildGEP(builder, superStruct, listOf(Int32(0).llvm, Int32(1).llvm).toCValues(), 2, "")
                    val superClassPtr = LLVMBuildLoad(builder, superClassPtrPtr, "")
                    val classPtr = LLVMBuildCall(builder, getSuperClass, listOf(superClassPtr).toCValues(), 1, "")
                    val calledPtrLlvmFunPtr = LLVMBuildCall(builder, getMethodImpl, listOf(classPtr, LLVMGetArgOperand(call, 1)).toCValues(), 2, "")
                    calledPtrLlvm = LLVMBuildBitCast(builder, calledPtrLlvmFunPtr, int8TypePtr, "")
                }
                else -> {
                    callSiteDescription = functionName
                    calledName = calleeInfo.name
                    calledPtrLlvm = LLVMBuildBitCast(builder, calleeInfo.calledPtr, int8TypePtr, "")
                }
            }
            val llvm = moduleToLlvm.getValue(llvmModule)
            val callSiteDescriptionLlvm = llvm.staticData.cStringLiteral(callSiteDescription).llvm
            val calledNameLlvm = if (calledName == null) LLVMConstNull(int8TypePtr) else llvm.staticData.cStringLiteral(calledName).llvm
            LLVMBuildCall(builder, checkerFunction, listOf(callSiteDescriptionLlvm, calledNameLlvm, calledPtrLlvm).toCValues(), 3, "")
        }
        LLVMDisposeBuilder(builder)
    }

    fun processFunction(function: LLVMValueRef) {
        if (function == checkerFunction) return
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

internal fun checkLlvmModuleExternalCalls(context: Context, llvmModule: LLVMModuleRef) {
    val staticData = moduleToStaticData.getValue(llvmModule)

    val annotations = staticData.getGlobal("llvm.global.annotations")?.getInitializer()

    val ignoredFunctions = annotations?.run {
        getOperands(this).mapNotNull {
            val annotationName = LLVMGetInitializer(LLVMGetOperand(LLVMGetOperand(it, 1), 0))?.getAsCString()
            if (annotationName == "no_external_calls_check") {
                LLVMGetOperand(LLVMGetOperand(it, 0), 0)!!.name
            } else {
                null
            }
        }.toSet()
    } ?: emptySet()

    val goodFunctions = staticData.getGlobal("Kotlin_callsCheckerGoodFunctionNames")?.getInitializer()?.run {
        getOperands(this).map {
            LLVMGetInitializer(LLVMGetOperand(it, 0))!!.getAsCString()
        }.toList()
    } ?: emptyList()

    val checker = CallsChecker(context, llvmModule, goodFunctions)
    getFunctions(llvmModule)
            .filter { !it.isExternalFunction() && it.name !in ignoredFunctions }
            .forEach(checker::processFunction)
    // otherwise optimiser can inline it
    staticData.getGlobal(functionListGlobal)?.setExternallyInitialized(true);
    staticData.getGlobal(functionListSizeGlobal)?.setExternallyInitialized(true);
    context.verifyBitCode()
}

// this should be a separate pass, to handle DCE correctly
internal fun addFunctionsListSymbolForChecker(context: Context, llvmModule: LLVMModuleRef) {
    val staticData = moduleToStaticData.getValue(llvmModule)

    val functions = getFunctions(llvmModule)
            .filter { !it.isExternalFunction() }
            .map { constPointer(it).bitcast(int8TypePtr) }
            .toList()
    val functionsArray = staticData.placeGlobalConstArray("", int8TypePtr, functions)
    staticData.getGlobal(functionListGlobal)
            ?.setInitializer(functionsArray)
            ?: throw IllegalStateException("$functionListGlobal global not found")
    staticData.getGlobal(functionListSizeGlobal)
            ?.setInitializer(Int32(functions.size))
            ?: throw IllegalStateException("$functionListSizeGlobal global not found")
    context.verifyBitCode()
}