/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

interface ConvertedOptionalType {
    val viperType: Type?
    fun preconditions(v: Exp): List<Exp> = emptyList()
    fun postconditions(v: Exp): List<Exp> = emptyList()
}

interface ConvertedType : ConvertedOptionalType {
    override val viperType: Type
}

object ConvertedUnit : ConvertedOptionalType {
    override val viperType: Type? = null
}

object ConvertedNothing : ConvertedOptionalType {
    override val viperType: Type? = null

    override fun preconditions(v: Exp): List<Exp> = listOf(Exp.BoolLit(false))
}

object ConvertedInt : ConvertedType {
    override val viperType: Type = Type.Int
}

class ConvertedClassType : ConvertedType {
    override val viperType: Type = Type.Ref

    override fun preconditions(v: Exp): List<Exp> = listOf(Exp.NeCmp(v, Exp.NullLit()))
}
