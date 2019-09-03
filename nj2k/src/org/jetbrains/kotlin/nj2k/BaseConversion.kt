/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

interface Conversion {
    val context: NewJ2kConverterContext
    val symbolProvider
        get() = context.symbolProvider

    fun runConversion(treeRoots: List<JKTreeElement>, context: NewJ2kConverterContext): Boolean
}

interface SequentialBaseConversion : Conversion {
    fun runConversion(treeRoot: JKTreeElement, context: NewJ2kConverterContext): Boolean

    override fun runConversion(treeRoots: List<JKTreeElement>, context: NewJ2kConverterContext): Boolean {
        return treeRoots.asSequence().map { runConversion(it, context) }.max() ?: false
    }
}