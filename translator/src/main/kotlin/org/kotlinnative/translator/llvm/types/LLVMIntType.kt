package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable


class LLVMIntType() : LLVMType() {
    override fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression {
        //TODO switch by types: int + double = int
        return LLVMExpression(::LLVMIntType, "add nsw i32 $firstOp, $secondOp")
    }

}