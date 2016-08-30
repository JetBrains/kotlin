package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.types.*
import java.util.*

class LLVMBuilder(val arm: Boolean = false) {

    private var localCode: StringBuilder = StringBuilder()
    private var globalCode: StringBuilder = StringBuilder()
    private var variableCount = 0
    private var labelCount = 0
    var exceptions: Map<String, LLVMVariable> = mapOf()

    object UniqueGenerator {
        private var unique = 0
        fun generateUniqueString() =
                ".unique." + unique++
    }

    init {
        initBuilder()
    }

    private fun initBuilder() {
        val declares = arrayOf(
                "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)",
                "declare i8* @malloc_heap(i32)",
                "declare i32 @printf(i8*, ...)",
                "%class.Nothing = type { }",
                "declare void @abort()")

        declares.forEach { addLLVMCodeToGlobalPlace(it) }

        exceptions = mapOf(
                Pair("KotlinNullPointerException", initializeExceptionString("Exception in thread main kotlin.KotlinNullPointerException")))

        if (arm) {
            val functionAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
            addLLVMCodeToGlobalPlace(functionAttributes)
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

    fun addLLVMCodeToLocalPlace(code: String) =
            localCode.appendln(code)


    fun addLLVMCodeToGlobalPlace(code: String) =
            globalCode.appendln(code)


    fun addStartExpression() =
            addLLVMCodeToLocalPlace("{")

    fun addEndExpression() =
            addLLVMCodeToLocalPlace("}")


    fun receiveNativeValue(firstOp: LLVMSingleValue): LLVMSingleValue =
            when (firstOp) {
                is LLVMConstant -> firstOp
                is LLVMVariable -> if (firstOp.pointer == 0) firstOp else loadAndGetVariable(firstOp)
                else -> throw UnsupportedOperationException()
            }

    fun receivePointedArgument(variable: LLVMSingleValue, pointer: Int): LLVMSingleValue {
        var currentVariable = variable
        while (currentVariable.pointer > pointer) {
            currentVariable = receiveNativeValue(currentVariable)
        }
        return currentVariable
    }

    fun loadArgsIfRequired(names: List<LLVMSingleValue>, args: List<LLVMVariable>) =
            names.mapIndexed(fun(i: Int, value: LLVMSingleValue): LLVMSingleValue {
                return loadOneArgumentIfRequired(value, args[i])
            }).toList()

    fun loadOneArgumentIfRequired(value: LLVMSingleValue, argument: LLVMVariable): LLVMSingleValue {
        var result = value

        while (argument.pointer < result.pointer) {
            result = loadVariable(result as LLVMVariable)
        }

        if ((value.type is LLVMStringType) && (!(value.type as LLVMStringType).isLoaded)) {
            val newVariable = getNewVariable(value.type!!, pointer = result.pointer + 1)
            allocStackVar(newVariable, asValue = true)
            copyVariable(result as LLVMVariable, newVariable)
            result = loadVariable(newVariable)
        }

        return result
    }

    fun downLoadArgument(value: LLVMSingleValue, pointer: Int): LLVMSingleValue =
            loadOneArgumentIfRequired(value, LLVMVariable("", value.type!!, pointer = pointer))

    fun clean() {
        localCode = StringBuilder()
        globalCode = StringBuilder()
        initBuilder()
    }

    fun addAssignment(lhs: LLVMVariable, rhs: LLVMNode) =
            addLLVMCodeToLocalPlace("$lhs = $rhs")


    fun addReturnOperator(llvmVariable: LLVMSingleValue) =
            addLLVMCodeToLocalPlace("ret ${llvmVariable.type} $llvmVariable")


    fun addAnyReturn(type: LLVMType, value: String = type.defaultValue, pointer: Int = 0) =
            addLLVMCodeToLocalPlace("ret $type${"*".repeat(pointer)} $value")


    private fun initializeExceptionString(string: String): LLVMVariable {
        val result = getNewVariable(LLVMStringType(string.length), pointer = 0, scope = LLVMVariableScope(), prefix = "exceptions.str.")
        addStringConstant(result, string)
        return result
    }

    fun addStringConstant(variable: LLVMVariable, value: String) =
            addLLVMCodeToGlobalPlace("$variable = private unnamed_addr constant  ${(variable.type as LLVMStringType).fullArrayType} c\"${value.replace("\"", "\\\"")}\\00\", align 1")


    fun convertVariableToType(variable: LLVMSingleValue, targetType: LLVMType): LLVMSingleValue {
        var resultVariable = variable
        if (variable.type != targetType) {
            val convertedExpression = targetType.convertFrom(variable)
            resultVariable = getNewVariable(convertedExpression.variableType)
            addAssignment(resultVariable, convertedExpression)
        }
        return resultVariable
    }

    fun addGlobalInitialize(target: LLVMVariable, fields: ArrayList<LLVMVariable>, initializers: Map<LLVMVariable, String>, classType: LLVMType) {
        val code = "$target = internal global $classType { ${
        fields.map { it.getType() + " " + if (initializers.containsKey(it)) initializers[it] else "0" }.joinToString()
        } }, align ${classType.align}"
        globalCode.appendln(code)
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        val code = "store ${target.type} getelementptr inbounds (" +
                "${(source.type as LLVMStringType).fullArrayType}* $source, i32 0, i32 $offset), ${target.getType()} $target, align ${source.type.align}"
        (target.type as LLVMStringType).isLoaded = true
        localCode.appendln(code)
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) =
            addLLVMCodeToLocalPlace("$target = getelementptr inbounds ${source.getType()} $source, i32 0, i32 $offset")

    fun markWithLabel(label: LLVMLabel?) {
        if (label != null)
            addLLVMCodeToLocalPlace("${label.label}:")
    }

    fun storeVariable(target: LLVMSingleValue, source: LLVMSingleValue) {
        if ((source.type is LLVMStringType) && (!(source.type as LLVMStringType).isLoaded)) {
            storeString(target as LLVMVariable, source as LLVMVariable, 0)
        } else {
            addLLVMCodeToLocalPlace("store ${source.getType()} $source, ${target.getType()} $target, align ${source.type?.align!!}")
        }
    }


    fun storeExpression(target: LLVMSingleValue, expression: LLVMExpression): LLVMVariable {
        val resultOp = getNewVariable(expression.variableType)
        addAssignment(resultOp, expression)
        storeVariable(target, resultOp)
        return resultOp
    }

    fun storeNull(result: LLVMVariable) =
            addLLVMCodeToLocalPlace("store ${result.getType().dropLast(1)} null, ${result.getType()} $result, align ${TranslationState.pointerAlign}")

    fun nullCheck(variable: LLVMVariable): LLVMVariable {
        val result = getNewVariable(LLVMBooleanType(), pointer = 0)
        val loaded = loadVariable(variable)

        addLLVMCodeToLocalPlace("$result = icmp eq ${loaded.getType()} null, $loaded")
        return result
    }

    fun addComment(comment: String) =
            addLLVMCodeToLocalPlace("; " + comment)

    fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) =
            addLLVMCodeToLocalPlace("$target = getelementptr inbounds ${source.type} $source, ${index.type} ${index.value}")


    private fun copyVariableValue(target: LLVMVariable, source: LLVMVariable) {
        var from = source
        if (source.pointer > 0) {
            from = getNewVariable(source.type, source.pointer)
            addLLVMCodeToLocalPlace("$from = load ${source.getType()} $source, align ${from.type.align}")
        }
        addLLVMCodeToLocalPlace("store ${target.type} $from, ${target.getType()} $target, align ${from.type.align}")
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

    fun loadVariable(source: LLVMVariable): LLVMVariable {
        val target = getNewVariable(source.type, pointer = source.pointer - 1)
        addLLVMCodeToLocalPlace("$target = load ${source.getType()} $source, align ${target.type.align}")
        return target
    }

    fun allocStackVar(target: LLVMVariable, asValue: Boolean = false, pointer: Boolean = false) {
        val type = if (asValue) target.type.toString() else target.getType()
        addLLVMCodeToLocalPlace("$target = alloca ${if (pointer) type.removeSuffix("*") else type}, align ${target.type.align}")
    }

    fun allocStaticVar(target: LLVMVariable, asValue: Boolean = false, pointer: Boolean = false) {
        val allocated = getNewVariable(LLVMCharType(), pointer = 1)

        val size = if ((target.pointer >= 2) || (target.pointer >= 1 && !pointer)) TranslationState.pointerSize else target.type.size
        addLLVMCodeToLocalPlace("$allocated = call i8* @malloc_heap(i32 $size)")

        addLLVMCodeToLocalPlace("$target = bitcast ${allocated.getType()} $allocated to ${if (asValue) target.type.toString() else target.getType()}" + if (pointer) "" else "*")
    }

    fun addVariableByRef(targetVariable: LLVMVariable, sourceVariable: LLVMVariable, store: Boolean) {
        addLLVMCodeToLocalPlace("$targetVariable = alloca ${sourceVariable.getType()}, align ${sourceVariable.type.align}")

        if (store) {
            addLLVMCodeToLocalPlace("store ${sourceVariable.getType()} $sourceVariable, ${targetVariable.getType()} $targetVariable, align ${targetVariable.type.align}")
        }
    }

    fun defineGlobalVariable(variable: LLVMVariable, defaultValue: String = variable.type.defaultValue) =
            addLLVMCodeToLocalPlace("$variable = global ${variable.getType()} $defaultValue, align ${variable.type.align}")


    fun makeStructInitializer(args: List<LLVMVariable>, values: List<String>)
            = "{ ${args.mapIndexed { i: Int, variable: LLVMVariable -> "${variable.type} ${values[i]}" }.joinToString()} }"

    fun loadAndGetVariable(source: LLVMVariable): LLVMVariable {
        assert(source.pointer > 0)
        val target = getNewVariable(source.type, source.pointer - 1, source.kotlinName)
        addLLVMCodeToLocalPlace("$target = load ${source.getType()} $source, align ${target.type.align}")
        return target
    }

    fun addCondition(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) =
            addLLVMCodeToLocalPlace("br ${condition.getType()} $condition, label $thenLabel, label $elseLabel")


    fun addUnconditionalJump(label: LLVMLabel) =
            addLLVMCodeToLocalPlace("br label $label")


    fun createClass(name: String, fields: List<LLVMVariable>) =
            addLLVMCodeToGlobalPlace("%class.$name = type { ${fields.map { it.getType() }.joinToString()} }")


    fun bitcast(src: LLVMVariable, llvmType: LLVMVariable): LLVMVariable {
        val empty = getNewVariable(llvmType.type, pointer = llvmType.pointer)
        addLLVMCodeToLocalPlace("$empty = bitcast ${src.getType()} $src to ${llvmType.getType()}")
        return empty
    }

    fun addExceptionCall(exceptionName: String) {
        val exception = exceptions[exceptionName]
        val printResult = getNewVariable(LLVMIntType(), pointer = 0)
        addLLVMCodeToLocalPlace("$printResult = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds (${(exception!!.type as LLVMStringType).fullArrayType}* $exception, i32 0, i32 0))")
        addFunctionCall(LLVMVariable("abort", LLVMVoidType(), scope = LLVMVariableScope()), emptyList())
    }

    fun addFunctionCall(functionName: LLVMVariable, arguments: List<LLVMVariable>) =
            addLLVMCodeToLocalPlace("call ${functionName.type} $functionName(${arguments.joinToString { it -> "${it.type} $it" }})")


    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int = 4, volatile: Boolean = false) =
            addLLVMCodeToLocalPlace("call void @llvm.memcpy.p0i8.p0i8.i64(i8* $castedDst, i8* $castedSrc, i64 $size, i32 $align, i1 $volatile)")


    fun declareEntryPoint(name: String) {
        addLLVMCodeToLocalPlace("define weak void @main()")
        addStartExpression()
        addFunctionCall(LLVMVariable(name, LLVMVoidType(), scope = LLVMVariableScope()), listOf())
        addAnyReturn(LLVMVoidType())
        addEndExpression()
    }

    override fun toString() = globalCode.toString() + localCode.toString()

}