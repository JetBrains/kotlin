/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtPrimaryConstructorImpl

class PrimaryConstructorDetectConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKClass) {
            processClass(element)
        }
        return recurse(element)
    }

    private fun processClass(element: JKClass) {
        val constructors = element.declarationList.filterIsInstance<JKKtConstructor>()
        if (constructors.any { it is JKKtPrimaryConstructor }) return
        // TODO: Detecting primary for multiple constructors
        // TODO: Detecting primary constructors with field initializers
        val single = constructors.singleOrNull { it.block.statements.isEmpty() } ?: return
        val delegationCall = single.delegationCall as? JKDelegationConstructorCall
        if (delegationCall?.expression is JKThisExpression) return

        element.declarationList -= single

        single.invalidate()

        val primaryConstructor =
            JKKtPrimaryConstructorImpl(
                single.name,
                single.parameters,
                single.block,
                single.modifierList,
                single.delegationCall
            )

        context.symbolProvider.transferSymbol(primaryConstructor, single)

        element.declarationList += primaryConstructor
    }
}