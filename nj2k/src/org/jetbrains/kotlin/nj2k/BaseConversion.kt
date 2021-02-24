/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory

interface Conversion {
    val context: NewJ2kConverterContext

    val symbolProvider: JKSymbolProvider
        get() = context.symbolProvider

    val typeFactory: JKTypeFactory
        get() = context.typeFactory

    fun runConversion(treeRoots: Sequence<JKTreeElement>, context: NewJ2kConverterContext): Boolean
}

interface SequentialBaseConversion : Conversion {
    fun runConversion(treeRoot: JKTreeElement, context: NewJ2kConverterContext): Boolean

    override fun runConversion(treeRoots: Sequence<JKTreeElement>, context: NewJ2kConverterContext): Boolean {
        return treeRoots.maxOfOrNull { runConversion(it, context) } ?: false
    }
}
