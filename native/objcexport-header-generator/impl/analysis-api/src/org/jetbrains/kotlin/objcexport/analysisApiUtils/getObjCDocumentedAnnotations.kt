/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.annotations.annotationClassIds
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Returns the list of annotations which shall be documented for ObjC export.
 * Usually annotations that are marked with `@MustBeDocumented` are important and shall be retained for users.
 * However, some annotations e.g. [kotlin.native.ObjCName], whilst annotated with `@MustBeDocumented` shall not be exported for ObjC
 * documentation.
 */
context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.getObjCDocumentedAnnotations(): List<KtAnnotationApplicationWithArgumentsInfo> {
    return annotationsList.getObjCDocumentedAnnotations()
}

/**
 * See [getObjCDocumentedAnnotations]
 */
context(KtAnalysisSession)
internal fun KtAnnotationsList.getObjCDocumentedAnnotations(): List<KtAnnotationApplicationWithArgumentsInfo> {
    return annotations
        .filter { annotation ->
            val annotationClassId = annotation.classId ?: return@filter false
            if (annotationClassId.asSingleFqName() in mustBeDocumentedAnnotationsStopList) return@filter false
            val annotationClassSymbol = getClassOrObjectSymbolByClassId(annotationClassId) ?: return@filter false
            StandardClassIds.Annotations.MustBeDocumented in annotationClassSymbol.annotationClassIds
        }
}

private val mustBeDocumentedAnnotationsStopList = setOf(
    StandardNames.FqNames.deprecated,
    KonanFqNames.objCName,
    KonanFqNames.shouldRefineInSwift
)