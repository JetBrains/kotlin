/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script.jsr223

import org.jetbrains.kotlin.cli.common.repl.ReplRepeatingMode
import org.jetbrains.kotlin.script.SimplifiedRepl
import org.jetbrains.kotlin.script.jsr223.core.AbstractInvocableReplScriptEngine
import javax.script.ScriptEngineFactory

open class EvalOnlyJsr223ReplEngine(factory: ScriptEngineFactory,
                                    defaultImports: List<String> = emptyList())
    : AbstractInvocableReplScriptEngine(factory, defaultImports) {

    override val engine: SimplifiedRepl by lazy {
        SimplifiedRepl(
                moduleName = moduleName,
                additionalClasspath = extraClasspath,
                repeatingMode = ReplRepeatingMode.NONE,
                scriptDefinition = scriptDefinition,
                sharedHostClassLoader = Thread.currentThread().contextClassLoader
        )
    }

    companion object {
        val jsr223EngineName = "kotin-repl-eval-only"
    }
}


