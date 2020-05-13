/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import java.io.File
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl

/**
 * If the property is set to true, the dependencies will be resolved directly from the context classloader
 * (Thread.currentThread().contextClassLoader) using reflection and other techniques (experimental!).
 * Otherwise the same classloader will be used to extract the classpath first, and this classpath will be used instead.
 */
const val KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY = "kotlin.jsr223.experimental.resolve.dependencies.from.context.classloader"

class KotlinJsr223DefaultScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    private val scriptDefinition = createJvmScriptDefinitionFromTemplate<KotlinJsr223DefaultScript>()
    private var lastClassLoader: ClassLoader? = null
    private var lastClassPath: List<File>? = null

    @Synchronized
    private fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext() {
        val currentClassLoader = Thread.currentThread().contextClassLoader
        val classPath = if (lastClassLoader == null || lastClassLoader != currentClassLoader) {
            scriptCompilationClasspathFromContext(
                classLoader = currentClassLoader,
                wholeClasspath = true,
                unpackJarCollections = true
            ).also {
                lastClassLoader = currentClassLoader
                lastClassPath = it
            }
        } else lastClassPath!!
        updateClasspath(classPath)
    }

    override fun getScriptEngine(): ScriptEngine =
        KotlinJsr223ScriptEngineImpl(
            this,
            scriptDefinition.compilationConfiguration.with {
                jvm {
                    if (System.getProperty(KOTLIN_JSR223_RESOLVE_FROM_CLASSLOADER_PROPERTY) == "true") {
                        dependencies(JvmDependencyFromClassLoader { Thread.currentThread().contextClassLoader })
                    } else {
                        dependenciesFromCurrentContext()
                    }
                }
            },
            scriptDefinition.evaluationConfiguration
        ) { ScriptArgsWithTypes(arrayOf(it.getBindings(ScriptContext.ENGINE_SCOPE).orEmpty()), arrayOf(Bindings::class)) }
}

