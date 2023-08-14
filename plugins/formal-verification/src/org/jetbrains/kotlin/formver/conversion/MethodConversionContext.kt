package org.jetbrains.kotlin.formver.conversion

import viper.silver.ast.Method

interface MethodConversionContext : ProgramConversionContext {
    val toMethod: Method
    val returnVar: ConvertedVar
    val signature: ConvertedMethodSignature

    fun newAnonVar(type: ConvertedType): ConvertedVar
}