/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver


class ArrayInitializerConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        var newElement = element
        if (element is JKJavaNewArray) {
            val arrayType = element.type.type
            newElement = JKJavaMethodCallExpressionImpl(
                context.symbolProvider.provideByFqName(
                    if (arrayType is JKJavaPrimitiveType)
                        CollectionLiteralResolver.PRIMITIVE_TYPE_TO_ARRAY[PrimitiveType.valueOf(arrayType.jvmPrimitiveType.name)]!!.asString()
                    else
                        CollectionLiteralResolver.ARRAY_OF_FUNCTION.asString()
                ),
                JKExpressionListImpl(element.initializer.also { element.initializer = emptyList() })
            )
        } else if (element is JKJavaNewEmptyArray) {
            newElement = buildArrayInitializer(element.initializer.also { element.initializer = emptyList() }, element.type.type)
        }

        return recurse(newElement)
    }

    private fun buildArrayInitializer(dimensions: List<JKExpression>, type: JKType): JKExpression {
        if (dimensions.size == 1) {
            val methodOrConstructorReference = if (type !is JKJavaPrimitiveType)
                context.symbolProvider.provideByFqName("kotlin/arrayOfNulls")
            else
                JKUnresolvedMethod(arrayFqName(type).replace('/', '.')/*TODO resolve real reference*/)
            return JKJavaMethodCallExpressionImpl(
                methodOrConstructorReference,
                JKExpressionListImpl(dimensions[0]),
                JKTypeArgumentListImpl(if (type is JKJavaPrimitiveType) emptyList() else listOf(JKTypeElementImpl(type)))
            )
        }
        if (dimensions[1] !is JKStubExpression) {
            return JKJavaMethodCallExpressionImpl(
                JKUnresolvedMethod("kotlin.Array"),//TODO resolve real reference
                JKExpressionListImpl(
                    dimensions[0],
                    JKLambdaExpressionImpl(
                        statement = JKExpressionStatementImpl(buildArrayInitializer(dimensions.subList(1, dimensions.size), type))
                    )
                )
            )
        }
        var resultType = JKClassTypeImpl(
            context.symbolProvider.provideByFqName(arrayFqName(type)),
            if (type is JKJavaPrimitiveType) emptyList() else listOf(type),
            Nullability.NotNull
        )
        for (i in 0 until dimensions.size - 2) {
            resultType = JKClassTypeImpl(
                context.symbolProvider.provideByFqName(KotlinBuiltIns.FQ_NAMES.array.asString()),
                listOf(resultType),
                Nullability.NotNull
            )
        }
        return JKJavaMethodCallExpressionImpl(
            context.symbolProvider.provideByFqName("kotlin/arrayOfNulls"),
            JKExpressionListImpl(dimensions[0]),
            JKTypeArgumentListImpl(listOf(JKTypeElementImpl(resultType)))
        )
    }

    private fun arrayFqName(type: JKType): String = if (type is JKJavaPrimitiveType)
        PrimitiveType.valueOf(type.jvmPrimitiveType.name).arrayTypeFqName.asString()
    else KotlinBuiltIns.FQ_NAMES.array.asString()
}