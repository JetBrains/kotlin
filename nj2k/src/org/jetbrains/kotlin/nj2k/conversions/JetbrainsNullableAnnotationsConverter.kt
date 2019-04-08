/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.load.java.NOT_NULL_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABLE_ANNOTATIONS
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKTypeElementImpl


class JetbrainsNullableAnnotationsConverter(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKAnnotationListOwner) return recurse(element)

        val annotationsToRemove = mutableListOf<JKAnnotation>()
        for (annotation in element.annotationList.annotations) {
            val nullability = annotation.annotationNullability() ?: continue
            when (element) {
                is JKVariable -> {
                    annotationsToRemove += annotation
                    element.type = JKTypeElementImpl(element.type.type.updateNullability(nullability))
                }
                is JKMethod -> {
                    annotationsToRemove += annotation
                    element.returnType = JKTypeElementImpl(element.returnType.type.updateNullability(nullability))
                }
            }
        }
        element.annotationList.annotations -= annotationsToRemove

        return recurse(element)
    }

    private fun JKAnnotation.annotationNullability(): Nullability? =
        when {
            classSymbol.fqName in nullableAnnotationsFqNames -> Nullability.Nullable
            classSymbol.fqName in notNullAnnotationsFqNames -> Nullability.NotNull
            else -> null
        }


    companion object {
        val nullableAnnotationsFqNames =
            NULLABLE_ANNOTATIONS.map { it.asString() }.toSet()
        val notNullAnnotationsFqNames =
            NOT_NULL_ANNOTATIONS.map { it.asString() }.toSet()
    }
}