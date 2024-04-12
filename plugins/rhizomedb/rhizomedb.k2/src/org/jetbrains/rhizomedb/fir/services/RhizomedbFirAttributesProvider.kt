/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.rhizomedb.fir.RhizomedbAttribute
import org.jetbrains.rhizomedb.fir.RhizomedbAttributeKind
import org.jetbrains.rhizomedb.fir.RhizomedbFirPredicates
import org.jetbrains.rhizomedb.fir.hasAnnotation
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbAnnotations

class RhizomedbFirAttributesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirPropertySymbol, RhizomedbAttribute, FirClassSymbol<*>> =
        session.firCachesFactory.createCache(this::createAttributeForProperty)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.selfOrParentAnnotatedWithEntityType)
    }

    fun getAttributeForProperty(propertySymbol: FirPropertySymbol, owner: FirClassSymbol<*>): RhizomedbAttribute {
        return cache.getValue(propertySymbol, owner)
    }

    private fun createAttributeForProperty(propertySymbol: FirPropertySymbol, owner: FirClassSymbol<*>): RhizomedbAttribute {
        val propertyType = propertySymbol.resolvedReturnType
        val (valueKType, kind) = when {
            propertySymbol.hasAnnotation(RhizomedbAnnotations.manyAnnotationClassId, session) -> {
                propertyType.typeArguments.first() as ConeKotlinType to RhizomedbAttributeKind.MANY
            }
            propertyType.isMarkedNullable -> {
                propertyType.withNullability(ConeNullability.NOT_NULL, session.typeContext) to RhizomedbAttributeKind.OPTIONAL
            }
            else -> {
                propertyType to RhizomedbAttributeKind.REQUIRED
            }
        }
        return RhizomedbAttribute(
            propertySymbol.name,
            owner.constructType(emptyArray(), false),
            valueKType,
            kind,
        )
    }
}

val FirSession.attributesProviderProvider: RhizomedbFirAttributesProvider by FirSession.sessionComponentAccessor()
