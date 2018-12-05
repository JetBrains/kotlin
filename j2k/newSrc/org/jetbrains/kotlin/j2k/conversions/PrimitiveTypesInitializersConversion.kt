/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.copyTree
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class PrimitiveTypesInitializersConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKVariable) return recurse(element)
        val literalInitializer = element.initializer as? JKJavaLiteralExpression ?: return recurse(element)
        val literalType = literalInitializer.type.toPrimitiveType()
            ?: return recurse(element)
        val fieldType = element.type.type as? JKJavaPrimitiveType
            ?: (element.type.type as? JKClassType)?.toPrimitiveType()
            ?: return recurse(element)

        createPrimitiveTypeInitializer(literalInitializer.copyTreeAndDetach(), literalType, fieldType)?.also {
            element.initializer = it
        }
        return element
    }


    private fun createPrimitiveTypeInitializer(
        literal: JKJavaLiteralExpression,
        literalType: JKJavaPrimitiveType,
        fieldType: JKJavaPrimitiveType
    ): JKExpression? {
        if (literalType == fieldType) return null
        if (literalType == JKJavaPrimitiveTypeImpl.INT
            && fieldType == JKJavaPrimitiveTypeImpl.LONG
        ) return null

        if (literalType.isNumberType() && fieldType.isNumberType()) {
            return JKJavaLiteralExpressionImpl(
                literal.literal,
                fieldType.toLiteralType() ?: JKLiteralExpression.LiteralType.INT
            )
        }

        val conversionFunctionName = "to${fieldType.jvmPrimitiveType.javaKeywordName.capitalize()}"
        return JKQualifiedExpressionImpl(
            literal,
            JKKtQualifierImpl.DOT,
            JKJavaMethodCallExpressionImpl(
                context.symbolProvider.provideByFqName("kotlin.$conversionFunctionName"),
                JKExpressionListImpl()
            )
        )
    }
}