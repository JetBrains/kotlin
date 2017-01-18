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

package org.jetbrains.kotlin.script.jsr223.core

import org.jetbrains.kotlin.script.CompileResult
import org.jetbrains.kotlin.script.Evaluable
import org.jetbrains.kotlin.script.ReplCompilerException
import java.io.Reader
import javax.script.*


abstract class AbstractCompilableScriptEngine(factory: ScriptEngineFactory,
                                              defaultImports: List<String> = emptyList())
    : AbstractInvocableReplScriptEngine(factory, defaultImports), Compilable, Invocable {

    override fun compile(script: String): CompiledScript {
        try {
            @Suppress("DEPRECATION")
            val delayed = engine.compileToEvaluable(engine.nextCodeLine(script))
            return CompiledCode(delayed)
        }
        catch (ex: ReplCompilerException) {
            throw ScriptException(ex.errorResult.message,
                                  ex.errorResult.location.path,
                                  ex.errorResult.location.line,
                                  ex.errorResult.location.column)
        }
        catch (ex: Exception) {
            throw ScriptException(ex)
        }
    }

    override fun compile(script: Reader): CompiledScript {
        return compile(script.use(Reader::readText))
    }

    inner class CompiledCode(val compiled: Evaluable) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? {
            @Suppress("DEPRECATION")
            return compiled.eval(baseArgsForScriptTemplate(context), makeBestIoTrappingInvoker(context)).resultValue
        }

        override fun getEngine(): ScriptEngine {
            return this@AbstractCompilableScriptEngine
        }
    }

}