package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.utils.FunctionArgument

fun LLVMDescriptorGenerate(name: String, argTypes: List<FunctionArgument>?, returnType: String) =
        "define $returnType @$name(${argTypes?.mapIndexed { i: Int, s: FunctionArgument -> "${s.type} %tmp.${s.name}" }?.joinToString() ?: "" })"

fun LLVMMapStandardType(type: String) = when(type) {
    "Int" -> "i32"
    else -> "%$type*"
}