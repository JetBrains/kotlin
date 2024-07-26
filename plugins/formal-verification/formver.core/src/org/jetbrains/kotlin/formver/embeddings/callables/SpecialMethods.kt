/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.SpecialFields
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.names.AnonymousName
import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.ast.BuiltInMethod
import org.jetbrains.kotlin.formver.viper.ast.Declaration
import org.jetbrains.kotlin.formver.viper.ast.Exp

object InvokeFunctionObjectMethod : BuiltInMethod(SpecialName("invoke_function_object")) {
    private val thisArg = VariableEmbedding(AnonymousName(0), LegacyUnspecifiedFunctionTypeEmbedding)
    private val calls = EqCmp(
        Add(Old(FieldAccess(thisArg, SpecialFields.FunctionObjectCallCounterField)), IntLit(1)),
        FieldAccess(thisArg, SpecialFields.FunctionObjectCallCounterField)
    )

    override val formalArgs: List<Declaration.LocalVarDecl> = listOf(thisArg.toLocalVarDecl())
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf()
    override val pres: List<Exp> = thisArg.accessInvariants().toViper()
    override val posts: List<Exp> = (thisArg.accessInvariants() + listOf(calls)).toViper()
}

object SpecialMethods {
    val all = listOf(InvokeFunctionObjectMethod)
}