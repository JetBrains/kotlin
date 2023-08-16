/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

interface ConvertedType {
    val viperType: Type
    fun invariants(v: Exp): List<Exp> = emptyList()
}

object ConvertedUnit : ConvertedType {
    override val viperType: Type = UnitDomain.toType()
}

object ConvertedNothing : ConvertedType {
    override val viperType: Type = UnitDomain.toType()

    override fun invariants(v: Exp): List<Exp> = listOf(Exp.BoolLit(false))
}

object ConvertedInt : ConvertedType {
    override val viperType: Type = Type.Int
}

object ConvertedBoolean : ConvertedType {
    override val viperType: Type = Type.Bool
}

class ConvertedClassType : ConvertedType {
    override val viperType: Type = Type.Ref

    override fun invariants(v: Exp): List<Exp> = listOf(Exp.NeCmp(v, Exp.NullLit()))
}

class ConvertedTypeVar(val name: String) : ConvertedType {
    override val viperType: Type = Type.TypeVar(name)
}

class ConvertedNullable(val elementType: ConvertedType) : ConvertedType {
    override val viperType: Type = NullableDomain.toType(mapOf(NullableDomain.T to elementType.viperType))
}
