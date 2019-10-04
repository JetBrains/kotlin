/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement


abstract class StatefulRecursiveApplicableConversionBase<S>(context: NewJ2kConverterContext) : MatchBasedConversion(context) {
    open val initialState: S? = null

    final override fun onElementChanged(new: JKTreeElement, old: JKTreeElement) {
        somethingChanged = true
    }

    final override fun runConversion(treeRoot: JKTreeElement, context: NewJ2kConverterContext): Boolean {
        val root = applyToElement(treeRoot, initialState)
        assert(root === treeRoot)
        return somethingChanged
    }

    private var somethingChanged = false

    abstract fun applyToElement(element: JKTreeElement, state: S?): JKTreeElement

    fun <T : JKTreeElement> recurse(element: T, state: S?): T = applyRecursive(element, state, ::applyToElement)
}

abstract class RecursiveApplicableConversionBase(context: NewJ2kConverterContext) :
    StatefulRecursiveApplicableConversionBase<Nothing>(context) {

    final override val initialState = null

    abstract fun applyToElement(element: JKTreeElement): JKTreeElement

    final override fun applyToElement(element: JKTreeElement, state: Nothing?): JKTreeElement = applyToElement(element)

    fun <T : JKTreeElement> recurse(element: T): T = recurse(element, null)
}