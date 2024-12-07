/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns [Throws] classIds defined only for current function
 *
 * See [effectiveThrows]
 * See K1: org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.getDefinedThrows
 */
internal fun KaSession.getDefinedThrows(symbol: KaFunctionSymbol): List<ClassId> {
    if (symbol.isSuspend) return listOf(ClassId.topLevel(KonanFqNames.cancellationException))
    if (!symbol.hasThrowsAnnotation) return emptyList()

    val throwsAnnotations = symbol.annotations
        .filter { annotation -> annotation.classId?.asSingleFqName() == KonanFqNames.throws }
        .asSequence()

    return throwsAnnotations
        .flatMap { annotation -> annotation.arguments }
        .map { argument -> argument.expression }
        .filterIsInstance<KaAnnotationValue.ArrayValue>()
        .flatMap { arrayAnnotationValue -> arrayAnnotationValue.values }
        .filterIsInstance<KaAnnotationValue.ClassLiteralValue>()
        .mapNotNull { it.type as? KaClassType }
        .mapNotNull { it.classId }
        .toList()
}
