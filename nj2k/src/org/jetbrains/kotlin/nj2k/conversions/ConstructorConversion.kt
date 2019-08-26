/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtConstructorImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKStubExpressionImpl

class ConstructorConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaMethod) return recurse(element)
        val outerClass = element.parentOfType<JKClass>() ?: return recurse(element)
        if (element.name.value != outerClass.name.value) return recurse(element)

        element.invalidate()
        val delegationCall = lookupDelegationCall(element.block) ?: JKStubExpressionImpl()

        return JKKtConstructorImpl(
            element.name,
            element.parameters,
            element.block,
            delegationCall,
            element.annotationList,
            element.otherModifierElements,
            element.visibilityElement,
            element.modalityElement
        ).also {
            context.symbolProvider.transferSymbol(it, element)
        }.withNonCodeElementsFrom(element)
    }

    private fun lookupDelegationCall(block: JKBlock): JKDelegationConstructorCall? {
        val firstStatement = block.statements.firstOrNull() ?: return null
        val expressionStatement = firstStatement as? JKExpressionStatement ?: return null
        val expression = expressionStatement.expression as? JKDelegationConstructorCall ?: return null
        block.statements -= expressionStatement
        expressionStatement.invalidate()
        return expression
    }
}