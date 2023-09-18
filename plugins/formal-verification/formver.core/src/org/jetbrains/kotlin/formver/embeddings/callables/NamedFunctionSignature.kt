/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.ReturnVariableName
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.toViper
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.Trafos

interface NamedFunctionSignature : CallableSignature {
    val name: MangledName
    val receiver: VariableEmbedding?
    val params: List<VariableEmbedding>

    val returnVar
        get() = VariableEmbedding(ReturnVariableName, returnType)
    val formalArgs: List<VariableEmbedding>
        get() = listOfNotNull(receiver) + params

    override val receiverType: TypeEmbedding?
        get() = receiver?.type
    override val paramTypes: List<TypeEmbedding>
        get() = params.map { it.type }
}

fun NamedFunctionSignature.toMethodCall(
    parameters: List<ExpEmbedding>,
    targetVar: VariableEmbedding,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) = Stmt.MethodCall(name, parameters.toViper(), listOf(targetVar.toViper()), pos, info, trafos)
