/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object FirDataFrameErrors : KtDiagnosticsContainer() {
    val CAST_ERROR by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val CAST_TARGET_WARNING by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val MATERIALIZED_SCHEMA_INFO by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATA_SCHEMA_DECLARATION_VISIBILITY by error1<KtElement, String>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR by error1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATAFRAME_EXTENSION_PROPERTY_SHADOWED by warning1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATA_SCHEMA_LOCAL_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATAFRAME_PLUGIN_IS_DISABLED by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = DataFrameDiagnosticMessages

    object DataFrameDiagnosticMessages : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrameDiagnosticMessages") { map ->
            map.put(CAST_ERROR, "{0}", TO_STRING)
            map.put(CAST_TARGET_WARNING, "{0}", TO_STRING)
            map.put(MATERIALIZED_SCHEMA_INFO, "{0}", TO_STRING)
            map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE, "{0}", TO_STRING)
            map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC, "{0}", TO_STRING)
            map.put(DATA_SCHEMA_DECLARATION_VISIBILITY, "{0}", TO_STRING)
            map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR, "{0}", TO_STRING)
            map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE, "{0}", TO_STRING)
            map.put(DATAFRAME_EXTENSION_PROPERTY_SHADOWED, "{0}", TO_STRING)
            map.put(DATA_SCHEMA_LOCAL_DECLARATION, "{0}", TO_STRING)
            map.put(DATAFRAME_PLUGIN_IS_DISABLED, "{0}", TO_STRING)
        }
    }
}

object ImportedSchemasDiagnostics : KtDiagnosticsContainer() {
    val CONFLICTING_COMPANION_OBJECT_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val INVALID_SUPERTYPE by error1<KtElement, String>(SourceElementPositioningStrategies.SUPERTYPES_LIST)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = ImportedSchemaDiagnosticRenderers

    private object ImportedSchemaDiagnosticRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Imported Schemas") {
            it.put(CONFLICTING_COMPANION_OBJECT_DECLARATION, "{0}", TO_STRING)
            it.put(INVALID_SUPERTYPE, "{0}", TO_STRING)
        }
    }
}
