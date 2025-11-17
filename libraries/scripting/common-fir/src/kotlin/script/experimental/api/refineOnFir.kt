/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.script.experimental.api

import org.jetbrains.kotlin.fir.declarations.FirFile
import kotlin.script.experimental.api.impl.simpleRefineImpl
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The callback that will be called on the script compilation after converting the script into a FIR representation.
 * See the examples of FIR processing in compiler plugins.
 */
val ScriptCompilationConfigurationKeys.refineConfigurationOnFir by PropertiesCollection.key<List<RefineConfigurationUnconditionallyData>>(isTransient = true)

/**
 * The helper function to configure the [refineConfigurationOnFir] callback, should be called inside the [refineConfiguration] block.
 *
 * @param handler the callback that will be called
 */
fun RefineConfigurationBuilder.onFir(handler: RefineScriptCompilationConfigurationHandler) {
    ScriptCompilationConfiguration.refineConfigurationOnFir.append(RefineConfigurationUnconditionallyData(handler))
}

/**
 * The helper function for chaining refinement calls on the compilation
 */
fun ScriptCompilationConfiguration.refineOnFir(
    script: SourceCode,
    collectedData: ScriptCollectedData?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    simpleRefineImpl(ScriptCompilationConfiguration.refineConfigurationOnFir) { config, refineData ->
        refineData.handler.invoke(ScriptConfigurationRefinementContext(script, config, collectedData))
    }

/**
 * The script FIR representation.
 *
 * Note that this FIR representation could be specifically prepared for the refinement and could be different from the regular
 * representation used for the compilation itself.
 */
val ScriptCollectedDataKeys.fir by PropertiesCollection.key<List<FirFile>>()

