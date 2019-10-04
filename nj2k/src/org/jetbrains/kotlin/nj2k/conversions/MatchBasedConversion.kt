/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.SequentialBaseConversion
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement


abstract class MatchBasedConversion(override val context: NewJ2kConverterContext) : SequentialBaseConversion {
    inline fun <R : JKTreeElement, T> applyRecursive(element: R, data: T, func: (JKTreeElement, T) -> JKTreeElement): R =
        org.jetbrains.kotlin.nj2k.tree.applyRecursive(element, data, ::onElementChanged, func)

    abstract fun onElementChanged(new: JKTreeElement, old: JKTreeElement)
}