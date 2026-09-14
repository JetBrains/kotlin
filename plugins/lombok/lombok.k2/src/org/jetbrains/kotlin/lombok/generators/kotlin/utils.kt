/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.lombok.generators.hasJavaOrigin
import org.jetbrains.kotlin.lombok.generators.isSupportedLombokTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

/**
 * Annotations on primary constructor val/var params with @Target(FIELD) end up in the
 * backing field's annotation list, not in property.annotations. We must check both.
 */
fun FirPropertySymbol.findAnnotationOnPropertyOrField(classId: ClassId, session: FirSession): FirAnnotation? =
    getAnnotationByClassId(classId, session) ?: backingFieldSymbol?.getAnnotationByClassId(classId, session)

/**
 * Builds `@JvmStatic` annotation call. If `JvmStatic` symbol can't be found (stdlib is missing), then an error reference is generated.
 */
fun FirCallableSymbol<*>.buildJvmStaticAnnotationCallOrError(session: FirSession): FirAnnotation {
    val jvmStatic = JvmStandardClassIds.Annotations.JvmStatic

    return buildAnnotationCall {
        annotationTypeRef = jvmStatic.constructClassLikeType().toFirResolvedTypeRef()
        calleeReference = session.symbolProvider.getClassLikeSymbolByClassId(jvmStatic)?.let {
            buildResolvedNamedReference {
                this@buildAnnotationCall.source = source
                name = JvmStandardClassIds.Annotations.JvmStatic.shortClassName
                resolvedSymbol = it
            }
        } ?: buildErrorNamedReference {
            this@buildAnnotationCall.source = source
            name = jvmStatic.shortClassName
            diagnostic = ConeUnresolvedSymbolError(jvmStatic)
        }
        containingDeclarationSymbol = this@buildJvmStaticAnnotationCallOrError
    }
}

fun isCompanionNeeded(
    owner: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
): Boolean {
    // Companion objects are only relevant for Kotlin classes
    if (owner.hasJavaOrigin) {
        return false
    }

    // Ignore local classes and anonymous objects to prevent potential exceptions
    if (owner.isLocal) {
        return false
    }

    // Nothing is generated into these, so they must not grow a companion object to hold it either
    if (!owner.isSupportedLombokTarget) {
        return false
    }

    // Check for already existing companion or normal objects
    if (owner.classKind.isObject) {
        return false
    }

    // A companion object of any name rules one out - the members go into that one instead - and so does a nested
    // classifier that merely takes the name `Companion` without being a companion object at all: the generated
    // one would clash with it, and the class was left with a `REDECLARATION` it could not fix short of renaming
    // that classifier (KT-88276). `FirLombokCompanionObjectChecker` reports what is left ungenerated because
    // of it.
    var companionNameIsTaken = false
    context.declaredScope?.processAllClassifiers {
        val classLikeSymbol = it as? FirClassLikeSymbol ?: return@processAllClassifiers
        companionNameIsTaken = companionNameIsTaken ||
                classLikeSymbol.isCompanion ||
                classLikeSymbol.name == DEFAULT_NAME_FOR_COMPANION_OBJECT
    }
    return !companionNameIsTaken
}
