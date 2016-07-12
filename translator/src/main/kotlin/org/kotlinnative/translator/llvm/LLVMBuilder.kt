package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder {
    private var llvmCode: StringBuilder = StringBuilder()
    private var variableCount = 0

    fun getNewVariable(type: LLVMType?, pointer: Boolean = false, kotlinName: String? = null): LLVMVariable {
        variableCount++
        return LLVMVariable("%var$variableCount", type, kotlinName = kotlinName, pointer = pointer)
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

    fun receiveNativeValue(firstOp: LLVMSingleValue): LLVMSingleValue = when (firstOp) {
        is LLVMConstant -> firstOp
        is LLVMVariable -> when (firstOp.pointer) {
            false -> firstOp
            else -> loadAndGetVariable(firstOp)
        }
        else -> throw UnsupportedOperationException()
    }

    fun addPrimitiveBinaryOperation(operation: IElementType, resultOp: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMVariable {
        val firstNativeOp = receiveNativeValue(firstOp)
        val secondNativeOp = receiveNativeValue(secondOp)
        val llvmExpression = when (operation) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(resultOp, firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(resultOp, firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(resultOp, firstNativeOp, secondNativeOp)
            KtTokens.EQ -> {
                val result = firstOp as LLVMVariable
                storeVariable(result, secondNativeOp)
                return result
            }
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }

        addAssignment(resultOp, llvmExpression)

        return resultOp
    }

    fun clean() {
        llvmCode = StringBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        llvmCode.appendln("$llvmVariable = $rhs")
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        llvmCode.appendln("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addVoidReturn() {
        llvmCode.appendln("ret void")
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.type}, ${source.type}* $source, i32 0, i32 $offset"
        llvmCode.appendln(code)
    }

    fun storeVariable(target: LLVMVariable, source: LLVMSingleValue) {
        val code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        llvmCode.appendln(code)
    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true) {
        addVariableByRef(LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, true), llvmVariable, store)
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type?.align!!}"
        llvmCode.appendln(code)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        llvmCode.appendln("$targetVariable = alloca ${sourceVariable.type}, align ${sourceVariable.type?.align}")

        if (store) {
            llvmCode.appendln("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()} $targetVariable, align ${targetVariable.type?.align}")
        }
    }

    fun addConstant(allocVariable: LLVMVariable, constantValue: LLVMConstant) {
        llvmCode.appendln("$allocVariable   = alloca ${allocVariable.type}, align ${allocVariable.type?.align}")
        llvmCode.appendln("store ${allocVariable.type} $constantValue, ${allocVariable.getType()} $allocVariable, align ${allocVariable.type?.align}")
    }

    fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
        assert(!source.pointer)
        val target = getNewVariable(type = source.type, pointer = source.pointer, kotlinName = source.kotlinName)
        val code = "$target = load ${target.type}, ${source.getType()} $source, align ${target.type?.align!!}"
        llvmCode.appendln(code)
        return target
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "%class.$name = type { ${fields.map { it.type }.joinToString()} }"
        llvmCode.appendln(code)
    }

    fun bitcast(dst: LLVMVariable, llvmType: LLVMType): LLVMVariable {
        val empty = getNewVariable(llvmType, true)
        val code = "$empty = bitcast ${dst.getType()} $dst to $llvmType*"
        llvmCode.appendln(code)

        return empty
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        llvmCode.appendln(code)
    }

    override fun toString() = llvmCode.toString()
}