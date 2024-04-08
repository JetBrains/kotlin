/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.domains.FunctionBuilder
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.boolType
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.embeddings.FieldEmbedding
import org.jetbrains.kotlin.formver.names.GetterFunctionName
import org.jetbrains.kotlin.formver.names.GetterFunctionSubjectName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Function

/**
 * Function used to unfold a class predicate in order to get the value of a field.
 * The function looks like:
 * ```
 *  function className$get$fieldName(this: Ref): fieldType
 *      requires acc(className(this), write)
 *  {
 *      unfolding acc(className(this), write) in unfoldingBody
 *  }
 *  ```
 * @param unfoldingBody should be one of:
 * - a field access if the field we want to access is declared in the class;
 * - a call to another FieldAccessFunction if the field we want to access is declared in a superclass.
 */
class FieldAccessFunction(
    className: MangledName,
    field: FieldEmbedding,
    unfoldingBody: Exp,
    override val pos: Position = Position.NoPosition,
    override val info: Info = Info.NoInfo,
    override val trafos: Trafos = Trafos.NoTrafos,
) : Function {
    override val name = GetterFunctionName(className, field.name)
    private val subject = Exp.LocalVar(GetterFunctionSubjectName, Type.Ref)
    private val subjectAccess = Exp.PredicateAccess(className, listOf(subject))
    override val retType: Type = Type.Ref
    override val includeInDumpPolicy: IncludeInDumpPolicy = IncludeInDumpPolicy.PREDICATE_DUMP
    override val formalArgs: List<Declaration.LocalVarDecl> = listOf(Declaration.LocalVarDecl(GetterFunctionSubjectName, Type.Ref))
    override val pres: List<Exp> = listOf(subjectAccess)
    override val body: Exp = Exp.Unfolding(subjectAccess, unfoldingBody)
}

object SpecialFunctions {
    val duplicableFunction = FunctionBuilder.build("duplicable") {
        argument { Type.Ref }
        returns { Type.Bool }
    }
    val all = listOf(duplicableFunction) + RuntimeTypeDomain.accompanyingFunctions
}
