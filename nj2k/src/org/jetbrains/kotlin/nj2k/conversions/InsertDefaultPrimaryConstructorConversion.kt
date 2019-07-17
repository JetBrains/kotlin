/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseClassSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*


class InsertDefaultPrimaryConstructorConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind != JKClass.ClassKind.CLASS) return recurse(element)
        if (element.declarationList.any { it is JKKtConstructor }) return recurse(element)

        val constructor = JKKtPrimaryConstructorImpl(
            JKNameIdentifierImpl(element.name.value),
            emptyList(),
            JKStubExpressionImpl(),
            JKAnnotationListImpl(),
            emptyList(),
            JKVisibilityModifierElementImpl(Visibility.PUBLIC),
            JKModalityModifierElementImpl(Modality.FINAL)
        )

        element.classBody.declarations += constructor

        val superClassSymbol =
            (element.inheritance.extends.singleOrNull() as? JKClassType)?.classReference

        if (superClassSymbol is JKUniverseClassSymbol) {
            val superClass = recurse(superClassSymbol.target)
            val superConstructor = context.symbolProvider.provideUniverseSymbol(
                superClass.declarationList.singleOrNull { it is JKKtConstructor && it.parameters.isEmpty() } as? JKMethod ?: return recurse(
                    element
                )
            )
            constructor.delegationCall = JKDelegationConstructorCallImpl(superConstructor, JKSuperExpressionImpl(), JKArgumentListImpl())
        }

        return recurse(element)
    }

    private val JKClassSymbol.kind
        get() = when (this) {
            is JKUniverseClassSymbol -> target.classKind
            else -> null
        }
}