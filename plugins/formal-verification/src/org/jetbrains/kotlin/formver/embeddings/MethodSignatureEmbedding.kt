/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import viper.silver.ast.Method

class MethodSignatureEmbedding(val name: MangledName, val params: List<VariableEmbedding>, val returnType: TypeEmbedding) {
    val returnVar: VariableEmbedding
        get() = VariableEmbedding(ReturnVariableName, returnType)

    fun toMethod(
        pres: List<Exp>, posts: List<Exp>,
        body: Stmt.Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Method {
        val returns = listOf(returnVar)
        return method(
            name.mangled,
            params.map { it.toLocalVarDecl() },
            returns.map { it.toLocalVarDecl() },
            params.flatMap { it.invariants() } + pres,
            params.flatMap { it.invariants() } +
                    returns.flatMap { it.invariants() } + posts,
            body, pos, info, trafos,
        )
    }
}