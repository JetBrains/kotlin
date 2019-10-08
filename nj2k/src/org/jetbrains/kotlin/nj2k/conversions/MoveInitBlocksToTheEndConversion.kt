/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.JKClassBody
import org.jetbrains.kotlin.nj2k.tree.JKKtInitDeclaration
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement


class MoveInitBlocksToTheEndConversion(context : NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClassBody) return recurse(element)
        element.declarations = element.declarations.sortedBy {
            when (it) {
                is JKKtInitDeclaration -> 1
                else -> 0
            }
        }
        return recurse(element)
    }
}