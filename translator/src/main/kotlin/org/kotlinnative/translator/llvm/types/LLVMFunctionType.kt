package org.kotlinnative.translator.llvm.types

import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.LLVMInstanceOfStandardType
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMFunctionType(type: KotlinType, state: TranslationState) : LLVMType() {

    override val defaultValue = ""
    override val align: Int = 4
    override var size: Int = 4
    override val mangle: String
    override val typename = "FunctionType"

    val arguments: List<LLVMVariable>
    val returnType: LLVMVariable

    init {
        val types = type.arguments.map { LLVMInstanceOfStandardType("", it.type, state = state) }.toList()
        returnType = types.last()
        arguments = types.dropLast(1)
        mangle = "F.${LLVMType.mangleFunctionArguments(arguments)}.EF"
    }

    fun mangleArgs() = LLVMType.mangleFunctionArguments(arguments)

    override fun toString() =
            "${returnType.type} (${arguments.map { it.getType() }.joinToString()})"

    override fun equals(other: Any?) =
            (other is LLVMFunctionType) && (mangle == other.mangle)

    override fun hashCode() =
            mangle.hashCode()

}