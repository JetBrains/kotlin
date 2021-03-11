/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

sealed class HLParameterConversion {
    abstract fun convertExpression(expression: String, context: ConversionContext): String
    abstract fun convertType(type: KType): KType
    open val importsToAdd: List<String> get() = emptyList()
}

object HLIdParameterConversion : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext) = expression
    override fun convertType(type: KType): KType = type
}

class HLMapParameterConversion(
    private val parameterName: String,
    private val mappingConversion: HLParameterConversion,
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext): String {
        val innerExpression = mappingConversion.convertExpression(parameterName, context.increaseIndent())
        return buildString {
            appendLine("$expression.map { $parameterName ->")
            appendLine(innerExpression.withIndent(context.increaseIndent()))
            append("}".withIndent(context))
        }
    }

    override fun convertType(type: KType): KType =
        List::class.createType(
            arguments = listOf(
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = type.arguments.single().type?.let(mappingConversion::convertType)
                )
            )
        )

    override val importsToAdd get() = mappingConversion.importsToAdd
}

class HLFunctionCallConversion(
    private val callTemplate: String,
    private val callType: KType,
    override val importsToAdd: List<String> = emptyList()
) : HLParameterConversion() {
    override fun convertExpression(expression: String, context: ConversionContext) =
        callTemplate.replace("{0}", expression)

    override fun convertType(type: KType): KType = callType
}

data class ConversionContext(val currentIndent: Int, val indentUnitValue: Int) {
    fun increaseIndent() = copy(currentIndent = currentIndent + 1)
}

private fun String.withIndent(context: ConversionContext): String {
    val newIndent = " ".repeat(context.currentIndent * context.indentUnitValue)
    return replaceIndent(newIndent)
}
