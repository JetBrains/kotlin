/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirDeclarationsForMetadataProviderExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope

class AllPropertiesConstructorMetadataProvider(session: FirSession) : FirDeclarationsForMetadataProviderExtension(session) {
    companion object {
        val ANNOTATION_FQN = "AllPropertiesConstructor".fqn()
        val PREDICATE = DeclarationPredicate.create { annotated(ANNOTATION_FQN) }
    }

    private object Key : GeneratedDeclarationKey()

    override fun provideDeclarationsForClass(klass: FirClass, scopeSession: ScopeSession): List<FirDeclaration> {
        if (!session.predicateBasedProvider.matches(PREDICATE, klass)) return emptyList()
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
        val properties = scope.getCallableNames().flatMap { scope.getProperties(it) }
        val constructor = createConstructor(klass.symbol, Key) {
            for (property in properties) {
                valueParameter(property.name, property.resolvedReturnType)
            }
        }
        return listOf(constructor)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
