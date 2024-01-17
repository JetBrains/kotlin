/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir.services

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlinx.jspo.compiler.fir.JsPlainObjectsPredicates
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsAnnotations

class JsPlainObjectsPropertiesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirClassSymbol<*>, List<FirPropertySymbol>, Nothing?> =
        session.firCachesFactory.createCache(this::createJsPlainObjectProperties)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(JsPlainObjectsPredicates.AnnotatedWithJsPlainObject.DECLARATION)
    }

    fun getJsPlainObjectsPropertiesForClass(classSymbol: FirClassSymbol<*>): List<FirPropertySymbol> {
        return cache.getValue(classSymbol)
    }

    private fun createJsPlainObjectProperties(classSymbol: FirClassSymbol<*>): List<FirPropertySymbol> =
        if (!classSymbol.hasAnnotation(JsPlainObjectsAnnotations.jsPlainObjectAnnotationClassId, session)) {
            emptyList()
        } else {
            buildList {
                classSymbol.resolvedSuperTypes.forEach {
                    val superInterface = it.fullyExpandedType(session)
                        .toRegularClassSymbol(session)
                        ?.takeIf { it.classKind == ClassKind.INTERFACE } ?: return@forEach

                    val superInterfaceSimpleObjectProperties = createJsPlainObjectProperties(superInterface)
                    superInterfaceSimpleObjectProperties.forEach(::addIfNotNull)
                }

                classSymbol
                    .declaredMemberScope(session, null)
                    .processAllProperties {
                        addIfNotNull(it.takeIf { it.visibility == Visibilities.Public } as? FirPropertySymbol)
                    }
            }
        }
}

val FirSession.jsPlainObjectPropertiesProvider: JsPlainObjectsPropertiesProvider by FirSession.sessionComponentAccessor()