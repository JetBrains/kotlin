/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ReturnVariableName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * This embedding represents a method signature.
 * In case the method has a receiver it becomes the first argument of the function.
 * Example: Foo.bar(x: Int) --> Foo$bar(this: Foo, x: Int)
 */
class MethodSignatureEmbedding(
    val name: MangledName,
    val receiver: VariableEmbedding?,
    val params: List<VariableEmbedding>,
    val returnType: TypeEmbedding,
) {
    val returnVar = VariableEmbedding(ReturnVariableName, returnType)

    val formalArgs: List<VariableEmbedding> = listOfNotNull(receiver) + params

    fun toMethod(
        pres: List<Exp>, posts: List<Exp>,
        body: Stmt.Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = UserMethod(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        returnVar.toLocalVarDecl(),
        formalArgs.flatMap { it.invariants() } + pres,
        formalArgs.flatMap { it.invariants() } +
                formalArgs.flatMap { it.dynamicInvariants() } +
                returnVar.invariants() + posts,
        body, pos, info, trafos,
    )

    fun toMethodCall(
        parameters: List<Exp>,
        targetVar: VariableEmbedding,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Stmt.MethodCall(name, parameters, listOf(targetVar.toLocalVar()), pos, info, trafos)
}