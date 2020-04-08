/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.declarationList
import org.jetbrains.kotlin.nj2k.replace
import org.jetbrains.kotlin.nj2k.tree.*


class PrimaryConstructorDetectConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKClass &&
            (element.classKind == JKClass.ClassKind.CLASS || element.classKind == JKClass.ClassKind.ENUM)
        ) {
            processClass(element)
        }
        return recurse(element)
    }

    private fun processClass(element: JKClass) {
        val constructors = element.declarationList.filterIsInstance<JKConstructor>()
        if (constructors.any { it is JKKtPrimaryConstructor }) return
        val primaryConstructorCandidate = detectPrimaryConstructor(constructors) ?: return
        val delegationCall = primaryConstructorCandidate.delegationCall as? JKDelegationConstructorCall
        if (delegationCall?.expression is JKThisExpression) return


        primaryConstructorCandidate.invalidate()
        if (primaryConstructorCandidate.block.statements.isNotEmpty()) {
            val initDeclaration = JKKtInitDeclaration(primaryConstructorCandidate.block)
                .withFormattingFrom(primaryConstructorCandidate)
            primaryConstructorCandidate.clearFormatting()
            primaryConstructorCandidate.forEachModifier { modifierElement ->
                modifierElement.clearFormatting()
            }
            element.classBody.declarations =
                element.classBody.declarations.replace(primaryConstructorCandidate, initDeclaration)
        } else {
            element.classBody.declarations -= primaryConstructorCandidate
        }

        val primaryConstructor =
            JKKtPrimaryConstructor(
                primaryConstructorCandidate.name,
                primaryConstructorCandidate.parameters,
                primaryConstructorCandidate.delegationCall,
                primaryConstructorCandidate.annotationList,
                primaryConstructorCandidate.otherModifierElements,
                primaryConstructorCandidate.visibilityElement,
                primaryConstructorCandidate.modalityElement
            ).withFormattingFrom(primaryConstructorCandidate)

        symbolProvider.transferSymbol(primaryConstructor, primaryConstructorCandidate)

        element.classBody.declarations += primaryConstructor
    }

    private fun detectPrimaryConstructor(constructors: List<JKConstructor>): JKConstructor? {
        val constructorsWithoutOtherConstructorCall =
            constructors.filterNot { (it.delegationCall as? JKDelegationConstructorCall)?.expression is JKThisExpression }
        return constructorsWithoutOtherConstructorCall.singleOrNull()
    }
}