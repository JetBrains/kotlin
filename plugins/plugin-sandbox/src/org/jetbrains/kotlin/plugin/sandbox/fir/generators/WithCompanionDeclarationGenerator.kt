/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.CompanionObjectGenerationContext
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId

object WithCompanionKey : GeneratedDeclarationKey()

class WithCompanionDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val annotationClassId = ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/WithCompanion")
        private val annotationPredicate = LookupPredicate.create { annotated(annotationClassId.asSingleFqName()) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(annotationPredicate)
    }

    override fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: CompanionObjectGenerationContext): GeneratedDeclarationKey? {
        return if (session.predicateBasedProvider.matches(annotationPredicate, classSymbol)) WithCompanionKey else null
    }
}
