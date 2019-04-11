/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.isVarargsArgument
import org.jetbrains.kotlin.nj2k.toExpression
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationNameParameterImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationParameterImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtAnnotationArrayInitializerExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKNameIdentifierImpl

class AnnotationConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
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
                                && annotation.isVarargsArgument(index)
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

}