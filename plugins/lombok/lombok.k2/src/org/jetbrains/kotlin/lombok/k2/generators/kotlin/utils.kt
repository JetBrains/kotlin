/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators.kotlin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds

val FirCallableSymbol<*>.isRelevantForConflictsCheck: Boolean
    get() = !isExtension && !hasContextParameters

/**
 * Annotations on primary constructor val/var params with @Target(FIELD) end up in the
 * backing field's annotation list, not in property.annotations. We must check both.
 */
fun FirPropertySymbol.findAnnotationOnPropertyOrField(classId: ClassId, session: FirSession): FirAnnotation? =
    getAnnotationByClassId(classId, session) ?: backingFieldSymbol?.getAnnotationByClassId(classId, session)

/**
 * Builds `@JvmStatic` annotation call and returns null if it cannot be resolved (for instance, if stdlib is missing).
 */
fun FirCallableSymbol<*>.tryBuildingJvmStaticAnnotationCall(session: FirSession): FirAnnotation? {
    return buildAnnotationCall {
        annotationTypeRef =
            JvmStandardClassIds.Annotations.JvmStatic.constructClassLikeType().toFirResolvedTypeRef()
        calleeReference = buildResolvedNamedReference {
            name = JvmStandardClassIds.Annotations.JvmStatic.shortClassName
            resolvedSymbol =
                session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Annotations.JvmStatic)
                    ?: return null
        }
        containingDeclarationSymbol = this@tryBuildingJvmStaticAnnotationCall
    }
}

fun FirExtension.initializeCompanionObjectIfNeeded(
    owner: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
    extractKey: () -> GeneratedDeclarationKey?,
): FirRegularClassSymbol? {
    // Ignore local classes and anonymous objects to prevent potential exceptions
    if (owner.isLocal) {
        return null
    }

    // Check for already existing companion or normal objects
    if (owner.classKind.isObject) {
        return null
    }

    var companionAlreadyExists = false
    context.declaredScope?.processAllClassifiers {
        companionAlreadyExists = companionAlreadyExists || (it as? FirClassLikeSymbol)?.isCompanion == true
    }
    if (companionAlreadyExists) {
        return null
    }

    val key = extractKey() ?: return null

    return createCompanionObject(owner, key).symbol
}
