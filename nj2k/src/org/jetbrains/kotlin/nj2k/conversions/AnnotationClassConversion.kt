/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.toExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class AnnotationClassConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind != JKClass.ClassKind.ANNOTATION) return recurse(element)
        val javaAnnotationMethods =
            element.classBody.declarations
                .filterIsInstance<JKJavaAnnotationMethod>()
        val constructor = JKKtPrimaryConstructorImpl(
            JKNameIdentifierImpl(""),
            javaAnnotationMethods.map { it.asKotlinAnnotationParameter() },
            JKStubExpressionImpl(),
            JKAnnotationListImpl(),
            emptyList(),
            JKVisibilityModifierElementImpl(Visibility.PUBLIC),
            JKModalityModifierElementImpl(Modality.FINAL)
        )
        element.modality = Modality.FINAL
        element.classBody.declarations += constructor
        element.classBody.declarations -= javaAnnotationMethods
        return recurse(element)
    }

    private fun JKJavaAnnotationMethod.asKotlinAnnotationParameter(): JKParameterImpl {
        val type = returnType.type
            .updateNullabilityRecursively(Nullability.NotNull)
            .replaceJavaClassWithKotlinClassType(context.symbolProvider)
        val initializer = this::defaultValue.detached().toExpression(context.symbolProvider)
        val isVarArgs = type is JKJavaArrayType && name.value == "value"
        return JKParameterImpl(
            JKTypeElementImpl(
                if (!isVarArgs) type else (type as JKJavaArrayType).type
            ),
            JKNameIdentifierImpl(name.value),
            isVarArgs = isVarArgs,
            initializer =
            if (type.isArrayType()
                && initializer !is JKKtAnnotationArrayInitializerExpression
                && initializer !is JKStubExpression
            ) {
                JKKtAnnotationArrayInitializerExpressionImpl(initializer)
            } else initializer
        ).also { parameter ->
            if (leftNonCodeElements.any { it is JKCommentElement }) {
                parameter.leftNonCodeElements = leftNonCodeElements
            }
            if (rightNonCodeElements.any { it is JKCommentElement }) {
                parameter.rightNonCodeElements = rightNonCodeElements
            }

        }
    }
}