/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import viper.silver.ast.LocalVarDecl

class VariableEmbedding(val name: MangledName, val type: TypeEmbedding) {
    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): LocalVarDecl = localVarDecl(name.mangled, type.type, pos, info, trafos)

    fun toLocalVar(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name.mangled, type.type, pos, info, trafos)

    fun invariants(): List<Exp> = type.invariants(toLocalVar())
}