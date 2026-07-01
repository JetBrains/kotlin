/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.diagnostics.warning2
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.KOTLIN_TARGETS
import org.jetbrains.kotlin.lombok.LombokCliDiagnostics.LOMBOK_CONFIG_IS_MISSING
import org.jetbrains.kotlin.lombok.LombokCliDiagnostics.LOMBOK_PLUGIN_IS_EXPERIMENTAL
import org.jetbrains.kotlin.lombok.LombokCliDiagnostics.UNKNOWN_PLUGIN_OPTION
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.ANNOTATION_HAS_NO_EFFECT
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.DO_NOT_USE_GETTERS_IRRELEVANT
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.FLAG_USAGE_ERROR
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.FLAG_USAGE_WARNING
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.LOG_PROPERTY_ALREADY_EXISTS
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.CALL_SUPER_NOT_CALLED
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.TO_STRING_FUNCTION_ALREADY_EXISTS
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED
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
    val ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED by warning1<KtExpression, Name>()
    val ANNOTATION_HAS_NO_EFFECT by warning2<KtAnnotationEntry, String, Collection<KotlinTarget>>()
    val FLAG_USAGE_WARNING by warning1<KtAnnotationEntry, Name>()
    val FLAG_USAGE_ERROR by error1<KtAnnotationEntry, Name>()
    val EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE by error1<KtAnnotationEntry, Name>()
    val DO_NOT_USE_GETTERS_IRRELEVANT by warning0<KtExpression>()
    val CALL_SUPER_NOT_CALLED by warning2<KtAnnotationEntry, String, Name>()

    val LOG_PROPERTY_ALREADY_EXISTS by warning1<KtAnnotationEntry, Name>()
    val TO_STRING_FUNCTION_ALREADY_EXISTS by warning0<KtAnnotationEntry>()
    val NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED by error0<KtAnnotationEntry>()
    val EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST by error0<KtAnnotationEntry>()

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
        map.put(
            ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED,
            "Lombok annotation argument ''{0}'' is not supported in Kotlin.",
            CommonRenderers.NAME
        )
        map.put(
            ANNOTATION_HAS_NO_EFFECT,
            "This annotation has no effect on target ''{0}''. Relevant targets: {1}.",
            TO_STRING,
            KOTLIN_TARGETS,
        )
        map.put(FLAG_USAGE_WARNING, FLAG_USAGE_MESSAGE, CommonRenderers.NAME)
        map.put(FLAG_USAGE_ERROR, FLAG_USAGE_MESSAGE, CommonRenderers.NAME)

        map.put(LOG_PROPERTY_ALREADY_EXISTS, "Property ''{0}'' already exists.", CommonRenderers.NAME)
        map.put(TO_STRING_FUNCTION_ALREADY_EXISTS, "Not generating 'toString()': A method with that name already exists.")
        map.put(
            CALL_SUPER_NOT_CALLED,
            "Generating ''{0}'' implementation but without a call to superclass, even though this class does not extend ''Any''. " +
                    "If this is intentional, add ''@{1}(callSuper=false)'' to your type.",
            CommonRenderers.STRING,
            CommonRenderers.NAME,
        )
        map.put(
            EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE,
            "''@{0}.Exclude'' and ''@{0}.Include'' are mutually exclusive; the ''@Include'' annotation will be ignored.",
            CommonRenderers.NAME,
        )
        map.put(
            DO_NOT_USE_GETTERS_IRRELEVANT,
            "The 'doNotUseGetters' parameter has no effect in Kotlin. " +
                    "Unlike Java, Kotlin properties do not distinguish between direct field access and getter calls."
        )
        map.put(
            NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED,
            "Class contains required properties. " +
                    "Use '@NoArgsConstructor(force = true)' to force-initialize them to default values (0 / false / null)."
        )
        map.put(
            EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST,
            "Not generating 'equals' and 'hashCode': A method with one of those names already exists. (Either both or none of these methods will be generated)."
        )
    }
}
