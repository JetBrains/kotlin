/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.script.experimental.api

import kotlin.script.experimental.api.ast.SyntaxElement
import kotlin.script.experimental.api.impl.simpleRefineImpl
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The callback that will be called on the script compilation after converting the script into a AST representation.
 * See the examples of AST processing in compiler plugins.
 */
val ScriptCompilationConfigurationKeys.refineConfigurationOnAst by PropertiesCollection.key<List<RefineConfigurationUnconditionallyData>>(isTransient = true)

/**
 * The helper function to configure the [refineConfigurationOnAst] callback, should be called inside the [refineConfiguration] block.
 *
 * @param handler the callback that will be called
 */
fun RefineConfigurationBuilder.onAst(handler: RefineScriptCompilationConfigurationHandler) {
    ScriptCompilationConfiguration.refineConfigurationOnAst.append(RefineConfigurationUnconditionallyData(handler))
}

/**
 * The helper function for chaining refinement calls on the compilation
 */
fun ScriptCompilationConfiguration.refineOnSyntaxTree(
    script: SourceCode,
    collectedData: ScriptCollectedData?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    simpleRefineImpl(ScriptCompilationConfiguration.refineConfigurationOnAst) { config, refineData ->
        refineData.handler.invoke(ScriptConfigurationRefinementContext(script, config, collectedData))
    }

/**
 * The script AST representation.
 *
 * Note that this AST representation could be specifically prepared for the refinement and could be different from the regular
 * representation used for the compilation itself.
 */
val ScriptCollectedDataKeys.syntaxTree by PropertiesCollection.key<SyntaxElement>()

