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

@file:Suppress("unused") // could be used externally in javax.script.ScriptEngineFactory META-INF file

package org.jetbrains.kotlin.script.jsr223

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.utils.PathUtil.*
import java.io.File
import java.io.FileNotFoundException
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.StandardScriptTemplate

class KotlinJsr223JvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmLocalScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    listOf(kotlinRuntimeJar),
                    "kotlin.script.ScriptTemplateWithArgsAndBindings",
                    ::makeArgumentsForTemplateWithArgsAndBindings,
                    arrayOf(Array<String>::class.java, java.util.Map::class.java)
            )
}

class KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmDaemonLocalEvalScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    kotlinCompilerJar,
                    listOf(kotlinRuntimeJar),
                    "kotlin.script.ScriptTemplateWithArgsAndBindings",
                    ::makeArgumentsForTemplateWithArgsAndBindings,
                    arrayOf(Array<String>::class.java, java.util.Map::class.java)
            )
}

class KotlinJsr223JvmDaemonRemoteEvalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmDaemonRemoteEvalScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    kotlinCompilerJar,
                    listOf(kotlinRuntimeJar),
                    "kotlin.script.ScriptTemplateWithArgsAndBindings",
                    ::makeSerializableArgumentsForTemplateWithArgsAndBindings,
                    arrayOf(Array<String>::class.java, java.util.Map::class.java)
            )
}

private fun makeArgumentsForTemplateWithArgsAndBindings(ctx: ScriptContext): Array<Any?> {
    val bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE)
    return arrayOf(
            (bindings[ScriptEngine.ARGV] as? Array<*>) ?: emptyArray<String>(),
            bindings)
}

private fun makeSerializableArgumentsForTemplateWithArgsAndBindings(ctx: ScriptContext): Array<Any?> {
    val bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE)
    val serializableBindings = linkedMapOf<String, Any>()
    // TODO: consider deeper analysis and copying to serializable data if possible
    serializableBindings.putAll(bindings)
    return arrayOf(
            (bindings[ScriptEngine.ARGV] as? Array<*>) ?: emptyArray<String>(),
            serializableBindings)
}

private fun File.existsOrNull(): File? = existsAndCheckOrNull { true }
private inline fun File.existsAndCheckOrNull(check: (File.() -> Boolean)): File? = if (exists() && check()) this else null

private val kotlinCompilerJar = System.getProperty("kotlin.compiler.jar")?.let(::File)?.existsOrNull()
        ?: getPathUtilJar().existsAndCheckOrNull { name == KOTLIN_COMPILER_JAR }
        ?: throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.jar property to proper location")

private val kotlinRuntimeJar = System.getProperty("kotlin.java.runtime.jar")?.let(::File)?.existsOrNull()
        ?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_RUNTIME_JAR) }.existsOrNull()
        ?: getResourcePathForClass(StandardScriptTemplate::class.java).existsOrNull()
        ?: throw FileNotFoundException("Cannot find kotlin runtime jar, set kotlin.java.runtime.jar property to proper location")
