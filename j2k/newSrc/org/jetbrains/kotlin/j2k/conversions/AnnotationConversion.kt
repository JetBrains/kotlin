/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.isVarargsArgument
import org.jetbrains.kotlin.j2k.primaryConstructor
import org.jetbrains.kotlin.j2k.toExpression
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKAnnotationNameParameterImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKAnnotationParameterImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKUniverseClassSymbol

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