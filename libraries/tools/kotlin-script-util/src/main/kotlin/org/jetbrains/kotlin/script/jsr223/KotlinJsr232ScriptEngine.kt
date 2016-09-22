/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.repl.GenericRepl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.Reader
import javax.script.*

class KotlinJsr232ScriptEngine(
        disposable: Disposable,
        private val factory: ScriptEngineFactory,
        private val scriptDefinition: KotlinScriptDefinition,
        private val compilerConfiguration: CompilerConfiguration,
        baseClassLoader: ClassLoader
) : AbstractScriptEngine(), ScriptEngine {

    data class MessageCollectorReport(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageLocation)

    private val messageCollector = object : MessageCollector {

        private val messageRenderer = MessageRenderer.WITHOUT_PATHS
        private var hasErrors = false
        private val reports = arrayListOf<MessageCollectorReport>()

        override fun clear() {
            reports.clear()
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            hasErrors = hasErrors or severity.isError
            reports.add(MessageCollectorReport(severity, message, location))
        }

        override fun hasErrors(): Boolean = hasErrors

        fun resetAndThrowOnErrors() {
            try {
                if (hasErrors) {
                    val msg = reports.joinToString("\n") { messageRenderer.render(it.severity, it.message, it.location) }
                    val firstErr = reports.firstOrNull { it.severity.isError }
                    if (firstErr != null)
                        throw ScriptException(msg, firstErr.location.path, firstErr.location.line, firstErr.location.column)
                    else
                        throw ScriptException(msg)
                }
            }
            finally {
                clear()
                hasErrors = false
            }
        }
    }

    private val repl = object : GenericRepl(disposable, scriptDefinition, compilerConfiguration, messageCollector, baseClassLoader) {}

    private var lineCount = 0

    private val history = arrayListOf<ReplCodeLine>()

    override fun eval(script: String, context: ScriptContext?): Any? {
        lineCount += 1
        // TODO bind to context
        val codeLine = ReplCodeLine(lineCount, script)
        val evalResult = repl.eval(codeLine, history)
        messageCollector.resetAndThrowOnErrors()
        val ret = when (evalResult) {
            is ReplEvalResult.ValueResult -> evalResult.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(evalResult.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${evalResult.lineNo}")
        }
        history.add(codeLine)
        // TODO update context
        return ret
    }

    override fun eval(script: Reader, context: ScriptContext?): Any? = eval(script.readText(), context)

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = factory
}
