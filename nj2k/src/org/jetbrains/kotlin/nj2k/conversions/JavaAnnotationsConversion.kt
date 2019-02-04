/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationParameterImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtLiteralExpressionImpl
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JavaAnnotationsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKAnnotation) return recurse(element)
        if (element.classSymbol.name == "Deprecated" && element.arguments.isEmpty()) {
            element.arguments +=
                    JKAnnotationParameterImpl(JKKtLiteralExpressionImpl("\"\"", JKLiteralExpression.LiteralType.STRING))
        }
        if (element.classSymbol.fqName == "java.lang.annotation.Target") {
            element.classSymbol = context.symbolProvider.provideByFqName("kotlin.annotation.Target")

            val arguments = element.arguments.singleOrNull()?.let { parameter ->
                val value = parameter.value
                when (value) {
                    is JKKtAnnotationArrayInitializerExpression -> value.initializers
                    else -> listOf(value)
                }
            }
            if (arguments != null) {
                val newArguments =
                    arguments.flatMap { value ->
                        value.fieldAccessFqName()
                            ?.let { targetMappings[it] }
                            ?.map { fqName ->
                                JKFieldAccessExpressionImpl(context.symbolProvider.provideByFqName(fqName))
                            } ?: listOf(value)
                    }

                element.arguments = newArguments.map { JKAnnotationParameterImpl(it) }
            }
        }

        return recurse(element)
    }

    private fun JKAnnotationMemberValue.fieldAccessFqName(): String? =
        (safeAs<JKQualifiedExpression>()?.selector ?: this)
            .safeAs<JKFieldAccessExpression>()
            ?.identifier
            ?.fqName


    companion object {
        private val targetMappings =
            listOf(
                "ANNOTATION_TYPE" to listOf("ANNOTATION_CLASS"),
                "CONSTRUCTOR" to listOf("CONSTRUCTOR"),
                "FIELD" to listOf("FIELD"),
                "LOCAL_VARIABLE" to listOf("LOCAL_VARIABLE"),
                "METHOD" to listOf("FUNCTION", "PROPERTY_GETTER", "PROPERTY_SETTER"),
                "PACKAGE" to listOf("FILE"),
                "PARAMETER" to listOf("VALUE_PARAMETER"),
                "TYPE_PARAMETER" to listOf("TYPE_PARAMETER"),
                "TYPE" to listOf("ANNOTATION_CLASS", "CLASS"),
                "TYPE_USE" to listOf("TYPE_USE")
            ).map { (java, kotlins) ->
                "java.lang.annotation.ElementType.$java" to kotlins.map { "kotlin.annotation.AnnotationTarget.$it" }
            }.toMap()
    }
}