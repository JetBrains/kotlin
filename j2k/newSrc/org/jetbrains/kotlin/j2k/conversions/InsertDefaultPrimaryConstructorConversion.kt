/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class InsertDefaultPrimaryConstructorConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind != JKClass.ClassKind.CLASS) return recurse(element)
        if (element.declarationList.any { it is JKKtConstructor }) return recurse(element)

        val constructor = JKKtPrimaryConstructorImpl(
            JKNameIdentifierImpl(element.name.value),
            emptyList(),
            JKBodyStub,
            JKStubExpressionImpl(),
            emptyList(),
            Visibility.PUBLIC,
            Modality.FINAL

        )

        element.classBody.declarations += constructor

        val superClassSymbol = element.inheritance.inherit.map { it.type }
            .filterIsInstance<JKClassType>()
            .mapNotNull { (it.classReference as? JKClassSymbol) }
            .firstOrNull { it.kind == JKClass.ClassKind.CLASS }

        if (superClassSymbol is JKUniverseClassSymbol) {
            val superClass = recurse(superClassSymbol.target)
            val superConstructor = context.symbolProvider.provideUniverseSymbol(
                superClass.declarationList.single { it is JKKtConstructor && it.parameters.isEmpty() } as JKMethod
            )
            constructor.delegationCall = JKDelegationConstructorCallImpl(superConstructor, JKSuperExpressionImpl(), JKExpressionListImpl())
        }

        return recurse(element)
    }

    private val JKClassSymbol.kind
        get() = when (this) {
            is JKUniverseClassSymbol -> target.classKind
            else -> null
        }
}