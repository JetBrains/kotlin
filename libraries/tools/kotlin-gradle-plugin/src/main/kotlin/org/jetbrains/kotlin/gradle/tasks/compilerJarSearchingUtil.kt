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
import java.io.File
import java.util.zip.ZipFile

private val K2JVM_COMPILER_CLASS = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
private val K2JS_COMPILER_CLASS = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
private val K2METADATA_COMPILER_CLASS = "org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler"

internal fun findKotlinJvmCompilerJar(project: Project): File? =
        findKotlinCompilerJar(project, K2JVM_COMPILER_CLASS)

internal fun findKotlinJsCompilerJar(project: Project): File? =
        findKotlinCompilerJar(project, K2JS_COMPILER_CLASS)

internal fun findKotlinMetadataCompilerJar(project: Project): File? =
        findKotlinCompilerJar(project, K2METADATA_COMPILER_CLASS)

private fun findKotlinCompilerJar(project: Project, compilerClassName: String): File? {
    fun Project.classpathJars(): Sequence<File> =
            buildscript.configurations.findByName("classpath")?.files?.asSequence() ?: emptySequence()

    val entryToFind = compilerClassName.replace(".", "/") + ".class"
    val jarFromClasspath = project.classpathJars().firstOrNull { it.hasEntry(entryToFind) }

    return when {
        jarFromClasspath != null ->
            jarFromClasspath
        project.parent != null ->
            findKotlinCompilerJar(project.parent, compilerClassName)
        else ->
            null

    }
}

private fun File.hasEntry(entryToFind: String): Boolean {
    val zip = ZipFile(this)

    try {
        val enumeration = zip.entries()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            if (entry.name.equals(entryToFind, ignoreCase = true)) return true
        }
    }
    catch (e: Exception) {
        return false
    }
    finally {
        zip.close()
    }

    return false
}

