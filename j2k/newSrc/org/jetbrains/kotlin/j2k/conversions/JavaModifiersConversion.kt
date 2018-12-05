/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.jvmAnnotation
import org.jetbrains.kotlin.j2k.tree.*

class JavaModifiersConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKVisibilityOwner) {
            if (element.visibility == Visibility.PACKAGE_PRIVATE) {
                element.visibility = Visibility.INTERNAL
            }
        }
        if (element is JKExtraModifiersOwner && element is JKAnnotationListOwner) {
            if (ExtraModifier.VOLATILE in element.extraModifiers) {
                element.extraModifiers -= ExtraModifier.VOLATILE
                element.annotationList.annotations += jvmAnnotation("Volatile", context.symbolProvider)
            }
            if (ExtraModifier.TRANSIENT in element.extraModifiers) {
                element.extraModifiers -= ExtraModifier.TRANSIENT
                element.annotationList.annotations += jvmAnnotation("Transient", context.symbolProvider)
            }
            if (ExtraModifier.STRICTFP in element.extraModifiers) {
                element.extraModifiers -= ExtraModifier.STRICTFP
                element.annotationList.annotations += jvmAnnotation("Strictfp", context.symbolProvider)
            }
            if (ExtraModifier.SYNCHRONIZED in element.extraModifiers) {
                element.extraModifiers -= ExtraModifier.SYNCHRONIZED
                element.annotationList.annotations += jvmAnnotation("Synchronized", context.symbolProvider)
            }
        }
        return recurse(element)
    }
}