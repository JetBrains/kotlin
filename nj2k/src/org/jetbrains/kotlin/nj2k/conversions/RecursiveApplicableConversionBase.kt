/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

abstract class RecursiveApplicableConversionBase : MatchBasedConversion() {
    override fun onElementChanged(new: JKTreeElement, old: JKTreeElement) {
        somethingChanged = true
    }

    override fun runConversion(treeRoot: JKTreeElement, context: NewJ2kConverterContext): Boolean {
        val root = applyToElement(treeRoot)
        assert(root === treeRoot)
        return somethingChanged
    }

    protected var somethingChanged = false

    abstract fun applyToElement(element: JKTreeElement): JKTreeElement

    fun <T : JKTreeElement> recurse(element: T): T = applyRecursive(element, ::applyToElement)
}