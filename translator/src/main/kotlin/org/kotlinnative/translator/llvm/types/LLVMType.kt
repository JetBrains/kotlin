package org.kotlinnative.translator.llvm.types;

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

abstract class LLVMType() {

    abstract fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression;
    abstract fun operatorTimes(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression;
    abstract fun operatorMinus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression;
}
