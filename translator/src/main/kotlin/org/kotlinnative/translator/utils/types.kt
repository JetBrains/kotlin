package org.kotlinnative.translator.utils

data class FunctionArgument(val type: String, val name: String)

data class FunctionDescriptor(val returnType: String, val argTypes: List<String>)
