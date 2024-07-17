/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

/**
 * Representation of an entity that, in Viper, should have invariants associated with it.
 */
interface TypeInvariantHolder {
    /**
     * Invariants that provide access to a resource and thus behave linearly.
     */
    fun accessInvariants(): List<TypeInvariantEmbedding> = emptyList()

    // Note: these functions will replace accessInvariants when nested unfold will be implemented
    fun sharedPredicateAccessInvariant(): TypeInvariantEmbedding? = null
    fun uniquePredicateAccessInvariant(): TypeInvariantEmbedding? = null

    /**
     * Invariants that do not depend on the heap, and so do not need to be repeated
     * once they have been established once.
     */
    fun pureInvariants(): List<TypeInvariantEmbedding> = emptyList()
}
