/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.MethodEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

sealed interface SubstitutionItem {
    val name: MangledName?
    fun lambdaBody(): FirBlock? = null
    fun lambdaArgs(): List<Name>? = null
}

data class SubstitutionName(override val name: MangledName) : SubstitutionItem

data class SubstitutionLambda(val body: FirBlock, val args: List<Name>) : SubstitutionItem {
    override val name = null
    override fun lambdaBody(): FirBlock = body
    override fun lambdaArgs(): List<Name> = args
}

class InlineNameMangler(
    val discriminator: MangledName,
    override val mangledReturnValueName: MangledName,
    val substitutionParams: Map<Name, SubstitutionItem>,
) :
    NameMangler {
    override fun mangleParameterName(parameter: FirValueParameterSymbol): MangledName =
        substitutionParams[parameter.name]?.name ?: throw Exception("Unnamed parameter used in a way that requires a name.")

    override fun mangleLocalPropertyName(property: FirPropertySymbol): MangledName =
        InlineName(discriminator, NoopNameMangler.mangleLocalPropertyName(property))

    override val mangledReturnLabelName: MangledName = InlineName(discriminator, NoopNameMangler.mangledReturnLabelName)
}

class InlineMethodConverter(
    private val programCtx: ProgramConversionContext,
    override val method: MethodEmbedding,
    returnVarName: MangledName,
    private val substitutionParams: Map<Name, SubstitutionItem>,
) : MethodConversionContext, ProgramConversionContext by programCtx {
    override val nameMangler = InlineNameMangler(method.name, returnVarName, substitutionParams)

    override fun getLambdaOrNull(name: Name): SubstitutionLambda? = substitutionParams[name] as? SubstitutionLambda
}