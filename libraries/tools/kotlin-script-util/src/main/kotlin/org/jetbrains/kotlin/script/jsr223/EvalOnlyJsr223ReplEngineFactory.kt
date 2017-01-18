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

import org.jetbrains.kotlin.script.jsr223.core.AbstractEngineFactory
import javax.script.ScriptEngine


open class EvalOnlyJsr223ReplEngineFactory : AbstractEngineFactory() {
    override fun getScriptEngine(): ScriptEngine {
        return EvalOnlyJsr223ReplEngine(this).apply { fixupArgsForScriptTemplate() }
    }

    override fun getEngineName(): String = "Kotlin Eval-Only Scripting Engine"
    override fun getNames(): List<String> = listOf(jsr223EngineName)
    override fun getThreadingModel(): String = "MULTITHREADED"

    companion object {
        val jsr223EngineName = EvalOnlyJsr223ReplEngine.jsr223EngineName
    }
}