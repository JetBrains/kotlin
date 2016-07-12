package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.*


fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false) =
        "${if (declare) "declare" else "define"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.getType()} %${s.label}"
        }?.joinToString() })"

fun LLVMMapStandardType(type: String): LLVMType = when (type) {
    "Int" -> LLVMIntType()
    "Double" -> LLVMDoubleType()
    "Unit" -> LLVMVoidType()
    else -> LLVMReferenceType("%$type*")
}