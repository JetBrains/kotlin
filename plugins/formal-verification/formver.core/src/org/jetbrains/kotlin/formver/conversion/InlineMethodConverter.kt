/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.formver.embeddings.LocalName
import org.jetbrains.kotlin.formver.embeddings.MethodEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label

sealed interface SubstitutionItem {
    val name: MangledName?
    fun lambdaBody(): FirBlock? = null
    fun lambdaArgs(): List<LocalName>? = null
}

data class SubstitutionName(override val name: MangledName) : SubstitutionItem

data class SubstitutionLambda(val body: FirBlock, val args: List<LocalName>) : SubstitutionItem {
    override val name = null
    override fun lambdaBody(): FirBlock = body
    override fun lambdaArgs(): List<LocalName> = args
}

class InlineMethodConverter(
    private val programCtx: ProgramConversionContext,
    override val method: MethodEmbedding,
    override val returnVar: VariableEmbedding,
    private val substitutionParams: Map<MangledName, SubstitutionItem>,
) : MethodConversionContext, ProgramConversionContext by programCtx {
    override val returnLabel: Label = Label(InlineReturnLabelName(method.name), listOf())

    override fun resolveName(name: MangledName): MangledName {
        val sub = substitutionParams[name]
        return when {
            name == ReturnVariableName -> returnVar.name
            else -> sub?.name ?: InlineName(method.name, name)
        }
    }

    override fun getLambdaOrNull(name: MangledName): SubstitutionLambda? = substitutionParams[name] as? SubstitutionLambda
}