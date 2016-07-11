package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder {
    private var llvmCode: StringBuilder = StringBuilder()
    private var variableCount = 0

    fun getNewVariable(type: LLVMType?): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type)
    }

    fun addLLVMCode(code: String) {
        llvmCode.appendln(code)
    }

    fun addStartExpression() {
        llvmCode.appendln("{")
    }

    fun addEndExpression() {
        llvmCode.appendln("}")
    }

    fun addPrimitiveBinaryOperation(operation: IElementType, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMVariable {
        val newVar = getNewVariable(LLVMIntType())
        val llvmExpression = when (operation) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(newVar, firstOp, secondOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(newVar, firstOp, secondOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(newVar, firstOp, secondOp)
            else -> throw UnsupportedOperationException("Unkbown binary operator")
        }

        addAssignment(newVar, llvmExpression)

        return newVar
    }

    fun addAssignment(llvmVariable: LLVMNode, assignExpression: LLVMNode) {
        llvmCode.appendln("$llvmVariable = $assignExpression")
    }

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmCode.appendln("%$llvmVariable = $rhs")
    }

    override fun toString(): String {
        return llvmCode.toString()
    }


}