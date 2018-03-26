/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.BasicScriptingHost

open class JvmBasicScriptingHost<ScriptBase : Any>(
    configurationExtractor: ScriptCompilationConfigurator,
    compiler: JvmScriptCompiler,
    evaluator: ScriptEvaluator<ScriptBase>
) : BasicScriptingHost<ScriptBase>(configurationExtractor, compiler, evaluator)

class JvmScriptEvaluationEnvironmentParams : ScriptEvaluationEnvironmentParams() {
    companion object {
        val baseClassLoader by typedKey<ClassLoader?>()
    }
}

inline fun jvmScriptEvaluationEnvironment(from: HeterogeneousMap = HeterogeneousMap(), body: JvmScriptEvaluationEnvironmentParams.() -> Unit) =
    JvmScriptEvaluationEnvironmentParams().build(from, body)
