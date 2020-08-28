/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.declarationList
import org.jetbrains.kotlin.nj2k.getCompanion
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.tree.*


class ClassToObjectPromotionConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
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
                    JKClass(
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
                        element.annotationList,
                        element.otherModifierElements,
                        element.visibilityElement,
                        JKModalityModifierElement(Modality.FINAL)
                    ).withFormattingFrom(element)
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

    private fun JKClass.hasInheritors(): Boolean {
        val psi = psi<PsiClass>() ?: return false
        return context.converter.converterServices.oldServices.referenceSearcher.hasInheritors(psi)
    }
}