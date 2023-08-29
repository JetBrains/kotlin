/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

class VariableEmbedding(val name: MangledName, val type: TypeEmbedding) {

    val viperType = type.type

    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Declaration.LocalVarDecl = Declaration.LocalVarDecl(name, type.type, pos, info, trafos)

    fun toLocalVar(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name, type.type, pos, info, trafos)

    fun toField(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Field = Field(name, type.type, pos, info, trafos)

    fun invariants(): List<Exp> = type.invariants(toLocalVar())
    fun dynamicInvariants(): List<Exp> = type.dynamicInvariants(toLocalVar())
}