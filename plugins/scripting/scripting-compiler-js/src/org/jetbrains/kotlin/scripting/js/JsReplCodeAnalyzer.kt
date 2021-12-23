/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.js

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.toSourceCode
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities

class JsReplCodeAnalyzer(
    environment: KotlinCoreEnvironment,
    dependencies: List<ModuleDescriptor>,
    private val replState: ReplCodeAnalyzerBase.ResettableAnalyzerState
) : AbstractJsScriptlikeCodeAnalyser(environment, dependencies) {

    fun analyzeReplLine(linePsi: KtFile, codeLine: ReplCodeLine): AnalysisResult {
        linePsi.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, codeLine.no)
        replState.submitLine(linePsi)

        val result = analysisImpl(linePsi)

        return if (result.isSuccess) {
            replState.lineSuccess(linePsi, codeLine.toSourceCode(), result.script)
            AnalysisResult.success(result.bindingContext, result.moduleDescriptor)
        } else {
            replState.lineFailure(linePsi)
            AnalysisResult.compilationError(result.bindingContext)
        }
    }
}
