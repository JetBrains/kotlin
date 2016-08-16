package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

class LLVMBuilder(val arm: Boolean = false) {
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

    fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null, scope: LLVMScope = LLVMRegisterScope()): LLVMVariable {
        variableCount++
        return LLVMVariable("var$variableCount", type, kotlinName, scope, pointer)
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

    fun clean() {
        localCode = StringBuilder()
        globalCode = StringBuilder()
        initBuilder()
    }

    fun addAssignment(lhs: LLVMVariable, rhs: LLVMNode) {
        localCode.appendln("$lhs = $rhs")
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

    fun addGlobalInitialize(target: LLVMVariable, fields: ArrayList<LLVMVariable>, initializers: Map<LLVMVariable, String>, classType: LLVMType) {
        val code = "$target = internal global $classType { ${
        fields.map { it.getType() + " " + if (initializers.containsKey(it)) initializers[it] else "0" }.joinToString()
        } }, align ${classType.align}"
        globalCode.appendln(code)
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val stringType = source.type as LLVMStringType
        val code = "store ${target.type} getelementptr inbounds (" +
                "${stringType.fullType()}* $source, i32 0, i32 $offset), ${target.getType()} $target, align ${stringType.align}"
        localCode.appendln(code)
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "$target = getelementptr inbounds ${source.getType()} $source, i32 0, i32 $offset"
        localCode.appendln(code)
    }

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            localCode.appendln("${label.label}:")
    }

    fun addNopInstruction() {
        localCode.appendln(getNewVariable(LLVMIntType()).toString() + " = add i1 0, 0     ; nop instruction")
    }

    fun storeVariable(target: LLVMSingleValue, source: LLVMSingleValue) {
        val code = "store ${source.getType()} $source, ${target.getType()} $target, align ${source.type?.align!!}"
        localCode.appendln(code)
    }

    fun storeNull(result: LLVMVariable) {
        val code = "store ${result.getType().dropLast(1)} null, ${result.getType()} $result, align $POINTER_SIZE"
        localCode.appendln(code)
    }

    fun addComment(comment: String) {
        localCode.appendln("; " + comment)
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

    fun copyVariable(from: LLVMVariable, to: LLVMVariable) = when (from.type) {
        is LLVMStringType -> storeString(to, from, 0)
        else -> copyVariableValue(to, from)
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
        localCode.appendln("$target = alloca ${target.getType()}, align ${target.type.align}")
    }

    fun allocStackPointedVarAsValue(target: LLVMVariable) {
        localCode.appendln("$target = alloca ${target.type}, align ${target.type.align}")
    }

    fun allocStaticVar(target: LLVMVariable) {
        val allocedVar = getNewVariable(LLVMCharType(), pointer = 1)

        val size = if (target.pointer > 0) POINTER_SIZE else target.type.size
        val alloc = "$allocedVar = call i8* @malloc_static(i32 $size)"
        localCode.appendln(alloc)

        val cast = "$target = bitcast ${allocedVar.getType()} $allocedVar to ${target.getType()}*"
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

    fun defineGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) {
        localCode.appendln("$variable = global ${variable.getType()} $defaultValue, align ${variable.type.align}")
    }

    fun makeStructInitializer(args: List<LLVMVariable>, values: List<String>)
            = "{ ${args.mapIndexed { i: Int, variable: LLVMVariable -> "${variable.type} ${values[i]}" }.joinToString()} }"

    fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
        assert(source.pointer > 0)
        val target = getNewVariable(source.type, source.pointer - 1, source.kotlinName)
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
        globalCode.appendln(code)
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