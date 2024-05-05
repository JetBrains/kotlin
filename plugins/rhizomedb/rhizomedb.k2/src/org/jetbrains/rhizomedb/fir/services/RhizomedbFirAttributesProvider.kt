/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.rhizomedb.fir.RhizomedbAttribute
import org.jetbrains.rhizomedb.fir.RhizomedbAttributeKind
import org.jetbrains.rhizomedb.fir.RhizomedbFirPredicates
import org.jetbrains.rhizomedb.fir.hasAnnotation
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbAnnotations

class RhizomedbFirAttributesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirPropertySymbol, RhizomedbAttribute?, Nothing?> =
        session.firCachesFactory.createCache { it, _ -> createAttributeForProperty(it) }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(RhizomedbFirPredicates.selfOrParentAnnotatedWithEntityType)
    }

    fun getBackingAttribute(property: FirPropertySymbol): RhizomedbAttribute? {
        return cache.getValue(property, null)
    }

    private fun createAttributeForProperty(property: FirPropertySymbol): RhizomedbAttribute? {
        val klass = property.getContainingClassSymbol(session) ?: return null
        val propertyType = property.resolvedReturnType
        val (valueKType, kind) = when {
            property.hasAnnotation(RhizomedbAnnotations.manyAnnotationClassId, session) -> {
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
            property.name,
            klass.constructType(emptyArray(), false),
            valueKType,
            kind,
        )
    }
}

val FirSession.attributesProvider: RhizomedbFirAttributesProvider by FirSession.sessionComponentAccessor()
