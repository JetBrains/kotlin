/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.experimental.api.ScriptConfigurator
import kotlin.script.experimental.api.ScriptRunner
import kotlin.script.experimental.api.typedKey
import kotlin.script.experimental.host.BasicScriptingHost

open class JvmBasicScriptingHost<ScriptBase : Any>(
    configurationExtractor: ScriptConfigurator,
    compiler: JvmScriptCompiler,
    runner: ScriptRunner<ScriptBase>
) : BasicScriptingHost<ScriptBase>(configurationExtractor, compiler, runner)

object JvmScriptEvaluationEnvironmentParams {
    val baseClassLoader by typedKey<ClassLoader?>()
}
