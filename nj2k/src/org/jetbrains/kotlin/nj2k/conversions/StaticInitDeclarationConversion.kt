/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.createCompanion
import org.jetbrains.kotlin.nj2k.getCompanion
import org.jetbrains.kotlin.nj2k.replace
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtInitDeclarationImpl

class StaticInitDeclarationConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        val staticInitDeclarations =
            element.classBody.declarations.filterIsInstance<JKJavaStaticInitDeclaration>()
        if (staticInitDeclarations.isEmpty()) return recurse(element)
        val companion = element.getCompanion()
        if (companion == null) {
            element.classBody.declarations = element.classBody.declarations.replace(
                staticInitDeclarations.first(),
                createCompanion(staticInitDeclarations.map { it.toKtInitDeclaration() })
            )
            element.classBody.declarations -= staticInitDeclarations.drop(1)
        } else {
            companion.classBody.declarations += staticInitDeclarations.map { it.toKtInitDeclaration() }
            element.classBody.declarations -= staticInitDeclarations
        }

        return recurse(element)
    }

    private fun JKJavaStaticInitDeclaration.toKtInitDeclaration() =
        JKKtInitDeclarationImpl(::block.detached()).withNonCodeElementsFrom(this)
}