/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.impl

import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

// for internal use, could be removed at any point

fun ScriptCompilationConfiguration.refineOnAnnotationsWithLazyDataCollection(
    script: SourceCode,
    collectData: () -> ResultWithDiagnostics<ScriptCollectedData>?
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val refineDataList =
        this[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.takeIf { it.isNotEmpty() } ?: return this.asSuccess()
    return collectData()?.onSuccess { collectedData ->
        val foundAnnotationNames =
            collectedData[ScriptCollectedData.collectedAnnotations]?.mapTo(HashSet()) { it.annotation.annotationClass.java.name }.orEmpty()

        // deprecated legacy templates used unconditional refinement, so we need to call the handler, even if there are no annotations found
        @Suppress("DEPRECATION")
        val isFromLegacy = this[ScriptCompilationConfiguration.fromLegacyTemplate] ?: false
        if (foundAnnotationNames.isEmpty() && !isFromLegacy) return this.asSuccess()

        val thisResult: ResultWithDiagnostics<ScriptCompilationConfiguration> = this.asSuccess()
        return refineDataList.fold(thisResult) { currentResult, (annotations, handler) ->
            currentResult.onSuccess { configuration ->
                // checking that the collected data contains expected annotations
                if (annotations.none { foundAnnotationNames.contains(it.typeName) } && !isFromLegacy) configuration.asSuccess()
                else handler.invoke(ScriptConfigurationRefinementContext(script, configuration, collectedData))
            }
        }
    } ?: this.asSuccess()
}


inline fun <Configuration : PropertiesCollection, RefineData> Configuration.simpleRefineImpl(
    key: PropertiesCollection.Key<List<RefineData>>,
    refineFn: (Configuration, RefineData) -> ResultWithDiagnostics<Configuration>
): ResultWithDiagnostics<Configuration> {
    val diagnostics = mutableListOf<ScriptDiagnostic>()

    val configuration = this[key]
        ?.fold(this) { config, refineData ->
            val result = refineFn(config, refineData)
            diagnostics.addAll(result.reports)
            result.valueOr { return it }
        } ?: this

    return configuration.asSuccess(diagnostics)
}


inline fun <Configuration : PropertiesCollection, RefineData> Configuration.refineImplWithLazyDataCollection(
    key: PropertiesCollection.Key<List<RefineData>>,
    collectRefineData: () -> ScriptCollectedData?,
    refineFn: (Configuration, RefineData, ScriptCollectedData) -> ResultWithDiagnostics<Configuration>
): ResultWithDiagnostics<Configuration> {
    val diagnostics = mutableListOf<ScriptDiagnostic>()

    val refineDataList = this[key]?.takeIf { it.isNotEmpty() }
    val collectedData = if (refineDataList != null) collectRefineData() else null
    val configuration =
        if (refineDataList != null && collectedData != null)
            refineDataList.fold(this) { config, refineData ->
                val result = refineFn(config, refineData, collectedData)
                diagnostics.addAll(result.reports)
                result.valueOr { return it }
            }
        else null

    return (configuration ?: this).asSuccess(diagnostics)
}
