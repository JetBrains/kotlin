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
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.script.util.classpathFromClass
import org.jetbrains.kotlin.script.util.classpathFromClassloader
import org.jetbrains.kotlin.script.util.classpathFromClasspathProperty
import org.jetbrains.kotlin.script.util.manifestClassPath
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class KotlinJsr223JvmLocalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmLocalScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    scriptCompilationClasspathFromContext("kotlin-script-util.jar"),
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
                    arrayOf(Bindings::class)
            )
}

class KotlinJsr223JvmDaemonLocalEvalScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmDaemonCompileScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    kotlinCompilerJar,
                    scriptCompilationClasspathFromContext("kotlin-script-util.jar"),
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray()) },
                    arrayOf(Bindings::class)
            )
}

private const val KOTLIN_COMPILER_JAR = "kotlin-compiler.jar"
private const val KOTLIN_JAVA_STDLIB_JAR = "kotlin-stdlib.jar"
private const val KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar"

private fun File.existsOrNull(): File? = existsAndCheckOrNull { true }
private inline fun File.existsAndCheckOrNull(check: (File.() -> Boolean)): File? = if (exists() && check()) this else null

private fun <T> Iterable<T>.anyOrNull(predicate: (T) -> Boolean) = if (any(predicate)) this else null

private fun File.matchMaybeVersionedFile(baseName: String) =
        name == baseName ||
        name == baseName.removeSuffix(".jar") || // for classes dirs
        name.startsWith(baseName.removeSuffix(".jar") + "-")

private fun contextClasspath(keyName: String, classLoader: ClassLoader): List<File>? =
        (classpathFromClassloader(classLoader)?.anyOrNull { it.matchMaybeVersionedFile(keyName) }
         ?: manifestClassPath(classLoader)?.anyOrNull { it.matchMaybeVersionedFile(keyName) }
        )?.toList()

private val validJarExtensions = setOf("jar", "zip")

private fun scriptCompilationClasspathFromContext(keyName: String, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<File> =
        (System.getProperty("kotlin.script.classpath")?.split(File.pathSeparator)?.map(::File)
          ?: contextClasspath(keyName, classLoader)
        ).let {
            it?.plus(kotlinScriptStandardJars) ?: kotlinScriptStandardJars
        }
        .mapNotNull { it?.canonicalFile }
        .distinct()
        .filter { (it.isDirectory || (it.isFile && it.extension.toLowerCase() in validJarExtensions)) && it.exists() }

private val kotlinCompilerJar: File by lazy {
    // highest prio - explicit property
    System.getProperty("kotlin.compiler.jar")?.let(::File)?.existsOrNull()
    // search classpath from context classloader and `java.class.path` property
    ?: (classpathFromClass(Thread.currentThread().contextClassLoader, K2JVMCompiler::class)
        ?: contextClasspath(KOTLIN_COMPILER_JAR, Thread.currentThread().contextClassLoader)
        ?: classpathFromClasspathProperty()
       )?.firstOrNull { it.matchMaybeVersionedFile(KOTLIN_COMPILER_JAR) }
    ?: throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.jar property to proper location")
}

private val kotlinStdlibJar: File? by lazy {
    System.getProperty("kotlin.java.runtime.jar")?.let(::File)?.existsOrNull()
    ?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_STDLIB_JAR) }.existsOrNull()
    ?: PathUtil.getResourcePathForClass(JvmStatic::class.java).existsOrNull()
}

private val kotlinScriptRuntimeJar: File? by lazy {
    System.getProperty("kotlin.script.runtime.jar")?.let(::File)?.existsOrNull()
    ?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR) }.existsOrNull()
    ?: PathUtil.getResourcePathForClass(ScriptTemplateWithArgs::class.java).existsOrNull()
}

private val kotlinScriptStandardJars by lazy { listOf(kotlinStdlibJar, kotlinScriptRuntimeJar) }
