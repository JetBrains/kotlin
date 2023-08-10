package org.jetbrains.kotlin.formver.conversion

import viper.silver.ast.Method

interface MethodConversionContext : ProgramConversionContext {
    val toMethod: Method
    val returnVar: ConvertedVar

    fun newAnonVar(type: ConvertedType): ConvertedVar
}