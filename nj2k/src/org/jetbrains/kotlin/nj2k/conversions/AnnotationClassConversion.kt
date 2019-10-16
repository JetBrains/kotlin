/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.toExpression
import org.jetbrains.kotlin.nj2k.tree.*

import org.jetbrains.kotlin.nj2k.types.JKJavaArrayType
import org.jetbrains.kotlin.nj2k.types.isArrayType
import org.jetbrains.kotlin.nj2k.types.replaceJavaClassWithKotlinClassType
import org.jetbrains.kotlin.nj2k.types.updateNullabilityRecursively

class AnnotationClassConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind != JKClass.ClassKind.ANNOTATION) return recurse(element)
        val javaAnnotationMethods =
            element.classBody.declarations
                .filterIsInstance<JKJavaAnnotationMethod>()
        val constructor = JKKtPrimaryConstructor(
            JKNameIdentifier(""),
            javaAnnotationMethods.map { it.asKotlinAnnotationParameter() },
            JKStubExpression(),
            JKAnnotationList(),
            emptyList(),
            JKVisibilityModifierElement(Visibility.PUBLIC),
            JKModalityModifierElement(Modality.FINAL)
        )
        element.modality = Modality.FINAL
        element.classBody.declarations += constructor
        element.classBody.declarations -= javaAnnotationMethods
        return recurse(element)
    }

    private fun JKJavaAnnotationMethod.asKotlinAnnotationParameter(): JKParameter {
        val type = returnType.type
            .updateNullabilityRecursively(Nullability.NotNull)
            .replaceJavaClassWithKotlinClassType(symbolProvider)
        val initializer = this::defaultValue.detached().toExpression(symbolProvider)
        val isVarArgs = type is JKJavaArrayType && name.value == "value"
        return JKParameter(
            JKTypeElement(
                if (!isVarArgs) type else (type as JKJavaArrayType).type
            ),
            JKNameIdentifier(name.value),
            isVarArgs = isVarArgs,
            initializer =
            if (type.isArrayType()
                && initializer !is JKKtAnnotationArrayInitializerExpression
                && initializer !is JKStubExpression
            ) {
                JKKtAnnotationArrayInitializerExpression(initializer)
            } else initializer
        ).also { parameter ->
            if (trailingComments.any { it is JKComment }) {
                parameter.trailingComments += trailingComments
            }
            if (leadingComments.any { it is JKComment }) {
                parameter.leadingComments += leadingComments
            }

        }
    }
}