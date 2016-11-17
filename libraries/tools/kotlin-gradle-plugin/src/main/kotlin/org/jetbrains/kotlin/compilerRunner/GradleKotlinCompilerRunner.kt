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

internal const val KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY = "kotlin.compiler.execution.strategy"
internal const val DAEMON_EXECUTION_STRATEGY = "daemon"
internal const val IN_PROCESS_EXECUTION_STRATEGY = "in-process"
internal const val OUT_OF_PROCESS_EXECUTION_STRATEGY = "out-of-process"

internal const val KOTLIN_COMPILER_JAR_PATH_PROPERTY = "kotlin.compiler.jar.path"

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

    override fun doRunCompiler(compilerClassName: String, argsArray: Array<String>, environment: GradleCompilerEnvironment, messageCollector: MessageCollector, collector: OutputItemsCollector): ExitCode {
        val executionStrategy = System.getProperty(KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY) ?: DAEMON_EXECUTION_STRATEGY
        if (executionStrategy == DAEMON_EXECUTION_STRATEGY) {
            val daemonExitCode = compileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector)

            if (daemonExitCode != null) {
                return daemonExitCode
            }
            else {
                log.warn("Could not connect to kotlin daemon. Using fallback strategy.")
            }
        }

        val isGradleDaemonUsed = System.getProperty("org.gradle.daemon")?.let(String::toBoolean)
        return if (executionStrategy == IN_PROCESS_EXECUTION_STRATEGY || isGradleDaemonUsed == false) {
            compileInProcess(argsArray, collector, compilerClassName, environment, messageCollector)
        }
        else {
            compileOutOfProcess(argsArray, compilerClassName, environment)
        }
    }

    override fun compileWithDaemon(compilerClassName: String, argsArray: Array<String>, environment: GradleCompilerEnvironment, messageCollector: MessageCollector, collector: OutputItemsCollector, retryOnConnectionError: Boolean): ExitCode? {
        val exitCode = super.compileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector, retryOnConnectionError)
        exitCode?.let {
            logFinish(DAEMON_EXECUTION_STRATEGY)
        }
        return exitCode
    }

    private fun compileOutOfProcess(
            argsArray: Array<String>,
            compilerClassName: String,
            environment: GradleCompilerEnvironment
    ): ExitCode {
        val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val classpathString = environment.compilerClasspath.map {it.absolutePath}.joinToString(separator = File.pathSeparator)
        val builder = ProcessBuilder(javaBin, "-cp", classpathString, compilerClassName, *argsArray)
        val process = builder.start()

        // important to read inputStream, otherwise the process may hang on some systems
        val readErrThread = thread {
            process.errorStream!!.bufferedReader().forEachLine {
                System.err.println(it)
            }
        }
        process.inputStream!!.bufferedReader().forEachLine {
            System.out.println(it)
        }
        readErrThread.join()

        val exitCode = process.waitFor()
        logFinish(OUT_OF_PROCESS_EXECUTION_STRATEGY)
        return exitCodeFromProcessExitCode(exitCode)
    }

    private fun compileInProcess(
            argsArray: Array<String>,
            collector: OutputItemsCollector,
            compilerClassName: String,
            environment: GradleCompilerEnvironment,
            messageCollector: MessageCollector
    ): ExitCode {
        val stream = ByteArrayOutputStream()
        val out = PrintStream(stream)
        // todo: cache classloader?
        val classLoader = ParentLastURLClassLoader(environment.compilerClasspathURLs, this.javaClass.classLoader)
        val servicesClass = Class.forName(Services::class.java.canonicalName, true, classLoader)
        val emptyServices = servicesClass.getField("EMPTY").get(servicesClass)
        val compiler = Class.forName(compilerClassName, true, classLoader)

        val exec = compiler.getMethod(
                "execAndOutputXml",
                PrintStream::class.java,
                servicesClass,
                Array<String>::class.java
        )

        val res = exec.invoke(compiler.newInstance(), out, emptyServices, argsArray)
        val exitCode = ExitCode.valueOf(res.toString())
        processCompilerOutput(messageCollector, collector, stream, exitCode)
        logFinish(IN_PROCESS_EXECUTION_STRATEGY)
        return exitCode
    }

    private fun logFinish(strategy: String) {
        log.debug("Finished executing kotlin compiler using $strategy strategy")
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

    val compilerClasspath: List<File>
        get() = listOf(compilerJar).filterNotNull()

    val compilerClasspathURLs: List<URL>
        get() = compilerClasspath.map { it.toURI().toURL() }
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