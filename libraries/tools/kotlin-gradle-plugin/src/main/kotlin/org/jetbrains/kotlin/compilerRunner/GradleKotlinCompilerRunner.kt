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

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.plugin.ParentLastURLClassLoader
import org.jetbrains.kotlin.gradle.plugin.kotlinInfo
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import org.jetbrains.kotlin.incremental.makeModuleFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.util.zip.ZipFile
import kotlin.concurrent.thread


const val KOTLIN_COMPILER_JAR_PATH_PROPERTY = "kotlin.compiler.jar.path"

internal class GradleCompilerRunner(private val project: Project) : KotlinCompilerRunner<GradleCompilerEnvironment>() {
    override val log = GradleKotlinLogger(project.logger)
    private val flagFile = run {
        val dir = File(project.rootProject.buildDir, "kotlin").apply { mkdirs() }
        File(dir, "daemon-is-alive").apply {
            if (!exists()) {
                createNewFile()
                deleteOnExit()
            }
        }
    }

    fun runJvmCompiler(
            sourcesToCompile: List<File>,
            javaSourceRoots: Iterable<File>,
            args: K2JVMCompilerArguments,
            messageCollector: MessageCollector,
            outputItemsCollector: OutputItemsCollector
    ): ExitCode {
        val outputDir = args.destinationAsFile
        log.debug("Removing all kotlin classes in $outputDir")
        // we're free to delete all classes since only we know about that directory
        outputDir.deleteRecursively()

        val moduleFile = makeModuleFile(
                args.moduleName,
                isTest = false,
                outputDir = outputDir,
                sourcesToCompile = sourcesToCompile,
                javaSourceRoots = javaSourceRoots,
                classpath = args.classpathAsList,
                friendDirs = args.friendPaths?.map(::File) ?: emptyList())
        args.module = moduleFile.absolutePath

        val additionalArguments = ""

        try {
            return runCompiler(K2JVM_COMPILER, args, additionalArguments, messageCollector, outputItemsCollector, GradleCompilerEnvironment(project, K2JVM_COMPILER))
        }
        finally {
            moduleFile.delete()
        }
    }

    @Synchronized
    override fun getDaemonConnection(environment: GradleCompilerEnvironment, messageCollector: MessageCollector): DaemonConnection {
        return newDaemonConnection(environment.compilerJar, messageCollector, flagFile)
    }
}

internal class GradleCompilerEnvironment(
        private val project: Project,
        private val compilerClassName: String
) : CompilerEnvironment(Services.EMPTY) {
    val compilerJar: File by lazy {
        val file = findKotlinCompilerJar(project, compilerClassName)
                ?: throw IllegalStateException("Could not found Kotlin compiler jar. " +
                        "As a workaround you may specify path to compiler jar using " +
                        "\"$KOTLIN_COMPILER_JAR_PATH_PROPERTY\" system property")

        project.logger.kotlinInfo("Using kotlin compiler jar: $file")
        file
    }

}

fun findKotlinCompilerJar(project: Project, compilerClassName: String): File? {
    fun Project.classpathJars(): Sequence<File> =
            buildscript.configurations.findByName("classpath")?.files?.asSequence() ?: emptySequence()

    val pathFromSysProperties = System.getProperty(KOTLIN_COMPILER_JAR_PATH_PROPERTY)
    if (pathFromSysProperties != null) {
        val fileFromSysProperties = File(pathFromSysProperties)

        if (fileFromSysProperties.exists()) return fileFromSysProperties
    }

    val projectsToRoot = generateSequence(project) { if (it != it.rootProject) it.parent else null }
    val classpathDeps = projectsToRoot.flatMap { it.classpathJars() }
    val entryToFind = compilerClassName.replace(".", "/") + ".class"
    val jarCandidate = classpathDeps.firstOrNull { it.hasEntry(entryToFind) }

    System.setProperty(KOTLIN_COMPILER_JAR_PATH_PROPERTY, jarCandidate?.absolutePath)
    return jarCandidate
}

private fun File.hasEntry(entryToFind: String): Boolean {
    try {
        val zip = ZipFile(this)
        val enumeration = zip.entries()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            if (entry.name.equals(entryToFind, ignoreCase = true)) return true
        }
    }
    catch (e: Exception) {
        return false
    }

    return false
}