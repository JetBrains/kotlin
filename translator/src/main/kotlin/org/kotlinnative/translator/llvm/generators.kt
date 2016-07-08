package org.kotlinnative.translator.llvm

fun LLVMDescriptorGenerate(name: String, argTypes: List<Pair<String, String>>?, returnType: String) =
        "define $returnType @$name(${argTypes?.mapIndexed { i: Int, s: Pair<String, String> -> "${s.second} %${s.first}" }?.joinToString() ?: "" })"

fun LLVMMapStandardType(type: String) = when(type) {
    "Int" -> "i32"
    else -> "%$type*"
}