package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType
import kotlin.reflect.KFunction0

open class LLVMExpression(val variableType: KFunction0<LLVMType>, val llvmCode: String) : LLVMNode() {

    fun generateExpression(builder: LLVMBuilder): LLVMVariable {
        val newVar = builder.getNewVariable(variableType)
        builder.addLLVMCode("%$newVar = $llvmCode")
        return newVar
    }

}