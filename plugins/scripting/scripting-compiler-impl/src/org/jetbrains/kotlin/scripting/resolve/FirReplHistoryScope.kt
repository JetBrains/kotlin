/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirReplHistoryScope(
    val properties: Map<Name, FirVariableSymbol<*>>,
    val functions: Map<Name, List<FirNamedFunctionSymbol>>,
    val classLikes: Map<Name, FirClassLikeSymbol<*>>,
    val useSiteSession: FirSession,
) : FirContainingNamesAwareScope() {

    override fun collectFunctionsByName(name: Name): List<FirNamedFunctionSymbol> {
        return functions[name].orEmpty()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val klass = classLikes[name]
        if (klass != null) {
            val substitution = klass.typeParameterSymbols.associateWith { it.toConeType() }
            processor(klass, substitutorByMap(substitution, useSiteSession, allowIdenticalSubstitution = true))
        }
    }

    override fun mayContainName(name: Name): Boolean {
        return properties.containsKey(name) || functions[name]?.isNotEmpty() == true || classLikes.containsKey(name)
    }

    override fun getCallableNames(): Set<Name> = properties.keys + functions.keys
    override fun getClassifierNames(): Set<Name> = classLikes.keys

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirLocalScope? {
        return null
    }
}
