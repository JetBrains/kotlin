package org.kotlinnative.translator.utils

data class FunctionArgument(val type: String, val name: String)

data class KtType(val name: String)

data class FunctionDescriptor(val returnType: KtType, val argTypes: List<KtType>)
