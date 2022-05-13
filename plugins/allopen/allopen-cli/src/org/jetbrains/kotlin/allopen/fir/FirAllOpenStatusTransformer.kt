/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension.Factory
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicate.metaHas
import org.jetbrains.kotlin.fir.extensions.predicate.or
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.FqName

class FirAllOpenStatusTransformer(allOpenAnnotationFqNames: List<String>, session: FirSession) : FirStatusTransformerExtension(session) {
    companion object {
        fun getFactory(allOpenAnnotationFqNames: List<String>): Factory {
            return Factory { session -> FirAllOpenStatusTransformer(allOpenAnnotationFqNames, session) }
        }
    }

    private val annotationFqNames = allOpenAnnotationFqNames.map { FqName(it) }
    private val hasPredicate = has(annotationFqNames) or metaHas(annotationFqNames)

    private val cache: FirCache<FirRegularClassSymbol, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        symbol.annotated()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(hasPredicate)
    }

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return when (declaration) {
            is FirRegularClass -> declaration.classKind == ClassKind.CLASS && cache.getValue(declaration.symbol)
            is FirCallableDeclaration -> {
                val parentClassId = declaration.symbol.callableId.classId ?: return false
                if (parentClassId.isLocal) return false
                val parentClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId) as? FirRegularClassSymbol
                    ?: return false
                cache.getValue(parentClassSymbol)
            }
            else -> false
        }
    }

    private fun FirRegularClassSymbol.annotated(): Boolean {
        if (session.predicateBasedProvider.matches(hasPredicate, this)) return true
        return resolvedSuperTypes.any {
            val superSymbol = it.toRegularClassSymbol(session) ?: return@any false
            cache.getValue(superSymbol)
        }
    }

    override fun transformStatus(status: FirDeclarationStatus, declaration: FirDeclaration): FirDeclarationStatus {
        return if (status.modality == null) {
            status.copy(modality = Modality.OPEN)
        } else {
            status
        }
    }
}
