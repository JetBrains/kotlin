/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.repl

import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.util.ChainedPropertyBag


object ReplEvaluationEnvironmentParams {

}

typealias ReplEvaluationEnvironment = ChainedPropertyBag

interface ReplSnippetEvaluator {

    suspend operator fun invoke(
        compiledSnippet: CompiledReplSnippet<*>,
        replEvaluationEnvironment: ReplEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult>
}
