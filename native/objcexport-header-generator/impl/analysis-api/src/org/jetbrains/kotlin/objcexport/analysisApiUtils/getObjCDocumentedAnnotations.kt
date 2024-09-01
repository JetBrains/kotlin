/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Returns the list of annotations which shall be documented for ObjC export.
 * Usually annotations that are marked with `@MustBeDocumented` are important and shall be retained for users.
 * However, some annotations e.g. [kotlin.native.ObjCName], whilst annotated with `@MustBeDocumented` shall not be exported for ObjC
 * documentation.
 */
internal fun KaSession.getObjCDocumentedAnnotations(symbol: KaAnnotatedSymbol): List<KaAnnotation> {
    return getObjCDocumentedAnnotations(symbol.annotations)
}

/**
 * See [getObjCDocumentedAnnotations]
 */
internal fun KaSession.getObjCDocumentedAnnotations(list: KaAnnotationList): List<KaAnnotation> {
    return list
        .filter { annotation ->
            val annotationClassId = annotation.classId ?: return@filter false
            if (annotationClassId.asSingleFqName() in mustBeDocumentedAnnotationsStopList) return@filter false
            val annotationClassSymbol = findClass(annotationClassId) ?: return@filter false
            StandardClassIds.Annotations.MustBeDocumented in annotationClassSymbol.annotations.classIds
        }
}

private val mustBeDocumentedAnnotationsStopList = setOf(
    StandardNames.FqNames.deprecated,
    KonanFqNames.objCName,
    KonanFqNames.shouldRefineInSwift
)