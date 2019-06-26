/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.annotationByFqName
import org.jetbrains.kotlin.nj2k.jvmAnnotation
import org.jetbrains.kotlin.nj2k.tree.*

class JavaModifiersConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKModalityOwner && element is JKAnnotationListOwner) {
            val overrideAnnotation = element.annotationList.annotationByFqName("java.lang.Override")
            if (overrideAnnotation != null) {
                element.annotationList.annotations -= overrideAnnotation
                //TODO change modality to OVERRIDE???
            }
        }
        if (element is JKOtherModifiersOwner && element is JKAnnotationListOwner) {
            element.elementByModifier(OtherModifier.VOLATILE)?.also { modifierElement ->
                element.otherModifierElements -= modifierElement
                element.annotationList.annotations +=
                    jvmAnnotation("Volatile", context.symbolProvider).withNonCodeElementsFrom(modifierElement)
            }

            element.elementByModifier(OtherModifier.TRANSIENT)?.also { modifierElement ->
                element.otherModifierElements -= modifierElement
                element.annotationList.annotations +=
                    jvmAnnotation("Transient", context.symbolProvider).withNonCodeElementsFrom(modifierElement)
            }

            element.elementByModifier(OtherModifier.STRICTFP)?.also { modifierElement ->
                element.otherModifierElements -= modifierElement
                element.annotationList.annotations +=
                    jvmAnnotation("Strictfp", context.symbolProvider).withNonCodeElementsFrom(modifierElement)
            }

            element.elementByModifier(OtherModifier.SYNCHRONIZED)?.also { modifierElement ->
                element.otherModifierElements -= modifierElement
                element.annotationList.annotations +=
                    jvmAnnotation("Synchronized", context.symbolProvider).withNonCodeElementsFrom(modifierElement)
            }

            element.elementByModifier(OtherModifier.NATIVE)?.also { modifierElement ->
                modifierElement.otherModifier = OtherModifier.EXTERNAL
            }
        }
        return recurse(element)
    }
}