/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

/*
 * Generates `private suspend fun testFun_generated() {}` function
 * for each package containing function annotated with `org.jetbrains.kotlin.fir.plugin.TestTopLevelPrivateSuspendFun`.
 */
@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
internal class TopLevelPrivateSuspendFunctionGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private companion object {
        private val PREDICATE = LookupPredicate.create { annotated("TestTopLevelPrivateSuspendFun".fqn()) }
        private val TEST_FUN_NAME = Name.identifier("testFun_generated")
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedPackageNames by lazy {
        // This could be done more elegantly if TestTopLevelPrivateSuspendFun was file-level annotation,
        // but it's currently not possible to find all annotated files using FirPredicateBasedProvider (see KT-66151).
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirNamedFunctionSymbol>()
            .map { it.callableId.packageName }
            .toSet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context != null) return emptyList()
        if (callableId.callableName != TEST_FUN_NAME) return emptyList()
        val function = createTopLevelFunction(Key, callableId, session.builtinTypes.unitType.type) {
            visibility = Visibilities.Private
            status { isSuspend = true }
        }
        return listOf(function.symbol)
    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return matchedPackageNames.map { CallableId(it, TEST_FUN_NAME) }.toSet()
    }

    object Key : GeneratedDeclarationKey()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
