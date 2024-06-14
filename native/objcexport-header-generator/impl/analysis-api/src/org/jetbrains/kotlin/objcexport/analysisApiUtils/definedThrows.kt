package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns [Throws] classIds defined only for current function
 *
 * See [effectiveThrows]
 * See K1: org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.getDefinedThrows
 */
context(KaSession)
internal val KaFunctionLikeSymbol.definedThrows: List<ClassId>
    get() {
        if (isSuspend) return listOf(ClassId.topLevel(KonanFqNames.cancellationException))
        if (!hasThrowsAnnotation) return emptyList()

        val throwsAnnotations = annotations
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