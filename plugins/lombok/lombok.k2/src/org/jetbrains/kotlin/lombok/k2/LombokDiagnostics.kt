/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.lombok.k2.LombokCliDiagnostics.LOMBOK_CONFIG_IS_MISSING
import org.jetbrains.kotlin.lombok.k2.LombokCliDiagnostics.LOMBOK_PLUGIN_IS_EXPERIMENTAL
import org.jetbrains.kotlin.lombok.k2.LombokCliDiagnostics.UNKNOWN_PLUGIN_OPTION
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.FLAG_USAGE_ERROR
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.FLAG_USAGE_WARNING
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.LOG_PROPERTY_ALREADY_EXISTS
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.TO_STRING_CALL_SUPER_NOT_CALLED
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.TO_STRING_EXCLUDE_AND_INCLUDE
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics.TO_STRING_FUNCTION_ALREADY_EXISTS
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtExpression
import kotlin.getValue

object LombokCliDiagnostics : KtDiagnosticsContainer() {
    val LOMBOK_PLUGIN_IS_EXPERIMENTAL by warningWithoutSource()
    val LOMBOK_CONFIG_IS_MISSING by warningWithoutSource()
    val UNKNOWN_PLUGIN_OPTION by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = LombokCliDiagnosticsMessages
}

object LombokFirDiagnostics : KtDiagnosticsContainer() {
    val ANNOTATION_IS_NOT_SUPPORTED by warning1<KtAnnotationEntry, Name>()
    val FLAG_USAGE_WARNING by warning1<KtAnnotationEntry, Name>()
    val FLAG_USAGE_ERROR by error1<KtAnnotationEntry, Name>()
    val LOG_PROPERTY_ALREADY_EXISTS by warning1<KtAnnotationEntry, Name>()
    val TO_STRING_FUNCTION_ALREADY_EXISTS by warning0<KtAnnotationEntry>()
    val TO_STRING_CALL_SUPER_NOT_CALLED by warning0<KtAnnotationEntry>()
    val TO_STRING_EXCLUDE_AND_INCLUDE by warning0<KtAnnotationEntry>()
    val TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT by warning0<KtExpression>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = LombokFirDiagnosticsMessages
}

object LombokCliDiagnosticsMessages : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("CLI") { map ->
        map.put(LOMBOK_PLUGIN_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
        map.put(LOMBOK_CONFIG_IS_MISSING, MESSAGE_PLACEHOLDER)
        map.put(UNKNOWN_PLUGIN_OPTION, MESSAGE_PLACEHOLDER)
    }
}

object LombokFirDiagnosticsMessages : BaseDiagnosticRendererFactory() {
    const val FLAG_USAGE_MESSAGE = "Use of any @''{0}'' is flagged according to lombok configuration."
    override val MAP by KtDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(ANNOTATION_IS_NOT_SUPPORTED, "Lombok annotation ''{0}'' is not supported in Kotlin.", CommonRenderers.NAME)
        map.put(FLAG_USAGE_WARNING, FLAG_USAGE_MESSAGE, CommonRenderers.NAME)
        map.put(FLAG_USAGE_ERROR, FLAG_USAGE_MESSAGE, CommonRenderers.NAME)

        map.put(LOG_PROPERTY_ALREADY_EXISTS, "Property ''{0}'' already exists.", CommonRenderers.NAME)
        map.put(TO_STRING_FUNCTION_ALREADY_EXISTS, "Not generating 'toString()': A method with that name already exists.")
        map.put(
            TO_STRING_CALL_SUPER_NOT_CALLED,
            "Generating 'toString' implementation but without a call to superclass, even though this class does not extend 'java.lang.Object'. " +
                    "If this is intentional, add '@ToString(callSuper=false)' to your type."
        )
        map.put(
            TO_STRING_EXCLUDE_AND_INCLUDE,
            "@ToString.Exclude and @ToString.Include are mutually exclusive; the @Include annotation will be ignored."
        )
        map.put(
            TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT,
            "The 'doNotUseGetters' parameter has no effect in Kotlin. " +
                    "Unlike Java, Kotlin properties do not distinguish between direct field access and getter calls."
        )
    }
}
