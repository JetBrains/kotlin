/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.buildType
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FirVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.nullableAny
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.names.SetterValueName
import org.jetbrains.kotlin.formver.names.ThisReceiverName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.UserMethod

interface FullNamedFunctionSignature : NamedFunctionSignature {
    /**
     * Preconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPreconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>

    /**
     * Postconditions of function in form of `ExpEmbedding`s with type `boolType()`.
     */
    fun getPostconditions(returnVariable: VariableEmbedding): List<ExpEmbedding>

    val declarationSource: KtSourceElement?
}

/**
 * We generate very reduced methods for getters and setters.
 * They don't have bodies or any invariants.
 * Since after the call to getter, invariants for the result will be inhaled based on the difference
 * between the returned `TypeEmbedding` and expected, we return the broadest possible type here.
 * Types of the arguments don't matter at all, but intuitively they must be `NullableAnyTypeEmbedding` as well.
 */
abstract class PropertyAccessorFunctionSignature(
    override val name: MangledName,
    symbol: FirPropertySymbol
) : FullNamedFunctionSignature {
    override fun getPreconditions(returnVariable: VariableEmbedding) = emptyList<ExpEmbedding>()
    override fun getPostconditions(returnVariable: VariableEmbedding) = emptyList<ExpEmbedding>()
    override val receiver: VariableEmbedding
        get() = PlaceholderVariableEmbedding(ThisReceiverName, buildType { nullableAny() })
    override val declarationSource: KtSourceElement? = symbol.source
}

class GetterFunctionSignature(name: MangledName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {

    override val params = emptyList<FirVariableEmbedding>()
    override val returnType: TypeEmbedding = buildType { nullableAny() }
}

class SetterFunctionSignature(name: MangledName, symbol: FirPropertySymbol) :
    PropertyAccessorFunctionSignature(name, symbol) {
    override val params = listOf(
        FirVariableEmbedding(SetterValueName, buildType { nullableAny() }, symbol)
    )
    override val returnType: TypeEmbedding = buildType { unit() }
}



fun FullNamedFunctionSignature.toViperMethod(
    body: Stmt.Seqn?,
    returnVariable: VariableEmbedding,
) = UserMethod(
    name,
    formalArgs.map { it.toLocalVarDecl() },
    returnVariable.toLocalVarDecl(),
    getPreconditions(returnVariable).pureToViper(toBuiltin = true),
    getPostconditions(returnVariable).pureToViper(toBuiltin = true),
    body,
    declarationSource.asPosition
)