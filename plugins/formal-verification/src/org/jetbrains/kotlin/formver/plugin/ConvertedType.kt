/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type

interface ConvertedType {
    val viperType: Type?
    fun preconditions(v: Exp.LocalVar): List<Exp>
    fun postconditions(v: Exp.LocalVar): List<Exp>
}

interface ConvertedNonUnitType : ConvertedType {
    override val viperType: Type
}

abstract class ConvertedPrimitive : ConvertedNonUnitType {
    override fun preconditions(v: Exp.LocalVar): List<Exp> = emptyList()
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}

class ConvertedUnit : ConvertedType {
    override val viperType: Type? = null
    override fun preconditions(v: Exp.LocalVar): List<Exp> = emptyList()
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}

class ConvertedInt : ConvertedPrimitive() {
    override val viperType: Type = Type.Int
}

class ConvertedClassType : ConvertedNonUnitType {
    override val viperType: Type = Type.Ref

    override fun preconditions(v: Exp.LocalVar): List<Exp> = listOf(Exp.NeCmp(v, Exp.NullLit()))
    override fun postconditions(v: Exp.LocalVar): List<Exp> = emptyList()
}
