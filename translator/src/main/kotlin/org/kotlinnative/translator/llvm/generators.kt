package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.utils.FunctionArgument

fun LLVMFunctionDescriptor(name: String, argTypes: List<FunctionArgument>?, returnType: String, declare: Boolean = false) =
        "${ if (declare) "declare" else "define"} $returnType @$name(${
            argTypes?.mapIndexed { i: Int, s: FunctionArgument -> "${s.type} %tmp.${s.name}"
        }?.joinToString() ?: "" })"


fun LLVMMapStandardType(type: String) = when(type) {
    "Int" -> "i32"
    "Unit" -> "void"
    else -> "%$type*"
}