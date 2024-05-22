/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId


fun FirBasedSymbol<*>.hasAnnotation(classId: ClassId, session: FirSession): Boolean {
    return resolvedCompilerAnnotationsWithClassIds.getAnnotationByClassId(classId, session) != null
}

fun FirAnnotationContainer.getEntityTypeAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(RhizomedbAnnotations.generatedEntityTypeClassId, session)
}

fun FirAnnotationContainer.getManyAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(RhizomedbAnnotations.manyAnnotationClassId, session)
}

val FirClassSymbol<*>.declarationProperties: List<FirPropertySymbol>
    get() = declarationSymbols.filterIsInstance<FirPropertySymbol>()

fun FirClassLikeSymbol<*>.getContainingClass(session: FirSession): FirClassSymbol<*>? =
    getContainingDeclaration(session) as? FirClassSymbol
