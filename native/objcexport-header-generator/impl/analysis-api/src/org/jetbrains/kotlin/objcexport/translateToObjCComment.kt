/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.renderAsSourceCode
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.MethodBridge
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.backend.konan.objcexport.plus
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getEffectiveThrows
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getObjCDocumentedAnnotations
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isSuspend

internal fun KaSession.translateToObjCComment(list: KaAnnotationList): ObjCComment? {
    val annotations = getObjCDocumentedAnnotations(list)
        .mapNotNull { annotation -> renderAnnotation(annotation) }

    if (annotations.isEmpty()) return null
    return ObjCComment(listOf("@note annotations") + annotations.map { "  $it" })
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildComment]
 */
internal fun KaSession.translateToObjCComment(
    function: KaFunctionSymbol,
    bridge: MethodBridge,
    parameters: List<ObjCParameter>,
): ObjCComment? {
    val throwsComments = if (function.isSuspend || bridge.returnsError) {
        val effectiveThrows = getEffectiveThrows(function).toSet()
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

    val visibilityComments = function.buildObjCVisibilityComment("method")

    val paramComments = function.valueParameters.mapNotNull { parameterSymbol ->
        val param = parameters.find { parameter -> parameter.name == parameterSymbol.name.asString() }
        param?.let { renderedObjCDocumentedParamAnnotations(it, parameterSymbol) }
    }
    val annotationsComments = translateToObjCComment(function.annotations)
    return annotationsComments + paramComments + throwsComments + visibilityComments
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mustBeDocumentedParamAttributeList]
 */
private fun KaSession.renderedObjCDocumentedParamAnnotations(parameter: ObjCParameter, parameterSymbol: KaValueParameterSymbol): String? {
    val renderedAnnotationsString = getObjCDocumentedAnnotations(parameterSymbol)
        .mapNotNull { annotation -> renderAnnotation(annotation) }
        .ifEmpty { return null }
        .joinToString(" ")
    return "@param ${parameter.name} annotations $renderedAnnotationsString"
}


private fun renderAnnotation(annotation: KaAnnotation): String? {
    return renderAnnotation(annotation.classId ?: return null, annotation.arguments)
}

private fun renderAnnotation(clazz: ClassId, arguments: List<KaNamedAnnotationValue>): String {
    return buildString {
        append(clazz.asSingleFqName())
        if (arguments.isNotEmpty()) {
            append('(')
            arguments.joinTo(this) { arg -> arg.render() }
            append(')')
        }
    }
}

private fun KaNamedAnnotationValue.render(): String {
    return "$name=${expression.renderAsSourceCode()}"
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.visibilityComments]
 */
private fun KaSymbol.buildObjCVisibilityComment(kind: String): ObjCComment? = when ((this as? KaDeclarationSymbol)?.visibility) {
    KaSymbolVisibility.PROTECTED -> ObjCComment("@note This $kind has protected visibility in Kotlin source and is intended only for use by subclasses.")
    else -> null
}
