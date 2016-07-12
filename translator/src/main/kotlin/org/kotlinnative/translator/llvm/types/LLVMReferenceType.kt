package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMReferenceType(val type: String) : LLVMType() {
    override fun operatorTimes(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }

    override fun operatorMinus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }

    override fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression {
        throw UnsupportedOperationException("not implemented")
    }

    override fun toString() = type

    override val align = -1
}