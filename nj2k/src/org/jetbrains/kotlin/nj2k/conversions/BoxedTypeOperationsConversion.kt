/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.primitiveTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import java.util.*

class BoxedTypeOperationsConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return recurse(
            when (element) {
                is JKCallExpression ->
                    convertBoxedTypeUnwrapping(element)
                is JKNewExpression -> convertCreationOfBoxedType(element)
                else -> null
            } ?: element
        )

    }

    private fun convertCreationOfBoxedType(newExpression: JKNewExpression): JKExpression? {
        if (newExpression.classSymbol.fqName !in boxedTypeFqNames) return null
        val singleArgument = newExpression.arguments.arguments.singleOrNull() ?: return null
        return singleArgument::value.detached()
    }

    private fun convertBoxedTypeUnwrapping(methodCallExpression: JKCallExpression): JKExpression? {
        val (boxedJavaType, operationType) =
            primitiveTypeUnwrapRegexp.matchEntire(methodCallExpression.identifier.fqName)
                ?.groupValues
                ?.let {
                    it[1] to it[2]
                } ?: return null
        val primitiveTypeName = boxedTypeToPrimitiveType[boxedJavaType] ?: return null
        if (operationType !in primitiveTypeNames) return null

        val shouldConvertToIntFirst =
            primitiveTypeName in floatingPointPrimitiveTypeNames && operationType in typeNameOfIntegersLesserThanInt

        val conversionType = if (shouldConvertToIntFirst) "Int" else operationType.capitalize(Locale.US)

        return JKCallExpressionImpl(
            symbolProvider.provideMethodSymbol(
                "kotlin.${primitiveTypeName.capitalize(Locale.US)}.to$conversionType"
            ),
            JKArgumentList()
        ).withFormattingFrom(methodCallExpression)
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

        private val floatingPointPrimitiveTypeNames =
            listOf(JvmPrimitiveType.DOUBLE.javaKeywordName, JvmPrimitiveType.FLOAT.javaKeywordName)

        private val typeNameOfIntegersLesserThanInt =
            listOf(JvmPrimitiveType.SHORT.javaKeywordName, JvmPrimitiveType.BYTE.javaKeywordName)
    }
}
