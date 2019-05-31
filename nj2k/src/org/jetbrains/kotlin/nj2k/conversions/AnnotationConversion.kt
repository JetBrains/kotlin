/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.primaryConstructor
import org.jetbrains.kotlin.nj2k.toExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationNameParameterImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationParameterImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtAnnotationArrayInitializerExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKNameIdentifierImpl

class AnnotationConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKAnnotation) return recurse(element)
        fixVarargsInvocation(element)
        for (parameter in element.arguments) {
            parameter.value = parameter.value.toExpression(context.symbolProvider)
        }

        return recurse(element)
    }

    private fun fixVarargsInvocation(annotation: JKAnnotation) {
        val newParameters =
            annotation.arguments.withIndex()
                .flatMap { (index, annotationParameter) ->
                    when {
                        annotationParameter !is JKAnnotationNameParameter
                                && annotation.isVarargsArgument(index)
                                && annotationParameter.value is JKKtAnnotationArrayInitializerExpression ->
                            (annotationParameter.value as JKKtAnnotationArrayInitializerExpression)::initializers
                                .detached()
                                .map { JKAnnotationParameterImpl(it) }
                        annotationParameter is JKAnnotationNameParameter
                                && annotation.isVarargsArgument(annotationParameter.name.value)
                                && annotation.classSymbol.target is JKClass
                                && annotationParameter.value !is JKKtAnnotationArrayInitializerExpression -> {
                            listOf(
                                JKAnnotationNameParameterImpl(
                                    JKKtAnnotationArrayInitializerExpressionImpl(annotationParameter::value.detached()),
                                    JKNameIdentifierImpl(annotationParameter.name.value)
                                )
                            )
                        }
                        annotationParameter is JKAnnotationNameParameterImpl ->
                            listOf(
                                JKAnnotationNameParameterImpl(
                                    annotationParameter::value.detached(),
                                    annotationParameter::name.detached()
                                )
                            )
                        else -> listOf(
                            JKAnnotationParameterImpl(
                                annotationParameter::value.detached()
                            )
                        )
                    }
                }
        annotation.arguments = newParameters
    }

    private fun PsiMethod.isVarArgsAnnotationMethod(isNamedArgument: Boolean) =
        isVarArgs || returnType is JavaArrayType || name == "value" && !isNamedArgument

    private fun JKParameter.isVarArgsAnnotationParameter(isNamedArgument: Boolean) =
        isVarArgs || type.type.isArrayType() || name.value == "value" && !isNamedArgument

    private fun JKAnnotation.isVarargsArgument(index: Int) =
        when (val target = classSymbol.target) {
            is JKClass -> target.primaryConstructor()
                ?.parameters
                ?.getOrNull(index)
                ?.isVarArgsAnnotationParameter(isNamedArgument = false)
            is PsiClass -> target.methods
                .getOrNull(index)
                ?.isVarArgsAnnotationMethod(isNamedArgument = false)
            else -> false
        } ?: false


    private fun JKAnnotation.isVarargsArgument(name: String): Boolean =
        when (val target = classSymbol.target) {
            is JKClass -> target.primaryConstructor()
                ?.parameters
                ?.firstOrNull { it.name.value == name }
                ?.isVarArgsAnnotationParameter(isNamedArgument = true)
            is PsiClass -> target.methods
                .firstOrNull { it.name == name }
                ?.isVarArgsAnnotationMethod(isNamedArgument = true)
            else -> false
        } ?: false
}