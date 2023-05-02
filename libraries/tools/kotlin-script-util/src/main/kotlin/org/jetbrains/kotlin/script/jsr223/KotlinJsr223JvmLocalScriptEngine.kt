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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.script.jsr223

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.GenericReplCompiler
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import kotlin.reflect.KClass

@Deprecated("Use kotlin-scripting-jsr223 instead")
class KotlinJsr223JvmLocalScriptEngine(
        factory: ScriptEngineFactory,
        val templateClasspath: List<File>,
        templateClassName: String,
        val getScriptArgs: (ScriptContext, Array<out KClass<out Any>>?) -> ScriptArgsWithTypes?,
        val scriptArgsTypes: Array<out KClass<out Any>>?
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223JvmInvocableScriptEngine {

    override val replCompiler: ReplCompiler by lazy {
       GenericReplCompiler(
               makeScriptDefinition(templateClasspath, templateClassName),
               makeCompilerConfiguration(),
               PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false))
    }
    // TODO: bindings passing works only once on the first eval, subsequent setContext/setBindings call have no effect. Consider making it dynamic, but take history into account
    private val localEvaluator by lazy { GenericReplCompilingEvaluator(replCompiler, templateClasspath, Thread.currentThread().contextClassLoader, getScriptArgs(getContext(), scriptArgsTypes)) }

    override val replEvaluator: ReplFullEvaluator get() = localEvaluator

    override val state: IReplStageState<*> get() = getCurrentState(getContext())

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = replEvaluator.createState(lock)

    override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = getScriptArgs(context, scriptArgsTypes)

    private fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this.javaClass.classLoader)
        val cls = classloader.loadClass(templateClassName)
        return KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, emptyMap())
    }

    private fun makeCompilerConfiguration() = CompilerConfiguration().apply {
        addJvmSdkRoots(PathUtil.getJdkClassesRootsFromCurrentJre())
        addJvmClasspathRoots(templateClasspath)
        configureJdkClasspathRoots()
        add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
        languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, mapOf(AnalysisFlags.skipMetadataVersionCheck to true)
        )
    }
}
