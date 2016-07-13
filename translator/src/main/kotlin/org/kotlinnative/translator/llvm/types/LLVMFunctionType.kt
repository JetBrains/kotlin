package org.kotlinnative.translator.llvm.types

import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.llvm.LLVMMapStandardType
import org.kotlinnative.translator.llvm.LLVMVariable
import java.util.*

class LLVMFunctionType(type: KotlinType) : LLVMType() {
    override val align: Int = 4
    override val size: Byte = 4

    val arguments: List<LLVMVariable>
    val returnType: LLVMVariable

    init {
        val types = type.arguments.map { LLVMMapStandardType("", it.type) }.toList()
        returnType = types.last()
        arguments = types.dropLast(1)
    }

    override fun toString(): String =
            "${returnType.type} (${arguments.map { it.getType() }.joinToString()})"
}
