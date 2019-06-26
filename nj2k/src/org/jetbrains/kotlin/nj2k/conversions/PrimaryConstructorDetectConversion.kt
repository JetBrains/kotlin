/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.replace
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtInitDeclarationImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtPrimaryConstructorImpl

class PrimaryConstructorDetectConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKClass &&
            (element.classKind == JKClass.ClassKind.CLASS || element.classKind == JKClass.ClassKind.ENUM)
        ) {
            processClass(element)
        }
        return recurse(element)
    }

    private fun processClass(element: JKClass) {
        val constructors = element.declarationList.filterIsInstance<JKKtConstructor>()
        if (constructors.any { it is JKKtPrimaryConstructor }) return
        val primaryConstructorCandidate = detectPrimaryConstructor(constructors) ?: return
        val delegationCall = primaryConstructorCandidate.delegationCall as? JKDelegationConstructorCall
        if (delegationCall?.expression is JKThisExpression) return


        primaryConstructorCandidate.invalidate()
        if (primaryConstructorCandidate.block.statements.isNotEmpty()) {
            val initDeclaration = JKKtInitDeclarationImpl(primaryConstructorCandidate.block)
                .withNonCodeElementsFrom(primaryConstructorCandidate)
            primaryConstructorCandidate.clearNonCodeElements()
            for (modifierElement in primaryConstructorCandidate.modifierElements()) {
                modifierElement.clearNonCodeElements()
            }
            element.classBody.declarations =
                element.classBody.declarations.replace(primaryConstructorCandidate, initDeclaration)
        } else {
            element.classBody.declarations -= primaryConstructorCandidate
        }

        val primaryConstructor =
            JKKtPrimaryConstructorImpl(
                primaryConstructorCandidate.name,
                primaryConstructorCandidate.parameters,
                primaryConstructorCandidate.delegationCall,
                primaryConstructorCandidate.annotationList,
                primaryConstructorCandidate.otherModifierElements,
                primaryConstructorCandidate.visibilityElement,
                primaryConstructorCandidate.modalityElement
            ).withNonCodeElementsFrom(primaryConstructorCandidate)

        context.symbolProvider.transferSymbol(primaryConstructor, primaryConstructorCandidate)

        element.classBody.declarations += primaryConstructor
    }

    private fun detectPrimaryConstructor(constructors: List<JKKtConstructor>): JKKtConstructor? {
        val constructorsWithoutOtherConstructorCall =
            constructors.filterNot { (it.delegationCall as? JKDelegationConstructorCall)?.expression is JKThisExpression }
        return constructorsWithoutOtherConstructorCall.singleOrNull()
    }
}