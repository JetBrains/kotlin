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

package org.jetbrains.kotlin.script.util


import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.repl.GenericRepl
import java.io.File
import kotlin.reflect.KClass

// TODO: are some of these already in PathUtils?

fun findKotlinCompilerJarsOrEmpty(useEmbeddedCompiler: Boolean = true): List<File> {
    val filter = if (useEmbeddedCompiler) """.*\/kotlin-compiler-embeddable.*\.jar""".toRegex()
    else """.*\/kotlin-compiler-(?!embeddable).*\.jar""".toRegex()
    return listOf(K2JVMCompiler::class.containingClasspath(filter)).filterNotNull()
}

fun <T : Any> List<T>.assertNotEmpty(error: String): List<T> {
    if (this.isEmpty()) throw IllegalStateException(error)
    return this
}

fun findKotlinCompilerJars(useEmbeddedCompiler: Boolean = true): List<File> {
    return findKotlinCompilerJarsOrEmpty(useEmbeddedCompiler).assertNotEmpty("Cannot find kotlin compiler classpath, which is required")
}

fun findKotlinStdLibJarsOrEmpty(): List<File> {
    return listOf(Pair::class.containingClasspath(""".*\/kotlin-stdlib.*\.jar""".toRegex())).filterNotNull()
}

fun findKotlinStdLibJars(): List<File> {
    return findKotlinStdLibJarsOrEmpty().assertNotEmpty("Cannot find kotlin stdlib classpath, which is required")
}

fun findKotlinRuntimeJarsOrEmpty(): List<File> {
    return listOf(JvmName::class.containingClasspath(""".*\/kotlin-runtime.*\.jar""".toRegex())).filterNotNull()
}

fun findKotlinRuntimeJars(): List<File> {
    return findKotlinRuntimeJarsOrEmpty().assertNotEmpty("Cannot find kotlin runtime classpath, which is required")
}

fun findClassJarsOrEmpty(klass: KClass<out Any>, filterJarByRegex: Regex = ".*".toRegex()): List<File> {
    return listOf(klass.containingClasspath(filterJarByRegex)).filterNotNull()
}

fun findClassJars(klass: KClass<out Any>, filterJarByRegex: Regex = ".*".toRegex()): List<File> {
    return findClassJarsOrEmpty(klass, filterJarByRegex).assertNotEmpty("Cannot find required JAR for $klass")
}

fun findRequiredScriptingJarFiles(templateClass: KClass<out Any>? = null,
                                  includeScriptEngine: Boolean = false,
                                  includeKotlinCompiler: Boolean = false,
                                  useEmbeddableCompiler: Boolean = true,
                                  includeStdLib: Boolean = true,
                                  includeRuntime: Boolean = true,
                                  additionalClasses: List<KClass<out Any>> = emptyList()): List<File> {
    val templateClassJars = if (templateClass != null) findClassJarsOrEmpty(templateClass).assertNotEmpty("Cannot find template classpath, which is required")
    else emptyList()
    val additionalClassJars = additionalClasses.map { findClassJarsOrEmpty(it).assertNotEmpty("Missing JAR for additional class $it") }.flatten()
    val scriptEngineJars = if (includeScriptEngine) findClassJarsOrEmpty(GenericRepl::class).assertNotEmpty("Cannot find repl engine classpath, which is required")
    else emptyList()
    val kotlinJars = (if (includeKotlinCompiler) findKotlinCompilerJars(useEmbeddableCompiler) else emptyList()) +
                     (if (includeStdLib) findKotlinStdLibJars() else emptyList()) +
                     (if (includeRuntime) findKotlinRuntimeJars() else emptyList())
    return (templateClassJars + additionalClassJars + scriptEngineJars + kotlinJars).toSet().toList()
}

private val zipOrJarRegex = """(?:zip:|jar:file:)(.*)!\/(?:.*)""".toRegex()
private val filePathRegex = """(?:file:)(.*)""".toRegex()

internal fun zipOrJarUrlToBaseFile(url: String): String? {
    return zipOrJarRegex.find(url)?.let { it.groupValues[1] }
}

internal fun classFilenameToBaseDir(url: String, resource: String): String? {
    return filePathRegex.find(url)?.let { it.groupValues[1].removeSuffix(resource) }
}

internal fun <T : Any> KClass<T>.containingClasspath(filterJarName: Regex = ".*".toRegex()): File? {
    val clp = "${qualifiedName?.replace('.', '/')}.class"
    return Thread.currentThread().contextClassLoader.getResources(clp)
            ?.toList()
            ?.map { it.toString() }
            ?.map { url ->
                zipOrJarUrlToBaseFile(url) ?: qualifiedName?.let { classFilenameToBaseDir(url, clp) }
                ?: throw IllegalStateException("Expecting a local classpath when searching for class: ${qualifiedName}")
            }
            ?.find { filterJarName.matches(it) }
            ?.let { File(it) }
}