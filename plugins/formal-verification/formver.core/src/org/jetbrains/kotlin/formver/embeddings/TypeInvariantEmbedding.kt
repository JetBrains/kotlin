/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.PermExp

/**
 * An invariant for a type.
 *
 * These are different from invariants in general because they are parametrised by a variable of this type,
 * i.e. they can be seen as an `ExpEmbedding` with a hole.
 */
interface TypeInvariantEmbedding {
    fun fillHole(exp: ExpEmbedding): ExpEmbedding
}

fun List<TypeInvariantEmbedding>.fillHoles(exp: ExpEmbedding): List<ExpEmbedding> = map { it.fillHole(exp) }

data object FalseTypeInvariant : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = BooleanLit(false)
}

data class SubTypeInvariantEmbedding(val type: TypeEmbedding) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = Is(exp, type)
}

data class IfNonNullInvariant(val invariant: TypeInvariantEmbedding) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding =
        Implies(NeCmp(exp, exp.type.getNullable().nullVal), invariant.fillHole(exp.withType(exp.type.getNonNullable())))
}

data class FieldAccessTypeInvariantEmbedding(val field: FieldEmbedding, val perm: PermExp) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = FieldAccessPermissions(exp, field, perm)
}

// Note that at present, the predicate name and class name are the same.
// We may want to mangle it better down the line.
data class PredicateAccessTypeInvariantEmbedding(val predicateName: MangledName) : TypeInvariantEmbedding {
    override fun fillHole(exp: ExpEmbedding): ExpEmbedding = PredicateAccessPermissions(predicateName, listOf(exp))
}
