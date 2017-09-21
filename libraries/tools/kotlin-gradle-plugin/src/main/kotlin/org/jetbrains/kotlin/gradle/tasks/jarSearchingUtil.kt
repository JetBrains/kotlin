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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.initialization.dsl.ScriptHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlugin
import java.io.File
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.zip.ZipFile

private val K2JVM_COMPILER_CLASS = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
private val K2JS_COMPILER_CLASS = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
private val K2JS_DCE_CLASS = "org.jetbrains.kotlin.cli.js.dce.K2JSDce"
private val K2METADATA_COMPILER_CLASS = "org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler"
private val KOTLIN_STDLIB_EXPECTED_CLASS = "kotlin.collections.ArraysKt"
private val KOTLIN_SCRIPT_RUNTIME_EXPECTED_CLASS = "kotlin.script.templates.AnnotationsKt"
private val KOTLIN_REFLECT_EXPECTED_CLASS = "kotlin.reflect.full.KClasses"
private val KOTLIN_MODULE_GROUP = "org.jetbrains.kotlin"
private val KOTLIN_GRADLE_PLUGIN = "kotlin-gradle-plugin"
private val KOTLIN_COMPILER_EMBEDDABLE = "kotlin-compiler-embeddable"
private val KOTLIN_STDLIB = "kotlin-stdlib"
private val KOTLIN_SCRIPT_RUNTIME = "kotlin-script-runtime"
private val KOTLIN_REFLECT = "kotlin-reflect"

internal fun findKotlinJvmCompilerClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, K2JVM_COMPILER_CLASS, KOTLIN_COMPILER_EMBEDDABLE).let {
            if (it.isEmpty()) it
            else it + findKotlinStdlibClasspath(project) + findKotlinScriptRuntimeClasspath(project) + findKotlinReflectClasspath(project)
        }

internal fun findKotlinJsCompilerClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, K2JS_COMPILER_CLASS, KOTLIN_COMPILER_EMBEDDABLE).let {
            if (it.isEmpty()) it
            else it + findKotlinStdlibClasspath(project) + findKotlinScriptRuntimeClasspath(project) + findKotlinReflectClasspath(project)
        }

internal fun findKotlinMetadataCompilerClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, K2METADATA_COMPILER_CLASS, KOTLIN_COMPILER_EMBEDDABLE).let {
            if (it.isEmpty()) it
            else it + findKotlinStdlibClasspath(project) + findKotlinScriptRuntimeClasspath(project) + findKotlinReflectClasspath(project)
        }

internal fun findKotlinJsDceClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, K2JS_DCE_CLASS, KOTLIN_COMPILER_EMBEDDABLE).let {
            if (it.isEmpty()) it
            else it + findKotlinStdlibClasspath(project) + findKotlinScriptRuntimeClasspath(project) + findKotlinReflectClasspath(project)
        }

internal fun findKotlinStdlibClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, KOTLIN_STDLIB_EXPECTED_CLASS, KOTLIN_STDLIB)

internal fun findKotlinScriptRuntimeClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, KOTLIN_SCRIPT_RUNTIME_EXPECTED_CLASS, KOTLIN_SCRIPT_RUNTIME)

internal fun findKotlinReflectClasspath(project: Project): List<File> =
        findKotlinModuleJar(project, KOTLIN_REFLECT_EXPECTED_CLASS, KOTLIN_REFLECT)

internal fun findToolsJar(): File? =
        Class.forName("com.sun.tools.javac.util.Context")?.let(::findJarByClass)

private fun findJarByClass(klass: Class<*>): File? {
    val classFileName = klass.name.substringAfterLast(".") + ".class"
    val resource = klass.getResource(classFileName) ?: return null
    val uri = resource.toString()
    if (!uri.startsWith("jar:file:")) return null

    val fileName = URLDecoder.decode(uri.removePrefix("jar:file:").substringBefore("!"), Charset.defaultCharset().name())
    return File(fileName)
}

private fun findKotlinModuleJar(project: Project, expectedClassName: String, moduleId: String): List<File> {
    val pluginVersion = pluginVersionFromAppliedPlugin(project)

    val filesToCheck = sequenceOf(pluginVersion?.let(::getCompilerFromClassLoader)) +
                       Sequence { findPotentialModuleJars(project, moduleId).iterator() } //call the body only when queried
    val entryToFind = expectedClassName.replace(".", "/") + ".class"
    return filesToCheck.filterNotNull().firstOrNull { it.hasEntry(entryToFind) }?.let { listOf(it) } ?: emptyList()
}

private fun pluginVersionFromAppliedPlugin(project: Project): String? =
        project.plugins.filterIsInstance<KotlinBasePluginWrapper>().firstOrNull()?.kotlinPluginVersion

private fun getCompilerFromClassLoader(pluginVersion: String): File? {
    val urlClassLoader = KotlinPlugin::class.java.classLoader as? URLClassLoader ?: return null
    return urlClassLoader.urLs
            .firstOrNull { it.toString().endsWith("kotlin-compiler-embeddable-$pluginVersion.jar") }
            ?.let { File(it.toURI()) }
            ?.takeIf(File::exists)
}

private fun findPotentialModuleJars(project: Project, moduleId: String): Iterable<File> {
    val projects = generateSequence(project) { it.parent }
    val classpathConfigurations = projects
            .map { it.buildscript.configurations.findByName(ScriptHandler.CLASSPATH_CONFIGURATION) }
            .filterNotNull()

    val allFiles = HashSet<File>()

    for (configuration in classpathConfigurations) {
        val compilerEmbeddable = findKotlinModuleDependency(configuration, moduleId)

        if (compilerEmbeddable != null) {
            return compilerEmbeddable.moduleArtifacts.map { it.file }
        }
        else {
            allFiles.addAll(configuration.files)
        }
    }

    return allFiles
}

private fun findKotlinModuleDependency(configuration: Configuration, moduleId: String): ResolvedDependency? {
    fun Iterable<ResolvedDependency>.findDependency(group: String, name: String): ResolvedDependency? =
            find { it.moduleGroup == group && it.moduleName == name }

    val firstLevelModuleDependencies = configuration.resolvedConfiguration.firstLevelModuleDependencies
    val gradlePlugin = firstLevelModuleDependencies.findDependency(KOTLIN_MODULE_GROUP, KOTLIN_GRADLE_PLUGIN)
    return gradlePlugin?.children?.findDependency(KOTLIN_MODULE_GROUP, moduleId)
}

private fun File.hasEntry(entryToFind: String): Boolean {
    val zip = ZipFile(this)

    try {
        return zip.getEntry(entryToFind) != null
    }
    catch (e: Exception) {
        return false
    }
    finally {
        zip.close()
    }
}
