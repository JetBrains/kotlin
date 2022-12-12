/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMValueRef

internal class InlineFunctionDebugInfo {
    data class ParameterInfo(val value: LLVMValueRef, val variableLocation: VariableDebugLocation)

    val refSlotParameters = mutableListOf<ParameterInfo>()
    val slotParameters = mutableListOf<ParameterInfo>()

    fun addVariableRecord(variableRecord: VariableManager.SlotRecord, variableLocation: VariableDebugLocation) {
        val address = variableRecord.address()
        if (variableRecord.refSlot) {
            refSlotParameters.add(ParameterInfo(address, variableLocation))
        } else {
            slotParameters.add(ParameterInfo(address, variableLocation))
        }
    }
}
