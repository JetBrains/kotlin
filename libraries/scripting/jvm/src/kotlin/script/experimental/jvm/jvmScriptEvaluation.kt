/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.ScriptCompilationConfigurator
import kotlin.script.experimental.api.ScriptEvaluator
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.util.typedKey

open class JvmBasicScriptingHost<ScriptBase : Any>(
    configurationExtractor: ScriptCompilationConfigurator,
    compiler: JvmScriptCompiler,
    evaluator: ScriptEvaluator<ScriptBase>
) : BasicScriptingHost<ScriptBase>(configurationExtractor, compiler, evaluator)

object JvmScriptEvaluationEnvironmentProperties {
    val baseClassLoader by typedKey<ClassLoader?>()
}
