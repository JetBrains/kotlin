/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.IReplStageHistory
import org.jetbrains.kotlin.cli.common.repl.IReplStageState
import org.jetbrains.kotlin.js.engine.ScriptEngineWithTypedResult
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

// NOTE: the state management machinery is not implemented here, since it is unused at the moment in the JS REPL (see JvmReplEvaluatorState for complete implementation, if needed)
class JsEvaluationState(override val lock: ReentrantReadWriteLock, val engine: ScriptEngineWithTypedResult) : IReplStageState<Nothing> {
    override fun dispose() {
        engine.reset()
    }

    override val history: IReplStageHistory<Nothing>
        get() = TODO("not implemented")

    override val currentGeneration: Int
        get() = TODO("not implemented")
}

class JsCompiledScript(
    val jsCode: String,
    override val compilationConfiguration: ScriptCompilationConfiguration
) : CompiledScript {
    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> {
        throw IllegalStateException("Class is not available for JS implementation")
    }
}
