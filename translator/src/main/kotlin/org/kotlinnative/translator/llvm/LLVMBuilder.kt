package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens

class LLVMBuilder {
    private var llvmCode: StringBuilder = StringBuilder()
    private var variableCount = 0

    private fun getNewVariable(): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount")
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
        val newVar = getNewVariable()
        val llvmOperator = when (operation) {
            KtTokens.PLUS -> "add nsw i32"
            KtTokens.MINUS -> "sub nsw i32"
            KtTokens.MUL -> "mul nsw i32"
            else -> throw UnsupportedOperationException("Unkbown binary operator")
        }

        llvmCode.appendln("$newVar = $llvmOperator $firstOp, $secondOp")
        return newVar
    }


    fun clean() {
        llvmCode = StringBuilder()
    }

    override fun toString(): String {
        return llvmCode.toString()
    }
}