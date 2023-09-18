/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.viper.ast.*

interface FullNamedFunctionSignature : NamedFunctionSignature {
    val preconditions: List<Exp>
    val postconditions: List<Exp>
}

fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    position: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVar.toLocalVarDecl(),
    preconditions,
    postconditions,
    body,
    position,
    info,
    trafos,
)