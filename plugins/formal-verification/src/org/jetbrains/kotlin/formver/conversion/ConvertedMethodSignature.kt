/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import viper.silver.ast.Method

class ConvertedMethodSignature(val name: ConvertedName, val params: List<ConvertedVar>, val returnType: ConvertedType) {
    val returnVar: ConvertedVar
        get() = ConvertedVar(ReturnVariableName, returnType)

    fun toMethod(
        pres: List<Exp>, posts: List<Exp>,
        body: Stmt.Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Method {
        val returns = listOf(returnVar)
        return method(
            name.asString,
            params.map { it.toLocalVarDecl() },
            returns.map { it.toLocalVarDecl() },
            params.flatMap { it.invariants() } + pres,
            params.flatMap { it.invariants() } +
                    returns.flatMap { it.invariants() } + posts,
            body, pos, info, trafos,
        )
    }
}