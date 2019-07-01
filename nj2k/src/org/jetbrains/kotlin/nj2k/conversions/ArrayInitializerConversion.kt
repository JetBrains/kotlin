/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.toArgumentList
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver


class ArrayInitializerConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        var newElement = element
        if (element is JKJavaNewArray) {
            val primitiveArrayType = element.type.type as? JKJavaPrimitiveType
            val arrayConstructorName =
                if (primitiveArrayType != null)
                    CollectionLiteralResolver.PRIMITIVE_TYPE_TO_ARRAY[PrimitiveType.valueOf(primitiveArrayType.jvmPrimitiveType.name)]!!.asString()
                else
                    CollectionLiteralResolver.ARRAY_OF_FUNCTION.asString()
            val typeArguments =
                if (primitiveArrayType == null) JKTypeArgumentListImpl(listOf(element::type.detached()))
                else JKTypeArgumentListImpl()
            newElement = JKJavaMethodCallExpressionImpl(
                context.symbolProvider.provideMethodSymbol("kotlin.$arrayConstructorName"),
                element.initializer.also { element.initializer = emptyList() }.toArgumentList(),
                typeArguments
            )
        } else if (element is JKJavaNewEmptyArray) {
            newElement = buildArrayInitializer(
                element.initializer.also { element.initializer = emptyList() }, element.type.type
            )
        }

        return recurse(newElement)
    }

    private fun buildArrayInitializer(dimensions: List<JKExpression>, type: JKType): JKExpression {
        if (dimensions.size == 1) {
            return if (type !is JKJavaPrimitiveType) {
                JKJavaMethodCallExpressionImpl(
                    context.symbolProvider.provideMethodSymbol("kotlin.arrayOfNulls"),
                    JKArgumentListImpl(dimensions[0]),
                    JKTypeArgumentListImpl(listOf(JKTypeElementImpl(type)))
                )
            } else {
                JKJavaNewExpressionImpl(
                    context.symbolProvider.provideClassSymbol(type.arrayFqName()),
                    JKArgumentListImpl(dimensions[0]),
                    JKTypeArgumentListImpl(emptyList())
                )
            }
        }
        if (dimensions[1] !is JKStubExpression) {
            val arrayType = dimensions.drop(1).fold(type) { currentType, _ ->
                JKJavaArrayTypeImpl(currentType)
            }
            return JKJavaNewExpressionImpl(
                context.symbolProvider.provideClassSymbol("kotlin.Array"),
                JKArgumentListImpl(
                    dimensions[0],
                    JKLambdaExpressionImpl(
                        JKExpressionStatementImpl(buildArrayInitializer(dimensions.subList(1, dimensions.size), type)),
                        emptyList()
                    )
                ),
                JKTypeArgumentListImpl(listOf(JKTypeElementImpl(arrayType)))
            )
        }
        var resultType = JKClassTypeImpl(
            context.symbolProvider.provideClassSymbol(type.arrayFqName()),
            if (type is JKJavaPrimitiveType) emptyList() else listOf(type),
            Nullability.Default
        )
        for (i in 0 until dimensions.size - 2) {
            resultType = JKClassTypeImpl(
                context.symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.array.toSafe()),
                listOf(resultType),
                Nullability.Default
            )
        }
        return JKJavaMethodCallExpressionImpl(
            context.symbolProvider.provideMethodSymbol("kotlin.arrayOfNulls"),
            JKArgumentListImpl(dimensions[0]),
            JKTypeArgumentListImpl(listOf(JKTypeElementImpl(resultType)))
        )
    }
}