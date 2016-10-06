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
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericRepl
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import javax.script.ScriptException

class KotlinJsr223JvmLocalScriptEngine(
        disposable: Disposable,
        factory: ScriptEngineFactory,
        val templateClasspath: List<File>,
        templateClassName: String,
        getScriptArgs: (ScriptContext) -> Array<Any?>?,
        scriptArgsTypes: Array<Class<*>>?
) : KotlinJsr223JvmScriptEngineBase(factory) {

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
                    val firstErr = reports.firstOrNull { it.severity.isError }
                    if (firstErr != null)
                        throw ScriptException(messageRenderer.render(firstErr.severity, firstErr.message, firstErr.location), firstErr.location.path, firstErr.location.line, firstErr.location.column)
                    else
                        throw ScriptException(reports.joinToString("\n") { messageRenderer.render(it.severity, it.message, it.location) })
                }
            }
            finally {
                clear()
                hasErrors = false
            }
        }
    }

    private val repl by lazy {
        GenericRepl(
                disposable,
                makeScriptDefinition(templateClasspath, templateClassName),
                makeCompilerConfiguration(),
                messageCollector,
                Thread.currentThread().contextClassLoader,
                getScriptArgs(getContext()),
                scriptArgsTypes)
    }

    private fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this.javaClass.classLoader)
        val cls = classloader.loadClass(templateClassName)
        return KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, null, null, emptyMap())
    }

    private fun makeCompilerConfiguration() = CompilerConfiguration().apply {
        addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
        addJvmClasspathRoots(templateClasspath)
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
    }

    override fun eval(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplEvalResult {
        val evalResult = repl.eval(codeLine, history)
        messageCollector.resetAndThrowOnErrors()
        return evalResult
    }
}
