/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKTypeElementImpl


class JetbrainsNullableAnnotationsConverter(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKAnnotationListOwner) return recurse(element)
        val nullableAnnotationSymbol =
            context.symbolProvider.provideByFqName("org.jetbrains.annotations.Nullable")
        val notNullAnnotationSymbol =
            context.symbolProvider.provideByFqName("org.jetbrains.annotations.NotNull")
        val nullableAnnotation =
            element.annotationList.annotations.firstOrNull { it.classSymbol == nullableAnnotationSymbol }
        val notNullAnnotation =
            element.annotationList.annotations.firstOrNull { it.classSymbol == notNullAnnotationSymbol }
        when (element) {
            is JKVariable -> {
                if (nullableAnnotation != null) {
                    element.annotationList.annotations -= nullableAnnotation
                    element.type =
                        JKTypeElementImpl(element.type.type.updateNullability(Nullability.Nullable))
                }
                if (notNullAnnotation != null) {
                    element.annotationList.annotations -= notNullAnnotation
                    element.type =
                        JKTypeElementImpl(element.type.type.updateNullability(Nullability.NotNull))
                }
            }
            is JKMethod -> {
                if (nullableAnnotation != null) {
                    element.annotationList.annotations -= nullableAnnotation
                    element.returnType =
                        JKTypeElementImpl(element.returnType.type.updateNullability(Nullability.Nullable))
                }
                if (notNullAnnotation != null) {
                    element.annotationList.annotations -= notNullAnnotation
                    element.returnType =
                        JKTypeElementImpl(element.returnType.type.updateNullability(Nullability.NotNull))
                }
            }
        }
        return recurse(element)
    }
}