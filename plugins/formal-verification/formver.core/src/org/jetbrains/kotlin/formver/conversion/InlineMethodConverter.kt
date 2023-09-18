/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.LambdaExp
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

sealed interface SubstitutionItem {
    val name: MangledName? get() = null
    val lambda: LambdaExp? get() = null
}

data class SubstitutionName(override val name: MangledName) : SubstitutionItem

data class SubstitutionLambda(override val lambda: LambdaExp) : SubstitutionItem

class InlineMethodConverter(
    private val programCtx: ProgramConversionContext,
    override val signature: FullNamedFunctionSignature,
    returnVarName: MangledName,
    private val substitutionParams: Map<Name, SubstitutionItem>,
    scopeDepth: Int
) : MethodConversionContext, ProgramConversionContext by programCtx {
    override val nameMangler = NameMangler(returnVarName, substitutionParams, scopeDepth)

    override fun getLambdaOrNull(name: Name): SubstitutionLambda? = substitutionParams[name] as? SubstitutionLambda
}