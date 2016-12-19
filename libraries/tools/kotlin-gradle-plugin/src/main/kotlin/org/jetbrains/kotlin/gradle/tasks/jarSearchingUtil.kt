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
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipFile

private val K2JVM_COMPILER_CLASS = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
private val K2JS_COMPILER_CLASS = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
private val KOTLIN_MODULE_GROUP = "org.jetbrains.kotlin"
private val KOTLIN_GRADLE_PLUGIN = "kotlin-gradle-plugin"
private val KOTLIN_COMPILER_EMBEDDABLE = "kotlin-compiler-embeddable"

internal fun findKotlinJvmCompilerJar(project: Project): File? =
        findKotlinCompilerJar(project, K2JVM_COMPILER_CLASS)

internal fun findKotlinJsCompilerJar(project: Project): File? =
        findKotlinCompilerJar(project, K2JS_COMPILER_CLASS)

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

private fun findKotlinCompilerJar(project: Project, compilerClassName: String): File? {
    val filesToCheck = findPotentialCompilerJars(project)
    val entryToFind = compilerClassName.replace(".", "/") + ".class"
    return filesToCheck.firstOrNull { it.hasEntry(entryToFind) }
}

private fun findPotentialCompilerJars(project: Project): Iterable<File> {
    val projects = generateSequence(project) { it.parent }
    val classpathConfigurations = projects
            .map { it.buildscript.configurations.findByName(ScriptHandler.CLASSPATH_CONFIGURATION) }
            .filterNotNull()

    val allFiles = HashSet<File>()

    for (configuration in classpathConfigurations) {
        val compilerEmbeddable = findCompilerEmbeddable(configuration)

        if (compilerEmbeddable != null) {
            return compilerEmbeddable.moduleArtifacts.map { it.file }
        }
        else {
            allFiles.addAll(configuration.files)
        }
    }

    return allFiles
}

private fun findCompilerEmbeddable(configuration: Configuration): ResolvedDependency? {
    fun Iterable<ResolvedDependency>.findDependency(group: String, name: String): ResolvedDependency? =
            find { it.moduleGroup == group && it.moduleName == name }

    val firstLevelModuleDependencies = configuration.resolvedConfiguration.firstLevelModuleDependencies
    val gradlePlugin = firstLevelModuleDependencies.findDependency(KOTLIN_MODULE_GROUP, KOTLIN_GRADLE_PLUGIN)
    return gradlePlugin?.children?.findDependency(KOTLIN_MODULE_GROUP, KOTLIN_COMPILER_EMBEDDABLE)
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
