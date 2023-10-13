/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.LegacyUnspecifiedFunctionTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.names.AnonymousName
import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.BuiltinFunction
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Type

object DuplicableFunction : BuiltinFunction(SpecialName("duplicable")) {
    private val thisArg = VariableEmbedding(AnonymousName(0), LegacyUnspecifiedFunctionTypeEmbedding)

    override val formalArgs: List<Declaration.LocalVarDecl> = listOf(thisArg.toLocalVarDecl())
    override val retType: Type = Type.Bool
}

object SpecialFunctions {
    val all = listOf(DuplicableFunction)
}