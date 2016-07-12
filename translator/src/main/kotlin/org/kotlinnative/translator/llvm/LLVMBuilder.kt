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
            KtTokens.EQ -> throw UnsupportedOperationException()
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        addAssignment(newVar, llvmExpression)

        return newVar
    }

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmCode.appendln("$llvmVariable = $rhs")
    }

    fun addReturnOperator(llvmVariable: LLVMVariable) {
        llvmCode.appendln("ret i32 $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendln("ret void")
    }

    fun loadVariable(llvmVariable: LLVMVariable) {
        addVariableByRef(llvmVariable, LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName))
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        llvmCode.appendln("$sourceVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.getAlign()}")
        llvmCode.appendln("store ${targetVariable.type} $targetVariable, ${targetVariable.type}* $sourceVariable, align ${targetVariable.type?.getAlign()}")
    }

    fun addVariableByValue(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        val tmp = getNewVariable(targetVariable.type)
        llvmCode.appendln("$tmp   = alloca ${tmp.type}, align ${tmp.type?.getAlign()}")
        llvmCode.appendln("store ${tmp.type} $sourceVariable, ${tmp.type}* $tmp, align ${tmp.type?.getAlign()}")
        llvmCode.appendln("$targetVariable = load ${targetVariable.type}, ${targetVariable.type}* $tmp, align ${targetVariable.type?.getAlign()}")
    }

    fun addConstant(sourceVariable: LLVMVariable): LLVMVariable {
        val target = getNewVariable(sourceVariable.type)
        addVariableByValue(target, sourceVariable)
        return target
    }


    fun  createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${ fields.map { it.type }.joinToString() } }"
        llvmCode.appendln(code)
    }

    override fun toString() = llvmCode.toString()

}