/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.llvm.coverage

import llvm.LLVMConstBitCast
import llvm.LLVMCreatePGOFunctionNameVar
import llvm.LLVMInstrProfIncrement
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction

/**
 * Places calls to `llvm.instrprof.increment` in the beginning of the each
 * region in [functionRegions].
 */
internal class LLVMCoverageInstrumentation(
        override val context: Context,
        private val functionRegions: FunctionRegions,
        private val callSitePlacer: (function: LLVMValueRef, args: List<LLVMValueRef>) -> Unit
) : ContextUtils {

    private val functionNameGlobal = createFunctionNameGlobal(functionRegions.function)

    private val functionHash = llvm.int64(functionRegions.structuralHash)

    // TODO: It's a great place for some debug output.
    fun instrumentIrElement(element: IrElement) {
        functionRegions.regions[element]?.let {
            placeRegionIncrement(it)
        }
    }

    /**
     * See https://llvm.org/docs/LangRef.html#llvm-instrprof-increment-intrinsic
     */
    private fun placeRegionIncrement(region: Region) {
        val numberOfRegions = llvm.int32(functionRegions.regions.size)
        val regionNumber = llvm.int32(functionRegions.regionEnumeration.getValue(region))
        val args = listOf(functionNameGlobal, functionHash, numberOfRegions, regionNumber)
        callSitePlacer(LLVMInstrProfIncrement(llvm.module)!!, args)
    }

    // Each profiled function should have a global with its name in a specific format.
    private fun createFunctionNameGlobal(function: IrFunction): LLVMValueRef {
        val name = function.llvmFunction.llvmValue.name
        val pgoFunctionName = LLVMCreatePGOFunctionNameVar(function.llvmFunction.llvmValue, name)!!
        return LLVMConstBitCast(pgoFunctionName, llvm.int8PtrType)!!
    }
}