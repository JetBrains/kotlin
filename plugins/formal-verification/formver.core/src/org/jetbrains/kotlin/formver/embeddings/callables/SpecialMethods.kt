/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.AnonymousName
import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.conversion.SpecialName
import org.jetbrains.kotlin.formver.embeddings.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Exp.*

object InvokeFunctionObjectMethod : BuiltInMethod(SpecialName("invoke_function_object")) {
    private val thisArg = VariableEmbedding(AnonymousName(0), FunctionTypeEmbedding)
    private val calls = EqCmp(
        Add(Old(thisArg.toFieldAccess(SpecialFields.FunctionObjectCallCounterField)), IntLit(1)),
        thisArg.toFieldAccess(SpecialFields.FunctionObjectCallCounterField)
    )

    override val formalArgs: List<Declaration.LocalVarDecl> = listOf(thisArg.toLocalVarDecl())
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf()
    override val pres: List<Exp> = thisArg.accessInvariants()
    override val posts: List<Exp> = thisArg.accessInvariants() + listOf(calls)
}

object SpecialMethods {
    val all = listOf(InvokeFunctionObjectMethod)
}