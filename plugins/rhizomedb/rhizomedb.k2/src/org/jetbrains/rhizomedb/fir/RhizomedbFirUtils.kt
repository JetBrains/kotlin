/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.rhizomedb.fir.resolve.RhizomedbAnnotations


fun FirClassSymbol<*>.hasEntityTypeAnnotation(session: FirSession): Boolean {
    return entityTypeAnnotation(session) != null
}

fun FirBasedSymbol<*>.hasAttributeAnnotation(session: FirSession): Boolean {
    return attributeAnnotation(session) != null
}

fun FirBasedSymbol<*>.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return resolvedCompilerAnnotationsWithClassIds.getAnnotationByClassId(classId, session) != null
}

fun FirBasedSymbol<*>.entityTypeAnnotation(session: FirSession): FirAnnotation? {
    return resolvedCompilerAnnotationsWithClassIds.entityTypeAnnotation(session)
}

fun FirBasedSymbol<*>.attributeAnnotation(session: FirSession): FirAnnotation? {
    return resolvedCompilerAnnotationsWithClassIds.attributeAnnotation(session)
}

fun List<FirAnnotation>.entityTypeAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(RhizomedbAnnotations.generatedEntityTypeAnnotationClassId, session)
}

fun List<FirAnnotation>.attributeAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(RhizomedbAnnotations.attributeAnnotationClassId, session)
}

// ---------------------- class utils ----------------------
internal fun FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion(session: FirSession): Boolean =
    classKind == ClassKind.CLASS && hasEntityTypeAnnotation(session)
