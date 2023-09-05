/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName

interface NameResolutionContext {
    fun resolveName(name: MangledName): MangledName
}

class SimpleNameResolver : NameResolutionContext {
    override fun resolveName(name: MangledName) = name
}

class InlineCallNameResolver(
    private val inlineFunctionName: MangledName,
    val resultVar: VariableEmbedding,
    private val substitutionParams: Map<MangledName, MangledName>
) :
    NameResolutionContext {
    override fun resolveName(name: MangledName): MangledName =
        when {
            name == ReturnVariableName -> resultVar.name
            else -> substitutionParams[name] ?: InlineName(inlineFunctionName, name)
        }
}