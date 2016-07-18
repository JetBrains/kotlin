package org.kotlinnative.translator.llvm

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMBuilder(val arm: Boolean) {
    private val POINTER_SIZE = 4

    private var localCode: StringBuilder = StringBuilder()
    private var globalCode: StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0

    init {
        initBuilder()
    }

    private fun initBuilder() {
        val declares = arrayOf(
                "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)",
                "declare i8* @malloc_static(i32)")

        declares.forEach { globalCode.appendln(it) }

        val funcAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
        if (arm) {
            globalCode.appendln(funcAttributes)
        }
    }

    fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null): LLVMVariable {
        variableCount++
        return LLVMVariable("var$variableCount", type, kotlinName, LLVMRegisterScope(), pointer)
    }

    fun getNewLabel(scope: LLVMScope = LLVMRegisterScope(), prefix: String): LLVMLabel {
        labelCount++
        return LLVMLabel("label.$prefix.$labelCount", scope)
    }

    fun addLLVMCode(code: String) {
        localCode.appendln(code)
    }

    fun addStartExpression() {
        localCode.appendln("{")
    }

    fun addEndExpression() {
        localCode.appendln("}")
    }

    fun receiveNativeValue(firstOp: LLVMSingleValue): LLVMSingleValue = when (firstOp) {
        is LLVMConstant -> firstOp
        is LLVMVariable -> if (firstOp.pointer == 0) firstOp else loadAndGetVariable(firstOp)
        else -> throw UnsupportedOperationException()
    }

    fun addPrimitiveBinaryOperation(operation: IElementType, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMVariable {
        val firstNativeOp = receiveNativeValue(firstOp)
        val secondNativeOp = receiveNativeValue(secondOp)
        val llvmExpression = when (operation) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
            KtTokens.LT -> firstOp.type!!.operatorLt(firstNativeOp, secondNativeOp)
            KtTokens.GT -> firstOp.type!!.operatorGt(firstNativeOp, secondNativeOp)
            KtTokens.LTEQ -> firstOp.type!!.operatorLeq(firstNativeOp, secondNativeOp)
            KtTokens.GTEQ -> firstOp.type!!.operatorGeq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQ -> firstOp.type!!.operatorEq(firstNativeOp, secondNativeOp)
            KtTokens.EXCLEQ -> firstOp.type!!.operatorNeq(firstNativeOp, secondNativeOp)
            KtTokens.EQ -> {
                val result = firstOp as LLVMVariable
                storeVariable(result, secondNativeOp)
                return result
            }
            else -> throw UnsupportedOperationException("Unknown binary operator")
        }
        val resultOp = getNewVariable(llvmExpression.variableType)
        addAssignment(resultOp, llvmExpression)

        return resultOp
    }

    fun clean() {
        localCode = StringBuilder()
        globalCode = StringBuilder()
        initBuilder()
    }

    fun addAssignment(llvmVariable: LLVMVariable, rhs: LLVMNode) {
        localCode.appendln("$llvmVariable = $rhs")
    }

    fun addReturnOperator(llvmVariable: LLVMSingleValue) {
        localCode.appendln("ret ${llvmVariable.type} $llvmVariable")
    }

    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue) {
        localCode.appendln("ret $type $value")
    }

    fun addStringConstant(variable: LLVMVariable, value: String) {
        val type = variable.type as LLVMStringType
        globalCode.appendln("$variable = private unnamed_addr constant  ${type.fullType()} c\"$value\\00\", align 1")
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val stringType = source.type as LLVMStringType
        val code = "store ${target.type} getelementptr inbounds (${stringType.fullType()}, " +
                "${stringType.fullType()}* $source, i32 0, i32 $offset), ${target.getType()} $target, align ${stringType.align}"
        localCode.appendln(code)
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.type}* $source, i32 0, i32 $offset"
        localCode.appendln(code)
    }

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            localCode.appendln("${label.label}:")
    }

    fun addNopInstruction() {
        localCode.appendln(getNewVariable(LLVMIntType()).toString() + " = add i1 0, 0     ; nop instruction")
    }

    fun storeVariable(target: LLVMVariable, source: LLVMSingleValue) {
        val code = "store ${source.type} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        localCode.appendln(code)
    }

    fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) {
        val code = "$target = getelementptr inbounds ${source.type} $source, ${index.type} ${index.value}"
        localCode.appendln(code)
    }

    fun copyVariableValue(target: LLVMVariable, source: LLVMVariable) {
        var from = source
        if (source.pointer > 0) {
            from = getNewVariable(source.type, source.pointer)
            localCode.appendln("$from = load ${source.getType()} $source, align ${from.type.align}")
        }
        localCode.appendln("store ${target.type} $from, ${target.getType()} $target, align ${from.type.align}")
    }

    fun loadArgument(llvmVariable: LLVMVariable, store: Boolean = true): LLVMVariable {
        val allocVar = LLVMVariable("${llvmVariable.label}.addr", llvmVariable.type, llvmVariable.kotlinName, LLVMRegisterScope(), pointer = llvmVariable.pointer + 1)
        addVariableByRef(allocVar, llvmVariable, store)
        return allocVar
    }

    fun loadVariable(target: LLVMVariable, source: LLVMVariable) {
        val code = "$target = load ${source.getType()} $source, align ${target.type.align}"
        localCode.appendln(code)
    }

    fun allocStackVar(target: LLVMVariable) {
        localCode.appendln("$target = alloca ${target.type}, align ${target.type.align}")
    }

    fun allocStaticVar(target: LLVMVariable) {
        val allocedVar = getNewVariable(LLVMCharType(), pointer = 1)

        val size = if (target.pointer > 0) POINTER_SIZE else target.type.size
        val alloc = "$allocedVar = call i8* @malloc_static(i32 $size)"
        localCode.appendln(alloc)

        val cast = "$target = bitcast ${allocedVar.getType()} $allocedVar to ${target.getType()}"
        localCode.appendln(cast)
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        localCode.appendln("$targetVariable = alloca ${sourceVariable.type}${"*".repeat(sourceVariable.pointer)}, align ${sourceVariable.type.align}")

        if (store) {
            localCode.appendln("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()} $targetVariable, align ${targetVariable.type.align}")
        }
    }

    fun addConstant(allocVariable: LLVMVariable, constantValue: LLVMConstant) {
        localCode.appendln("$allocVariable = alloca ${allocVariable.type}, align ${allocVariable.type.align}")
        localCode.appendln("store ${allocVariable.type} $constantValue, ${allocVariable.getType()} $allocVariable, align ${allocVariable.type.align}")
    }

    fun declareGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) {
        localCode.appendln("$variable = global ${variable.type} $defaultValue, align ${variable.type.align}")
    }

    fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
        assert(source.pointer > 0)
        val target = getNewVariable(source.type, source.pointer, source.kotlinName)
        val code = "$target = load ${source.getType()} $source, align ${target.type.align}"
        localCode.appendln(code)
        return target
    }

    fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
        localCode.appendln("br ${condition.getType()} $condition, label $thenLabel, label $elseLabel")
    }

    fun addUnconditionalJump(label: LLVMLabel) {
        localCode.appendln("br label $label")
    }

    fun createClass(name: String, fields: List<LLVMVariable>) {
        val code = "%class.$name = type { ${fields.map { it.getType() }.joinToString()} }"
        localCode.appendln(code)
    }

    fun bitcast(src: LLVMVariable, llvmType: LLVMVariable): LLVMVariable {
        val empty = getNewVariable(llvmType.type, pointer = llvmType.pointer)
        val code = "$empty = bitcast ${src.getType()} $src to ${llvmType.getType()}"
        localCode.appendln(code)
        return empty
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        localCode.appendln(code)
    }

    override fun toString() = globalCode.toString() + localCode.toString()
}