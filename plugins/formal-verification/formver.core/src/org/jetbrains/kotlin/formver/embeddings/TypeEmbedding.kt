/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.domains.AnyDomain
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Type

interface TypeEmbedding {
    // type represents a Viper expression specifying the type. They are defined in TypingDomain and are used for casting, subtyping and the is operator.
    val kotlinType: Exp

    // This represents the Viper type that an expression of this type has (e.g. an object is embedded as a Viper Ref type)
    val viperType: Type

    fun subTypeInvariant(v: Exp) = TypeDomain.isSubtype(TypeOfDomain.typeOf(v), kotlinType)

    fun accessInvariants(v: Exp): List<Exp> = emptyList()

    // This is a list of invariants that are already known to be true, thus they are just assumed by Viper instead of explicitly proven.
    // An example of this is when other systems (e.g. the type checker) have already proven these.
    fun provenInvariants(v: Exp): List<Exp> = emptyList()

    fun invariants(v: Exp): List<Exp> = emptyList()

    /**
     * Invariants that should correlate the old and new value of a value of this type
     * over a function call. When a caller gives away permissions to the callee, these
     * dynamic invariants give properties about the modifications of the argument as
     * part of the functions post conditions.
     * This is exclusively necessary for CallsInPlace.
     */
    fun dynamicInvariants(v: Exp): List<Exp> = emptyList()
}

object UnitTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.unitType()
    override val viperType: Type = UnitDomain.toType()
}

object NothingTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.nothingType()
    override val viperType: Type = UnitDomain.toType()

    override fun invariants(v: Exp): List<Exp> = listOf(Exp.BoolLit(false))
}

object AnyTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.anyType()
    override val viperType = AnyDomain.toType()
    override fun provenInvariants(v: Exp) = listOf(subTypeInvariant(v))
}

object IntTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.intType()
    override val viperType: Type = Type.Int
}

object BooleanTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.booleanType()
    override val viperType: Type = Type.Bool
}

data class NullableTypeEmbedding(val elementType: TypeEmbedding) : TypeEmbedding {
    override val kotlinType = TypeDomain.nullableType(elementType.kotlinType)
    override val viperType: Type = NullableDomain.nullableType(elementType.viperType)

    val nullVal: Exp = NullableDomain.nullVal(elementType.viperType)

    override fun provenInvariants(v: Exp) = listOf(subTypeInvariant(v))
}

object FunctionTypeEmbedding : TypeEmbedding {
    override val kotlinType = TypeDomain.functionType()
    override val viperType: Type = Type.Ref

    override fun provenInvariants(v: Exp): List<Exp> = listOf(subTypeInvariant(v))

    override fun accessInvariants(v: Exp) = listOf(v.fieldAccessPredicate(SpecialFields.FunctionObjectCallCounterField, PermExp.FullPerm()))

    override fun dynamicInvariants(v: Exp): List<Exp> =
        listOf(
            Exp.LeCmp(
                Exp.Old(v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)),
                v.fieldAccess(SpecialFields.FunctionObjectCallCounterField)
            )
        )
}

data class ClassTypeEmbedding(val name: ClassName, val superTypes: List<TypeEmbedding>) : TypeEmbedding {
    override val kotlinType = TypeDomain.classType(this.name)
    override val viperType = Type.Ref

    override fun provenInvariants(v: Exp) = listOf(subTypeInvariant(v))
}