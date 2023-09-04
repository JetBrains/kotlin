/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.formver.conversion.ContractDescriptionConversionVisitor
import org.jetbrains.kotlin.formver.conversion.ReturnVariableName
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * This embedding represents a method signature.
 * In case the method has a receiver it becomes the first argument of the function.
 * Example: Foo.bar(x: Int) --> Foo$bar(this: Foo, x: Int)
 */
class MethodSignatureEmbedding(
    symbol: FirFunctionSymbol<*>,
    val receiver: VariableEmbedding?,
    val params: List<VariableEmbedding>,
    val returnType: TypeEmbedding,
) {
    val name = symbol.callableId.embedName()

    val returnVar = VariableEmbedding(ReturnVariableName, returnType)

    val formalArgs: List<VariableEmbedding> = listOfNotNull(receiver) + params

    val preconditions = formalArgs.flatMap { it.invariants() }

    private val contractPostconditions = symbol.resolvedContractDescription?.effects?.map {
        it.effect.accept(ContractDescriptionConversionVisitor, this)
    } ?: emptyList()

    val postconditions =
        params.flatMap { it.invariants() } + params.flatMap { it.dynamicInvariants() } + returnVar.invariants() + contractPostconditions

    fun toMethod(
        body: Stmt.Seqn?,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = UserMethod(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        returnVar.toLocalVarDecl(),
        preconditions,
        postconditions,
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