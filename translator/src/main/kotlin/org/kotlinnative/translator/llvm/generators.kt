package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.*
import org.kotlinnative.translator.utils.FunctionArgument


fun LLVMFunctionDescriptor(name: String, argTypes: List<FunctionArgument>?, returnType: LLVMType, declare: Boolean = false) =
        "${if (declare) "declare" else "define"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: FunctionArgument ->
            "${s.type} %tmp.${s.name}"
        }?.joinToString() ?: ""})"

fun LLVMMapStandardType(type: String): LLVMType = when (type) {
    "Int" -> LLVMIntType()
    "Double" -> LLVMDoubleType()
    "Unit" -> LLVMVoidType()
    else -> LLVMReferenceType("%$type*")
}