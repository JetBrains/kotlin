package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder {
    private var llvmCode: StringBuilder = StringBuilder()
    private var variableCount = 0

    fun getNewVariable(type: LLVMType?, pointer: Boolean = false): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, pointer = pointer)
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
            KtTokens.EQ -> return moveVariableValue(secondOp, firstOp)
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        addAssignment(newVar, llvmExpression)

        return newVar
    }


    private fun moveVariableValue(from: LLVMVariable, to: LLVMVariable): LLVMVariable {
        //%0 = load i32, i32* %y, align 4
        //store i32 %0, i32* %z, align 4
        //%1 = load i32, i32* %z, align 4
        llvmCode.appendln("VARIABLE from $from TO $to")
        /*
        val tmp1 = getNewVariable(from.type)
        val tmp2 = getNewVariable(to.type)
        llvmCode.appendln("$tmp1 = load ${tmp1.type}, ${tmp1.type}* $from, align ${tmp1.type?.align}")
        llvmCode.appendln("store ${tmp1.type} $tmp1, ${to.type}* $to, align ${to.type?.align}")
        llvmCode.appendln("$tmp2 = load ${tmp2.type}, ${tmp2.type}* $to, align ${to.type?.align}")*/
        return from
    }

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmCode.appendln("$llvmVariable = $rhs")
    }

    fun addReturnOperator(llvmVariable: LLVMVariable) {
        llvmCode.appendln("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendln("ret void")
    }

    fun loadVariable(llvmVariable: LLVMVariable) {
        addVariableByRef(llvmVariable, LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, true))
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        llvmCode.appendln("$sourceVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.align}")
        llvmCode.appendln("store ${sourceVariable.type} $targetVariable, ${targetVariable.getType()} $sourceVariable, align ${targetVariable.type?.align}")
    }

    fun addVariableByValue(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        val tmp = getNewVariable(targetVariable.type, pointer = true)

        llvmCode.appendln("$tmp   = alloca ${tmp.type}, align ${tmp.type?.align}")
        llvmCode.appendln("store ${tmp.type} $sourceVariable, ${tmp.getType()} $tmp, align ${tmp.type?.align}")
        llvmCode.appendln("$targetVariable = load ${targetVariable.type}, ${tmp.getType()} $tmp, align ${targetVariable.type?.align}")

    }

    fun addConstant(sourceVariable: LLVMVariable): LLVMVariable {
        val target = getNewVariable(sourceVariable.type, pointer = sourceVariable.pointer)
        addVariableByValue(target, sourceVariable)
        return target
    }


    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "@class.$name = type { ${fields.map { it.type }.joinToString()} }"
        llvmCode.appendln(code)
    }

    override fun toString() = llvmCode.toString()

}