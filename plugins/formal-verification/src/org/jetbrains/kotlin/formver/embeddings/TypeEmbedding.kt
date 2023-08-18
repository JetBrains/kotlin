/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.PermExp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

interface TypeEmbedding {
    val type: Type
    fun invariants(v: Exp): List<Exp> = emptyList()

    /**
     * Invariants that should correlate the old and new value of a value of this type
     * over a function call.  This is exclusively necessary for CallsInPlace.
     *
     * TODO: add support for these in loops, too.
     */
    fun dynamicInvariants(v: Exp): List<Exp> = emptyList()
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

object FunctionTypeEmbedding : TypeEmbedding {
    override val type: Type = Type.Ref

    override fun invariants(v: Exp): List<Exp> =
        listOf(v.fieldAccessPredicate(SpecialFields.FunctionObjectCallCounterField, PermExp.FullPerm()))

    override fun dynamicInvariants(v: Exp): List<Exp> =
        listOf(
            Exp.LeCmp(
                Exp.Old(v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)),
                v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)
            )
        )
}

