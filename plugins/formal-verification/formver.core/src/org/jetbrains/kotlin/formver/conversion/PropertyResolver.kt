/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FirVariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.names.embedScopedLocalName
import org.jetbrains.kotlin.name.Name

data class LoopIdentifier(val targetName: String, val index: Int)

/**
 * Resolver for names of local properties.
 *
 * This is a stacked resolver: the resolver for the innermost scope contains a reference
 * to the resolver for the outer scopes, and automatically searches them.
 */
class PropertyResolver(
    private val scopeIndex: Int,
    val parent: PropertyResolver? = null,
    private val loopName: LoopIdentifier? = null,
) {
    private val names: MutableMap<Name, VariableEmbedding> = mutableMapOf()

    fun tryResolveLocalProperty(name: Name): VariableEmbedding? =
        names[name] ?: parent?.tryResolveLocalProperty(name)

    fun registerLocalProperty(symbol: FirPropertySymbol, type: TypeEmbedding) {
        check(symbol.isLocal) { "PropertyResolver must be used with local properties." }
        registerLocal(symbol.name, type, symbol)
    }

    fun registerLocalVariable(symbol: FirVariableSymbol<*>, type: TypeEmbedding) {
        registerLocal(symbol.name, type, symbol)
    }

    private fun registerLocal(name: Name, type: TypeEmbedding, symbol: FirBasedSymbol<*>) {
        names[name] = FirVariableEmbedding(name.embedScopedLocalName(scopeIndex), type, symbol)
    }

    fun innerScope(innerScopeIndex: Int) = PropertyResolver(innerScopeIndex, this)

    fun addLoopIdentifier(labelName: String, index: Int) = PropertyResolver(scopeIndex, parent, LoopIdentifier(labelName, index))

    fun tryResolveLoopName(name: String): Int? =
        if (loopName?.targetName == name) loopName.index
        else parent?.tryResolveLoopName(name)
}