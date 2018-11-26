/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class ConstructorConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
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
            element.extraModifiers,
            element.visibility,
            element.modality
        ).also {
            context.symbolProvider.transferSymbol(it, element)
        }
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