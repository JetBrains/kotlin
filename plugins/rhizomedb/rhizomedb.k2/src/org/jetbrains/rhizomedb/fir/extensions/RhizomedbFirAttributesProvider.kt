/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.rhizomedb.fir.RhizomedbAnnotations
import org.jetbrains.rhizomedb.fir.RhizomedbAttribute
import org.jetbrains.rhizomedb.fir.RhizomedbAttributeKind
import org.jetbrains.rhizomedb.fir.hasAnnotation

class RhizomedbFirAttributesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirPropertySymbol, RhizomedbAttribute?, Nothing?> =
        session.firCachesFactory.createCache { it, _ -> createAttributeForProperty(it) }

    fun getBackingAttribute(property: FirPropertySymbol): RhizomedbAttribute? {
        return cache.getValue(property, null)
    }

    private fun createAttributeForProperty(property: FirPropertySymbol): RhizomedbAttribute? {
        val klass = property.getContainingClassSymbol() ?: return null
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
