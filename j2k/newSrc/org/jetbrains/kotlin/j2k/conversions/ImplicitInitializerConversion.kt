/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class ImplicitInitializerConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaField) return recurse(element)

        if (element.initializer !is JKStubExpression) return recurse(element)

        val fieldType = element.type.type
        val newInitializer = when (fieldType) {
            is JKClassType, is JKUnresolvedClassType -> JKNullLiteral()
            is JKJavaPrimitiveType -> createPrimitiveTypeInitializer(fieldType)
            else -> null
        }
        newInitializer?.also {
            element.initializer = it
        }
        return element
    }

    fun createPrimitiveTypeInitializer(primitiveType: JKJavaPrimitiveType): JKJavaLiteralExpression =
        when (primitiveType) {
            is JKJavaPrimitiveTypeImpl.BOOLEAN ->
                JKJavaLiteralExpressionImpl("false", JKLiteralExpression.LiteralType.BOOLEAN)
            else ->
                JKJavaLiteralExpressionImpl("0", JKLiteralExpression.LiteralType.INT)
        }

}