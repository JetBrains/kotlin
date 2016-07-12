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
        return LLVMVariable("%var$variableCount", type, "", pointer)
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
        llvmCode.appendln("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendln("ret void")
    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) {
        addVariableByRef(LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, true), llvmVariable, store)
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type?.align!!}"
        llvmCode.appendln(code)
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        var code = "$target = getelementptr inbounds ${source.type}, ${source.type}* $source, i32 0, i32 $offset"
        llvmCode.appendln(code)
    }

    fun storeVariable(target: LLVMVariable, source: LLVMVariable) {
        var code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        llvmCode.appendln(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        llvmCode.appendln("$targetVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.align}")

        if (store) {
            llvmCode.appendln("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()} $targetVariable, align ${targetVariable.type?.align}")
        }
    }

    fun addVariableByValue(targetVariable: LLVMVariable, sourceVariable: LLVMVariable) {
        val tmp = getNewVariable(targetVariable.type, true)

        llvmCode.appendln("$tmp   = alloca ${tmp.type}, align ${tmp.type?.align}")
        llvmCode.appendln("store ${tmp.type} $sourceVariable, ${tmp.getType()} $tmp, align ${tmp.type?.align}")
        llvmCode.appendln("$targetVariable = load ${targetVariable.type}, ${tmp.getType()} $tmp, align ${targetVariable.type?.align}")

    }

    fun addConstant(sourceVariable: LLVMVariable): LLVMVariable {
        val target = getNewVariable(sourceVariable.type, sourceVariable.pointer)
        addVariableByValue(target, sourceVariable)
        return target
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "%class.$name = type { ${fields.map { it.type }.joinToString()} }"
        llvmCode.appendln(code)
    }

    fun bitcast(dst: LLVMVariable, llvmType: LLVMType): LLVMVariable {
        var empty = getNewVariable(llvmType, true)
        var code = "$empty = bitcast ${dst.getType()} $dst to $llvmType*"
        llvmCode.appendln(code)

        return empty
    }

    fun  memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        var code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        llvmCode.appendln(code)
    }

    override fun toString() = llvmCode.toString()
}