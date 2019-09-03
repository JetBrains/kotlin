/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.getCompanion
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKAnnotationListImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKClassImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKModalityModifierElementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.psi

class ClassToObjectPromotionConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKClass && element.classKind == JKClass.ClassKind.CLASS) {
            val companion = element.getCompanion() ?: return recurse(element)

            val allDeclarationsMatches = element.declarationList.all {
                when (it) {
                    is JKKtPrimaryConstructor -> it.parameters.isEmpty() && it.block.statements.isEmpty()
                    is JKKtInitDeclaration ->
                        it.block.statements.all { statement ->
                            when (statement) {
                                is JKDeclarationStatement -> statement.declaredStatements.isEmpty()
                                else -> false
                            }
                        }
                    is JKClass -> true
                    else -> false
                }
            }

            if (allDeclarationsMatches && !element.hasInheritors()) {
                companion.invalidate()
                element.invalidate()
                return recurse(
                    JKClassImpl(
                        element.name,
                        element.inheritance,
                        JKClass.ClassKind.OBJECT,
                        element.typeParameterList,
                        companion.classBody.also { body ->
                            body.handleDeclarationsModifiers()
                            body.declarations += element.classBody.declarations.filter {
                                //TODO preseve order
                                it is JKClass && it.classKind != JKClass.ClassKind.COMPANION
                            }.map { it.detached(element.classBody) }
                        },
                        JKAnnotationListImpl(),
                        element.otherModifierElements,
                        element.visibilityElement,
                        JKModalityModifierElementImpl(Modality.FINAL)
                    ).withNonCodeElementsFrom(element)
                )
            }
        }

        return recurse(element)
    }

    private fun JKClassBody.handleDeclarationsModifiers() {
        for (declaration in declarations) {
            if (declaration !is JKVisibilityOwner) continue
            if (declaration.visibility == Visibility.PROTECTED) {
                //in old j2k it is internal. should it be private instead?
                declaration.visibility = Visibility.INTERNAL
            }
        }
    }

    private fun JKClass.hasInheritors() =
        context.converter.converterServices.oldServices.referenceSearcher.hasInheritors(psi()!!)
}