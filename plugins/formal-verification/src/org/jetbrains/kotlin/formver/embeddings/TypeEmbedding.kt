/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

interface TypeEmbedding {
    val type: Type
    fun invariants(v: Exp): List<Exp> = emptyList()
}

object UnitTypeEmbedding : TypeEmbedding {
    override val type: Type = UnitDomain.toType()
}

object NothingTypeEmbedding : TypeEmbedding {
    override val type: Type = UnitDomain.toType()

    override fun invariants(v: Exp): List<Exp> = listOf(Exp.BoolLit(false))
}

object IntTypeEmbedding : TypeEmbedding {
    override val type: Type = Type.Int
}

object BooleanTypeEmbedding : TypeEmbedding {
    override val type: Type = Type.Bool
}

class TypeVarEmbedding(val name: String) : TypeEmbedding {
    override val type: Type = Type.TypeVar(name)
}

class NullableTypeEmbedding(val elementType: TypeEmbedding) : TypeEmbedding {
    override val type: Type = NullableDomain.toType(mapOf(NullableDomain.T to elementType.type))
}
