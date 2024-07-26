/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.names.FunctionKotlinName
import org.jetbrains.kotlin.formver.names.ScopedKotlinName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.Position
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.Trafos

interface NamedFunctionSignature : FunctionSignature {
    val name: MangledName
    override val sourceName: String?
        get() = when (val signatureName = name) {
            is FunctionKotlinName -> signatureName.name.asString()
            is ScopedKotlinName -> (signatureName.name as? FunctionKotlinName)?.name?.asString()
            else -> null
        }
}

fun NamedFunctionSignature.toMethodCall(
    parameters: List<ExpEmbedding>,
    targetVar: VariableEmbedding,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) = Stmt.MethodCall(name, parameters.pureToViper(), listOf(targetVar.toLocalVarUse()), pos, info, trafos)
