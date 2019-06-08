/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKArgumentListImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtCallExpressionImpl

class BoxedTypeOperationsConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return recurse(
            when (element) {
                is JKMethodCallExpression ->
                    convertBoxedTypeUnwrapping(element)
                is JKJavaNewExpression -> convertCreationOfBoxedType(element)
                else -> null
            } ?: element
        )

    }

    private fun convertCreationOfBoxedType(newExpression: JKJavaNewExpression): JKExpression? {
        if (newExpression.classSymbol.fqName !in boxedTypeFqNames) return null
        val singleArgument = newExpression.arguments.arguments.singleOrNull() ?: return null
        return singleArgument::value.detached()
    }

    private fun convertBoxedTypeUnwrapping(methodCallExpression: JKMethodCallExpression): JKExpression? {
        val (boxedJavaType, operationType) =
            primitiveTypeUnwrapRegexp.matchEntire(methodCallExpression.identifier.fqName)
                ?.groupValues
                ?.let {
                    it[1] to it[2]
                } ?: return null
        val primitiveTypeName = boxedTypeToPrimitiveType[boxedJavaType] ?: return null
        if (operationType !in primitiveTypeNames) return null
        return JKKtCallExpressionImpl(
            context.symbolProvider.provideMethodSymbol(
                "kotlin.${primitiveTypeName.capitalize()}.to${operationType.capitalize()}"
            ),
            JKArgumentListImpl()
        ).withNonCodeElementsFrom(methodCallExpression)
    }

    companion object {
        private val boxedTypeFqNames =
            primitiveTypes.map { it.wrapperFqName.asString() }

        private val boxedTypeToPrimitiveType =
            primitiveTypes.map { it.wrapperFqName.asString() to it.javaKeywordName }.toMap()

        private val primitiveTypeNames =
            primitiveTypes.map { it.javaKeywordName }

        private val primitiveTypeUnwrapRegexp =
            """([\w.]+)\.(\w+)Value""".toRegex()
    }
}