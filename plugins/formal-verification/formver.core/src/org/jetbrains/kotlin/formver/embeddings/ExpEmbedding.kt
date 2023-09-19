/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.domains.*
import org.jetbrains.kotlin.formver.viper.ast.AccessPredicate
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp

sealed interface ExpEmbedding {
    val type: TypeEmbedding
    fun toViper(): Exp

    fun withType(newType: TypeEmbedding): ExpEmbedding =
        if (newType == type) this else Cast(this, newType)
}

fun List<ExpEmbedding>.toViper(): List<Exp> = map { it.toViper() }

sealed interface IntArithExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type
        get() = IntTypeEmbedding
}

data class Add(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper() = Exp.Add(left.toViper(), right.toViper())
}

data class Sub(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper() = Exp.Sub(left.toViper(), right.toViper())
}

data class Mul(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper() = Exp.Mul(left.toViper(), right.toViper())
}

data class Div(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper() = Exp.Div(left.toViper(), right.toViper())
}

data class Mod(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntArithExpression {
    override fun toViper() = Exp.Mod(left.toViper(), right.toViper())
}


sealed interface IntComparisonExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type
        get() = BooleanTypeEmbedding
}

data class LtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper() = Exp.LtCmp(left.toViper(), right.toViper())
}

data class LeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper() = Exp.LeCmp(left.toViper(), right.toViper())
}

data class GtCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper() = Exp.GtCmp(left.toViper(), right.toViper())
}

data class GeCmp(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : IntComparisonExpression {
    override fun toViper() = Exp.GeCmp(left.toViper(), right.toViper())
}


data class EqCmp(
    val left: ExpEmbedding,
    val right: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper() = Exp.EqCmp(left.toViper(), right.toViper())
}

data class NeCmp(
    val left: ExpEmbedding,
    val right: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper() = Exp.NeCmp(left.toViper(), right.toViper())
}


sealed interface BinaryBooleanExpression : ExpEmbedding {
    val left: ExpEmbedding
    val right: ExpEmbedding
    override val type: BooleanTypeEmbedding
        get() = BooleanTypeEmbedding
}

data class And(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper() = Exp.And(left.toViper(), right.toViper())
}

data class Or(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper() = Exp.Or(left.toViper(), right.toViper())
}

data class Implies(
    override val left: ExpEmbedding,
    override val right: ExpEmbedding,
) : BinaryBooleanExpression {
    override fun toViper() = Exp.Implies(left.toViper(), right.toViper())
}

data class Not(
    val exp: ExpEmbedding,
) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper() = Exp.Not(exp.toViper())
}


data object UnitLit : ExpEmbedding {
    override val type = UnitTypeEmbedding

    override fun toViper() = UnitDomain.element
}

data class IntLit(val value: Int) : ExpEmbedding {
    override val type = IntTypeEmbedding

    override fun toViper() = Exp.IntLit(value)
}

data class BooleanLit(val value: Boolean) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper() = Exp.BoolLit(value)
}

data class NullLit(val elemType: TypeEmbedding) : ExpEmbedding {
    override val type = NullableTypeEmbedding(elemType)

    override fun toViper() = NullableDomain.nullVal(elemType.viperType)
}

data class Is(val exp: ExpEmbedding, val comparisonType: TypeEmbedding) : ExpEmbedding {
    override val type = BooleanTypeEmbedding

    override fun toViper() =
        TypeDomain.isSubtype(TypeOfDomain.typeOf(exp.toViper()), comparisonType.runtimeType)
}

data class Cast(val exp: ExpEmbedding, override val type: TypeEmbedding) : ExpEmbedding {
    override fun toViper() = CastingDomain.cast(exp.toViper(), type)
}

data class FieldAccess(val receiver: ExpEmbedding, val field: FieldEmbedding) : ExpEmbedding {
    override val type: TypeEmbedding = field.type
    override fun toViper() = Exp.FieldAccess(receiver.toViper(), field.toViper())
    fun getAccessPredicate(perm: PermExp = PermExp.FullPerm()) = AccessPredicate.FieldAccessPredicate(toViper(), perm)
}