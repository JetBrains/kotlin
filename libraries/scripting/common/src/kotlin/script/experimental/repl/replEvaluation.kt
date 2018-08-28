/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.repl

import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.util.PropertiesCollection

interface ReplEvaluationEnvironmentKeys

class ReplEvaluationEnvironment(baseEvaluationEnvironments: Iterable<ReplEvaluationEnvironment>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseEvaluationEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseEvaluationEnvironments: ReplEvaluationEnvironment, body: Builder.() -> Unit = {}
    ) : this(baseEvaluationEnvironments.asIterable(), body)

    class Builder internal constructor(baseEvaluationEnvironments: Iterable<ReplEvaluationEnvironment>) :
        ReplEvaluationEnvironmentKeys,
        PropertiesCollection.Builder(baseEvaluationEnvironments)

    companion object : ReplEvaluationEnvironmentKeys
}

interface ReplSnippetEvaluator {

    suspend operator fun invoke(
        compiledSnippet: CompiledReplSnippet<*>,
        replEvaluationEnvironment: ReplEvaluationEnvironment
    ): ResultWithDiagnostics<EvaluationResult>
}
