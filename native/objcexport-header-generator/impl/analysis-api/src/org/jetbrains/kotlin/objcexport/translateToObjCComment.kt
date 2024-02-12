/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridge
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.plus
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getObjCDocumentedAnnotations


context(KtAnalysisSession)
internal fun KtAnnotationsList.translateToObjCComment(): ObjCComment? {
    val annotations = getObjCDocumentedAnnotations()
        .mapNotNull { annotation -> renderAnnotation(annotation) }

    if (annotations.isEmpty()) return null
    return ObjCComment(listOf("@note annotations") + annotations.map { "  $it" })
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildComment]
 */
context(KtAnalysisSession)
internal fun KtFunctionLikeSymbol.translateToObjCComment(bridge: MethodBridge, parameters: List<ObjCParameter>): ObjCComment? {
    val isSuspend: Boolean = if (this is KtFunctionSymbol) this.isSuspend else false

    val throwsComments = if (isSuspend || bridge.returnsError) {
        val effectiveThrows = getEffectiveThrows(this).toSet()
        when {
            effectiveThrows.contains(StandardClassIds.Throwable) -> {
                listOf("@note This method converts all Kotlin exceptions to errors.")
            }

            effectiveThrows.isNotEmpty() -> {
                listOf(
                    buildString {
                        append("@note This method converts instances of ")
                        effectiveThrows.joinTo(this) { it.relativeClassName.asString() }
                        append(" to errors.")
                    },
                    "Other uncaught Kotlin exceptions are fatal."
                )
            }

            else -> {
                // Shouldn't happen though.
                listOf("@warning All uncaught Kotlin exceptions are fatal.")
            }
        }
    } else emptyList()

    val visibilityComments = buildObjCVisibilityComment("method")

    val paramComments = valueParameters.mapNotNull { parameterSymbol ->
        parameters.find { parameter -> parameter.origin?.name == parameterSymbol.name }
            ?.renderedObjCDocumentedParamAnnotations(parameterSymbol)
    }
    val annotationsComments = annotationsList.translateToObjCComment()
    return annotationsComments + paramComments + throwsComments + visibilityComments
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mustBeDocumentedParamAttributeList]
 */
context(KtAnalysisSession)
private fun ObjCParameter.renderedObjCDocumentedParamAnnotations(parameterSymbol: KtValueParameterSymbol): String? {
    val renderedAnnotationsString = parameterSymbol.getObjCDocumentedAnnotations()
        .mapNotNull { annotation -> renderAnnotation(annotation) }
        .ifEmpty { return null }
        .joinToString(" ")
    return "@param $name annotations $renderedAnnotationsString"
}


private fun renderAnnotation(annotation: KtAnnotationApplicationWithArgumentsInfo): String? {
    return renderAnnotation(annotation.classId ?: return null, annotation.arguments)
}

private fun renderAnnotation(clazz: ClassId, arguments: List<KtNamedAnnotationValue>): String {
    return buildString {
        append(clazz.asSingleFqName())
        if (arguments.isNotEmpty()) {
            append('(')
            arguments.joinTo(this) { arg -> arg.render() }
            append(')')
        }
    }
}

private fun KtNamedAnnotationValue.render(): String {
    return "$name=${expression.renderAsSourceCode()}"
}

/**
 * Not implemented [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.getEffectiveThrows]
 */
@Suppress("UNUSED_PARAMETER")
private fun getEffectiveThrows(method: KtFunctionLikeSymbol): Sequence<ClassId> {
    return emptySequence()
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.visibilityComments]
 */
private fun KtSymbol.buildObjCVisibilityComment(kind: String): ObjCComment? {
    return when ((this as? KtSymbolWithVisibility)?.visibility ?: return null) {
        Visibilities.Protected -> ObjCComment("@note This $kind has protected visibility in Kotlin source and is intended only for use by subclasses.")
        else -> null
    }
}