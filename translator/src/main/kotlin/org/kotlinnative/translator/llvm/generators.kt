package org.kotlinnative.translator.llvm

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.kotlinnative.translator.llvm.types.*


fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false, arm: Boolean = false) =
        "${if (declare) "declare" else "define"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.getType()} %${s.label}"
        }?.joinToString() }) ${ if (arm) "#0" else ""}"

fun LLVMMapStandardType(name: String, type: KotlinType): LLVMVariable = when {
    type.isFunctionType -> LLVMVariable(name, LLVMFunctionType(type), type.toString(), pointer = true)
    type.toString() == "Int" -> LLVMVariable(name, LLVMIntType(), type.toString())
    type.toString() == "Double" -> LLVMVariable(name, LLVMDoubleType(), type.toString())
    type.isUnit() -> LLVMVariable("", LLVMVoidType())
    else -> LLVMVariable(name, LLVMReferenceType("%$type"), name, pointer = true)
}