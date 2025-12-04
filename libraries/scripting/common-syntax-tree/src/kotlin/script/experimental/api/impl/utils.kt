/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api.impl

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.util.PropertiesCollection

internal inline fun <Configuration : PropertiesCollection, RefineData> Configuration.simpleRefineImpl(
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

