/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode

internal fun interface CirNodeTransformer {

    sealed class Context {
        object Unchecked : Context()
    }

    operator fun Context.invoke(root: CirRootNode)
}

internal class Checked private constructor() : CirNodeTransformer.Context() {
    companion object {
        private val areAssertionsEnabled = this::class.java.desiredAssertionStatus()

        operator fun CirNodeTransformer.invoke(cirRootNode: CirRootNode) {
            val context = if (areAssertionsEnabled) Checked() else Unchecked
            val declarationCountBefore = if (areAssertionsEnabled) cirRootNode.countCirDeclarations() else null

            with(this) {
                context.invoke(cirRootNode)
            }

            if (declarationCountBefore != null) {
                val declarationCountAfter = cirRootNode.countCirDeclarations()
                if (declarationCountAfter.nonArtificialNodeCount > declarationCountBefore.nonArtificialNodeCount) {
                    throw AssertionError(
                        "$this attached declarations not marked as artificial\n" +
                                "Declaration Count before: $declarationCountBefore\n" +
                                "Declaration Count after: $declarationCountAfter"
                    )
                }
            }
        }
    }
}
