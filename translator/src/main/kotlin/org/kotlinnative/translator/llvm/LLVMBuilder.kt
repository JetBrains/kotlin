package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.*
import java.util.*

class LLVMBuilder(val arm: Boolean = false) {
    private val POINTER_SIZE = 4

    private var localCode: StringBuilder = StringBuilder()
    private var globalCode: StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0

    object UniqueGenerator {
        private var unique = 0
        fun generateUniqueString(): String {
            unique += 1
            return ".unique." + unique
        }
    }

    init {
        initBuilder()
    }

    var exceptions: Map<String, LLVMVariable> = mapOf()

    private fun initBuilder() {
        val declares = arrayOf(
                "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)",
                "declare i8* @malloc_heap(i32)",
                "declare i32 @printf(i8*, ...)",
                "%class.Nothing = type { }",
                "declare void @abort()")

        declares.forEach { globalCode.appendln(it) }

        exceptions = mapOf(
                Pair("KotlinNullPointerException", initializeString("Exception in thread main kotlin.KotlinNullPointerException")))

        val funcAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
        if (arm) {
            globalCode.appendln(funcAttributes)
        }
    }

    fun getNewVariable(type: LLVMType, pointer: Int = 0, kotlinName: String? = null, scope: LLVMScope = LLVMRegisterScope(), prefix: String = "var"): LLVMVariable {
        variableCount++
        return LLVMVariable("$prefix$variableCount", type, kotlinName, scope, pointer)
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

    fun loadArgumentIfRequired(value: LLVMSingleValue, argument: LLVMVariable): LLVMSingleValue {
        var result = value

        while (argument.pointer < result.pointer) {
            val currentArgument = getNewVariable(result.type!!, pointer = result.pointer - 1)
            loadVariable(currentArgument, result as LLVMVariable)
            result = currentArgument
        }

        when (value.type) {
            is LLVMStringType -> if (!(value.type as LLVMStringType).isLoaded) {
                val newVariable = getNewVariable(value.type!!, pointer = result.pointer + 1)
                allocStackVar(newVariable, asValue = true)
                copyVariable(result as LLVMVariable, newVariable)

                result = getNewVariable(argument.type, pointer = newVariable.pointer - 1)
                loadVariable(result, newVariable)
            }
        }

        return result
    }

    fun downLoadArgument(value: LLVMSingleValue, pointer: Int): LLVMSingleValue =
            loadArgumentIfRequired(value, LLVMVariable("", value.type!!, pointer = pointer))

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

    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue, pointer: Int = 0) {
        localCode.appendln("ret $type${"*".repeat(pointer)} $value")
    }

    private fun initializeString(string: String): LLVMVariable {
        val result = getNewVariable(LLVMStringType(string.length), pointer = 0, scope = LLVMVariableScope(), prefix = "exceptions.str.")
        addStringConstant(result, string)
        return result
    }

    fun addStringConstant(variable: LLVMVariable, value: String) {
        val type = variable.type as LLVMStringType
        globalCode.appendln("$variable = private unnamed_addr constant  ${type.fullType()} c\"${value.replace("\"", "\\\"")}\\00\", align 1")
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
        (target.type as LLVMStringType).isLoaded = true
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
        if ((source.type is LLVMStringType) && (!(source.type as LLVMStringType).isLoaded)) {
            storeString(target as LLVMVariable, source as LLVMVariable, 0)
        } else {
            val code = "store ${source.getType()} $source, ${target.getType()} $target, align ${source.type?.align!!}"
            localCode.appendln(code)
        }
    }

    fun storeNull(result: LLVMVariable) {
        val code = "store ${result.getType().dropLast(1)} null, ${result.getType()} $result, align $POINTER_SIZE"
        localCode.appendln(code)
    }

    fun nullCheck(variable: LLVMVariable): LLVMVariable {
        val result = getNewVariable(LLVMBooleanType(), pointer = 0)

        val loaded = getNewVariable(variable.type, pointer = variable.pointer - 1)
        loadVariable(loaded, variable)

        val code = "$result = icmp eq ${loaded.getType()} null, $loaded"
        localCode.appendln(code)
        return result
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
        is LLVMStringType -> if ((from.type as LLVMStringType).isLoaded) copyVariableValue(to, from) else storeString(to, from, 0)
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

    fun allocStackVar(target: LLVMVariable, asValue: Boolean = false) {
        localCode.appendln("$target = alloca ${if (asValue) target.type.toString() else target.getType()}, align ${target.type.align}")
    }

    fun allocStaticVar(target: LLVMVariable, asValue: Boolean = false) {
        val allocated = getNewVariable(LLVMCharType(), pointer = 1)

        val size = if (target.pointer > 0) POINTER_SIZE else target.type.size
        val alloc = "$allocated = call i8* @malloc_heap(i32 $size)"
        localCode.appendln(alloc)

        val cast = "$target = bitcast ${allocated.getType()} $allocated to ${if (asValue) target.type.toString() else target.getType()}*"
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

    fun addExceptionCall(exceptionName: String) {
        val exception = exceptions[exceptionName]
        val printResult = getNewVariable(LLVMIntType(), pointer = 0)
        localCode.appendln("$printResult = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds (${(exception!!.type as LLVMStringType).fullType()}* $exception, i32 0, i32 0))")
        addFunctionCall(LLVMVariable("abort", LLVMVoidType(), scope = LLVMVariableScope()), emptyList())
    }

    fun addFunctionCall(functionName: LLVMVariable, arguments: List<LLVMVariable>) {
        localCode.appendln("call ${functionName.type} $functionName(${arguments.joinToString { it -> "${it.type} $it" }})")
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) {
        val code = "call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)"
        localCode.appendln(code)
    }

    override fun toString() = globalCode.toString() + localCode.toString()

}