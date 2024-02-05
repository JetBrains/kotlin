/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import llvm.*
import kotlinx.cinterop.*
import org.jetbrains.kotlin.backend.konan.BitcodePostProcessingContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.getBasicBlocks
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.getInstructions

private fun filterLoads(block: LLVMBasicBlockRef, variable: LLVMValueRef) = getInstructions(block)
        .mapNotNull { LLVMIsALoadInst(it) }
        .filter { inst ->
            LLVMGetOperand(inst, 0)?.let { LLVMIsAGlobalVariable(it) } == variable
        }

private fun process(function: LLVMValueRef, currentThreadTLV: LLVMValueRef) {
    val entry = LLVMGetEntryBasicBlock(function) ?: return
    val load = filterLoads(entry, currentThreadTLV).firstOrNull() ?: return
    getBasicBlocks(function)
            .flatMap { filterLoads(it, currentThreadTLV) }
            .filter { it != load }
            .toList() // to force evaluating of all sequences above, because removing something during iteration is bad idea
            .forEach {
                LLVMReplaceAllUsesWith(it, load)
                LLVMInstructionEraseFromParent(it)
            }
}

internal fun removeMultipleThreadDataLoads(context: BitcodePostProcessingContext) {
    val currentThreadTLV = context.llvm.runtimeAnnotationMap["current_thread_tlv"]?.singleOrNull() ?: return

    getFunctions(context.llvm.module)
            .filter { it.name?.startsWith("kfun:") == true }
            .filterNot { LLVMIsDeclaration(it) == 1 }
            .forEach { process(it, currentThreadTLV) }

    replaceWithCached(context)
}

internal fun replaceWithCached(context: BitcodePostProcessingContext) {
    val currentThreadTLV = context.llvm.runtimeAnnotationMap["current_thread_tlv"]?.singleOrNull() ?: return
    val currentThreadTLVFastAll = context.llvm.runtimeAnnotationMap["current_thread_tlv_fast"]
//            ?.filter { LLVMIsDeclaration(it) == 1 }
//            ?.firstOrNull() ?: return

    currentThreadTLVFastAll?.forEach {
        println("Found replacement ${it.name} of ${it.type}")
    }

    val currentThreadTLVFast = currentThreadTLVFastAll?.firstOrNull() ?: return


    val builder: LLVMBuilderRef = LLVMCreateBuilderInContext(context.llvmContext)!!

    fun callFastGetter(before: LLVMValueRef): LLVMValueRef {
        val type = LLVMGlobalGetValueType(currentThreadTLVFast)
        LLVMPositionBuilderBefore(builder, before)
        return LLVMBuildCall2(builder, type, currentThreadTLVFast, listOf<LLVMValueRef>().toCValues(), 0, "")!!
    }

    getFunctions(context.llvm.module)
            .filterNot { LLVMIsDeclaration(it) == 1 }
            .forEach { func ->
                //println("in func ${ func.name }")
                getBasicBlocks(func)
                        .flatMap { filterLoads(it, currentThreadTLV) }
                        .toList() // to force evaluating of all sequences above, because removing something during iteration is bad idea
                        .forEach {
                            val callRepl = callFastGetter(it)
                            LLVMReplaceAllUsesWith(it, callRepl)
                            LLVMInstructionEraseFromParent(it)
                            LLVMInlineCall(callRepl)
                        }
            }
}

//const val CTDN_NAME = "Kotlin_currentThreadDataNode";
//
//private fun filterCalls(block: LLVMBasicBlockRef) = getInstructions(block)
//        .filter { LLVMIsACallInst(it) != null || LLVMIsAInvokeInst(it) !=null }
//        .filter {
//            val name = LLVMGetValueName(LLVMGetCalledValue(it))?.toKString()
//            name?.let { CTDN_NAME in it } ?: false
//        }
//private fun getCalls(block: LLVMBasicBlockRef) = getInstructions(block)
//        .filter { LLVMIsACallInst(it) != null || LLVMIsAInvokeInst(it) !=null }
//        .map { LLVMGetCalledValue(it) }
//        .mapNotNull {
//            val name = LLVMGetValueName(it)?.toKString()
//            name
//        }
//
//private fun process(function: LLVMValueRef) {
//    val entry = LLVMGetEntryBasicBlock(function) ?: return
//    val calls = filterCalls(entry)
//    println("!!!! ${function.name}")
//    val allCalls = getBasicBlocks(function).flatMap { filterCalls(it) }.toList()
//    calls.firstOrNull()?.let { firstCall ->
//        println("     First call found")
//        var count = 0
//        getBasicBlocks(function)
//                .flatMap { filterCalls(it) }
//                .filter { it != firstCall }
//                .toList() // to force evaluating of all sequences above, because removing something during iteration is bad idea
//                .forEach {
//                    LLVMReplaceAllUsesWith(it, firstCall)
//                    LLVMInstructionEraseFromParent(it)
//                    ++count
//                }
//        println("     $count replaced")
//    }
//}
//
//private fun inlineAll(function: LLVMValueRef) {
//    println("!!!! ${function.name}")
//    getBasicBlocks(function)
//            .flatMap { filterCalls(it) }
//            .toList()
//            .forEach {
//                println("     inlined $it")
//                LLVMInlineCall(it)
//            }
//}
//
//internal fun removeMultipleThreadDataLoads(context: BitcodePostProcessingContext) {
//    //val currentThreadTLVs = context.llvm.runtimeAnnotationMap["current_thread_tlv"] ?: return
//
//    getFunctions(context.llvm.module)
//            //.filter { it.name?.startsWith("kfun:") == true }
//            .filterNot { LLVMIsDeclaration(it) == 1 }
//            .forEach { process(it) }
//
//    getFunctions(context.llvm.module)
//            //.filter { it.name?.startsWith("kfun:") == true }
//            .filterNot { LLVMIsDeclaration(it) == 1 }
//            .forEach { inlineAll(it) }
//}
