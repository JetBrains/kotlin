/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.MethodEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.Label

class InlineMethodConverter(
    private val programCtx: ProgramConversionContext,
    override val method: MethodEmbedding,
    override val returnVar: VariableEmbedding,
    private val substitutionParams: Map<MangledName, MangledName>,
) : MethodConversionContext, ProgramConversionContext by programCtx {
    override val returnLabel: Label = Label(InlineReturnLabelName(method.name), listOf())

    override fun resolveName(name: MangledName): MangledName =
        when {
            name == ReturnVariableName -> returnVar.name
            else -> substitutionParams[name] ?: InlineName(method.name, name)
        }
}