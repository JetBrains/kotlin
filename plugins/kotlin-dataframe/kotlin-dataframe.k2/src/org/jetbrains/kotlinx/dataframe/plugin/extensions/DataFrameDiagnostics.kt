/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

object FirDataFrameErrors : KtDiagnosticsContainer() {
    val CAST_ERROR by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val CAST_TARGET_WARNING by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MATERIALIZED_SCHEMA_INFO by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE by warning0<KtElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC by warning0<KtElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATA_SCHEMA_DECLARATION_VISIBILITY by error1<KtElement, String>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR by error0<KtElement>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATAFRAME_EXTENSION_PROPERTY_SHADOWED by warning0<KtElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATA_SCHEMA_LOCAL_DECLARATION by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATAFRAME_PLUGIN_IS_DISABLED by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = DataFrameDiagnosticMessages

    object DataFrameDiagnosticMessages : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrameDiagnosticMessages") { map ->
            map.put(CAST_ERROR, "Cast cannot succeed \n {0}", TO_STRING)
            map.put(CAST_TARGET_WARNING, "Annotate {0} with @DataSchema to use generated properties", TO_STRING)
            map.put(MATERIALIZED_SCHEMA_INFO, "{0}", TO_STRING)
            map.put(
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE,
                "DataFrame compiler plugin is not yet supported in inline functions. " +
                        "Annotate containing declaration with @DisableInterpretation to suppress this warning"
            )
            map.put(
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC,
                "DataFrame compiler plugin is not yet supported in generic context. " +
                        "Annotate containing declaration with @DisableInterpretation to suppress this warning"
            )
            map.put(
                DATA_SCHEMA_DECLARATION_VISIBILITY,
                "To allow plugin-generated declarations to refer to this declaration, it must be declared as either of [{0}]",
                TO_STRING
            )
            map.put(
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR,
                "DataFrame compiler plugin is not yet supported in property accessors bodies. " +
                        "Use property with initializer, a function, or annotate containing declaration with @DisableInterpretation to suppress this warning"
            )
            map.put(
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE,
                "Local types produced by the DataFrame compiler plugin are not yet supported in property return types. " +
                        "Convert this property to a function or cast it to a DataSchema type."
            )
            map.put(
                DATAFRAME_EXTENSION_PROPERTY_SHADOWED,
                "Extension property with implicit receiver is shadowed by a property with the same name."
            )
            map.put(
                DATA_SCHEMA_LOCAL_DECLARATION,
                "@DataSchema declaration cannot be local. Move it outside function body. " +
                        "This is required so that plugin-generated extension properties can refer to this @DataSchema"
            )
            map.put(DATAFRAME_PLUGIN_IS_DISABLED, "DataFrame compiler plugin is disabled by @DisableInterpretation on {0}", TO_STRING)
        }
    }
}

object ImportedSchemasDiagnostics : KtDiagnosticsContainer() {
    val CONFLICTING_COMPANION_OBJECT_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val INVALID_SUPERTYPE by error2<KtElement, String, String>(SourceElementPositioningStrategies.SUPERTYPES_LIST)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = ImportedSchemaDiagnosticRenderers

    private object ImportedSchemaDiagnosticRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Imported Schemas") {
            it.put(
                CONFLICTING_COMPANION_OBJECT_DECLARATION,
                "Declaration conflicts with plugin-generated companion object. " +
                        "Add `: ${Names.DATAFRAME_PROVIDER.shortClassName}<{0}>` supertype to resolve the conflict, or remove companion object.",
                TO_STRING
            )
            it.put(
                INVALID_SUPERTYPE,
                "Expected type argument of ${Names.DATAFRAME_PROVIDER.shortClassName}: {0}. Actual: {1}",
                TO_STRING,
                TO_STRING
            )
        }
    }
}
