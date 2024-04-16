package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtArrayAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtKClassAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns [Throws] classIds defined only for current function
 *
 * See [effectiveThrows]
 * See K1: org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.getDefinedThrows
 */
context(KtAnalysisSession)
internal val KtFunctionLikeSymbol.definedThrows: List<ClassId>
    get() {
        if (isSuspend) return listOf(ClassId.topLevel(KonanFqNames.cancellationException))
        if (!hasThrowsAnnotation) return emptyList()

        val throwsAnnotations = annotationsList.annotations
            .filter { annotation -> annotation.classId?.asSingleFqName() == KonanFqNames.throws }
            .asSequence()

        return throwsAnnotations
            .flatMap { annotation -> annotation.arguments }
            .map { argument -> argument.expression }
            .filterIsInstance<KtArrayAnnotationValue>()
            .flatMap { arrayAnnotationValue -> arrayAnnotationValue.values }
            .filterIsInstance<KtKClassAnnotationValue>()
            .mapNotNull { it.type as? KtNonErrorClassType }
            .mapNotNull { it.classId }
            .toList()
    }