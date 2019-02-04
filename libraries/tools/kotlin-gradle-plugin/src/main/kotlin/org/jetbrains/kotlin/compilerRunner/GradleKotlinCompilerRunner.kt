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
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.newTmpFile
import org.jetbrains.kotlin.gradle.utils.relativeToRoot
import org.jetbrains.kotlin.incremental.*
import java.io.File
import java.lang.ref.WeakReference

internal const val KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY = "kotlin.compiler.execution.strategy"
internal const val DAEMON_EXECUTION_STRATEGY = "daemon"
internal const val IN_PROCESS_EXECUTION_STRATEGY = "in-process"
internal const val OUT_OF_PROCESS_EXECUTION_STRATEGY = "out-of-process"
const val CREATED_CLIENT_FILE_PREFIX = "Created client-is-alive flag file: "
const val EXISTING_CLIENT_FILE_PREFIX = "Existing client-is-alive flag file: "
const val CREATED_SESSION_FILE_PREFIX = "Created session-is-alive flag file: "
const val EXISTING_SESSION_FILE_PREFIX = "Existing session-is-alive flag file: "
const val DELETED_SESSION_FILE_PREFIX = "Deleted session-is-alive flag file: "
const val COULD_NOT_CONNECT_TO_DAEMON_MESSAGE = "Could not connect to Kotlin compile daemon"

internal fun kotlinCompilerExecutionStrategy(): String =
    System.getProperty(KOTLIN_COMPILER_EXECUTION_STRATEGY_PROPERTY) ?: DAEMON_EXECUTION_STRATEGY

internal open class GradleCompilerRunner(protected val task: Task) {
    protected val project: Project
        get() = task.project

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runJvmCompilerAsync(
        sourcesToCompile: List<File>,
        commonSources: List<File>,
        javaSourceRoots: Iterable<File>,
        javaPackagePrefix: String?,
        args: K2JVMCompilerArguments,
        environment: GradleCompilerEnvironment
    ) {
        val buildFile = makeModuleFile(
            args.moduleName!!,
            isTest = false,
            outputDir = args.destinationAsFile,
            sourcesToCompile = sourcesToCompile,
            commonSources = commonSources,
            javaSourceRoots = javaSourceRoots.map { JvmSourceRoot(it, javaPackagePrefix) },
            classpath = args.classpathAsList,
            friendDirs = args.friendPaths?.map(::File).orEmpty()
        )
        args.buildFile = buildFile.absolutePath

        if (environment.incrementalCompilationEnvironment == null || kotlinCompilerExecutionStrategy() != DAEMON_EXECUTION_STRATEGY) {
            args.destination = null
        }

        runCompilerAsync(KotlinCompilerClass.JVM, args, environment, buildFile = buildFile)
    }

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runJsCompilerAsync(
        kotlinSources: List<File>,
        kotlinCommonSources: List<File>,
        args: K2JSCompilerArguments,
        environment: GradleCompilerEnvironment
    ) {
        args.freeArgs += kotlinSources.map { it.absolutePath }
        args.commonSources = kotlinCommonSources.map { it.absolutePath }.toTypedArray()
        runCompilerAsync(KotlinCompilerClass.JS, args, environment)
    }

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runMetadataCompilerAsync(
        kotlinSources: List<File>,
        args: K2MetadataCompilerArguments,
        environment: GradleCompilerEnvironment
    ) {
        args.freeArgs += kotlinSources.map { it.absolutePath }
        runCompilerAsync(KotlinCompilerClass.METADATA, args, environment)
    }

    private fun runCompilerAsync(
        compilerClassName: String,
        compilerArgs: CommonCompilerArguments,
        environment: GradleCompilerEnvironment,
        buildFile: File? = null
    ) {
        if (compilerArgs.version) {
            task.logger.lifecycle(
                "Kotlin version " + loadCompilerVersion(environment.compilerClasspath) +
                        " (JRE " + System.getProperty("java.runtime.version") + ")"
            )
            compilerArgs.version = false
        }
        val argsArray = ArgumentUtils.convertArgumentsToStringList(compilerArgs).toTypedArray()
        val incrementalCompilationEnvironment = environment.incrementalCompilationEnvironment
        val modulesInfo = incrementalCompilationEnvironment?.let { buildModulesInfo(project.gradle) }
        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(project),
            compilerFullClasspath = environment.compilerFullClasspath,
            compilerClassName = compilerClassName,
            compilerArgs = argsArray,
            isVerbose = compilerArgs.verbose,
            incrementalCompilationEnvironment = incrementalCompilationEnvironment,
            incrementalModuleInfo = modulesInfo,
            buildFile = buildFile,
            outputFiles = environment.outputFiles.toList(),
            taskPath = task.path,
            buildReportMode = environment.buildReportMode
        )
        TaskLoggers.put(task.path, task.logger)
        runCompilerAsync(workArgs)
    }

    protected open fun runCompilerAsync(workArgs: GradleKotlinCompilerWorkArguments) {
        val kotlinCompilerRunnable = GradleKotlinCompilerWork(workArgs)
        kotlinCompilerRunnable.run()
    }

    companion object {
        @Synchronized
        internal fun getDaemonConnectionImpl(
            clientIsAliveFlagFile: File,
            sessionIsAliveFlagFile: File,
            compilerFullClasspath: List<File>,
            messageCollector: MessageCollector,
            isDebugEnabled: Boolean,
            enableAssertions: Boolean
        ): CompileServiceSession? {
            val compilerId = CompilerId.makeCompilerId(compilerFullClasspath)
            val additionalJvmParams = arrayListOf<String>()
            if (enableAssertions) {
                additionalJvmParams.add("ea")
            }
            return KotlinCompilerRunnerUtils.newDaemonConnection(
                compilerId, clientIsAliveFlagFile, sessionIsAliveFlagFile,
                messageCollector = messageCollector,
                isDebugEnabled = isDebugEnabled,
                additionalJvmParams = additionalJvmParams.toTypedArray()
            )
        }

        @Volatile
        private var cachedGradle = WeakReference<Gradle>(null)
        @Volatile
        private var cachedModulesInfo: IncrementalModuleInfo? = null

        @Synchronized
        internal fun buildModulesInfo(gradle: Gradle): IncrementalModuleInfo {
            if (cachedGradle.get() === gradle && cachedModulesInfo != null) return cachedModulesInfo!!

            val dirToModule = HashMap<File, IncrementalModuleEntry>()
            val nameToModules = HashMap<String, HashSet<IncrementalModuleEntry>>()
            val jarToClassListFile = HashMap<File, File>()
            val jarToModule = HashMap<File, IncrementalModuleEntry>()

            for (project in gradle.rootProject.allprojects) {
                project.tasks.withType(AbstractKotlinCompile::class.java).forEach { task ->
                    val module = IncrementalModuleEntry(project.path, task.moduleName, project.buildDir, task.buildHistoryFile)
                    dirToModule[task.destinationDir] = module
                    task.javaOutputDir?.let { dirToModule[it] = module }
                    nameToModules.getOrPut(module.name) { HashSet() }.add(module)

                    if (task is Kotlin2JsCompile) {
                        jarForSourceSet(project, task.sourceSetName)?.let {
                            jarToModule[it] = module
                        }
                    }
                }
                project.tasks.withType(InspectClassesForMultiModuleIC::class.java).forEach { task ->
                    jarToClassListFile[File(task.archivePath)] = task.classesListFile
                }
                project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlinExt ->
                    for (target in kotlinExt.targets) {
                        val mainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME) ?: continue
                        val kotlinTask = mainCompilation.compileKotlinTask as? AbstractKotlinCompile<*> ?: continue
                        val module = IncrementalModuleEntry(project.path, kotlinTask.moduleName, project.buildDir, kotlinTask.buildHistoryFile)
                        val jarTask = project.tasks.findByName(target.artifactsTaskName) as? AbstractArchiveTask ?: continue
                        jarToModule[jarTask.archivePath] = module
                    }
                }
            }

            return IncrementalModuleInfo(
                projectRoot = gradle.rootProject.projectDir,
                dirToModule = dirToModule,
                nameToModules = nameToModules,
                jarToClassListFile = jarToClassListFile,
                jarToModule = jarToModule
            ).also {
                cachedGradle = WeakReference(gradle)
                cachedModulesInfo = it
            }
        }

        private fun jarForSourceSet(project: Project, sourceSetName: String): File? {
            val javaConvention = project.convention.findPlugin(JavaPluginConvention::class.java)
                ?: return null
            val sourceSet = javaConvention.sourceSets.findByName(sourceSetName) ?: return null
            val jarTask = project.tasks.findByName(sourceSet.jarTaskName) as? Jar
            return jarTask?.archivePath
        }

        @Synchronized
        internal fun clearBuildModulesInfo() {
            cachedGradle = WeakReference<Gradle>(null)
            cachedModulesInfo = null
        }

        // created once per gradle instance
        // when gradle daemon dies, kotlin daemon should die too
        // however kotlin daemon (if it idles enough) can die before gradle daemon dies
        @Volatile
        private var clientIsAliveFlagFile: File? = null

        @Synchronized
        internal fun getOrCreateClientFlagFile(project: Project): File {
            val log = project.logger
            if (clientIsAliveFlagFile == null || !clientIsAliveFlagFile!!.exists()) {
                val projectName = project.rootProject.name.normalizeForFlagFile()
                clientIsAliveFlagFile = newTmpFile(prefix = "kotlin-compiler-in-$projectName-", suffix = ".alive")
                log.kotlinDebug { CREATED_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            } else {
                log.kotlinDebug { EXISTING_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            }

            return clientIsAliveFlagFile!!
        }

        private fun String.normalizeForFlagFile(): String {
            val validChars = ('a'..'z') + ('0'..'9') + "-_"
            return filter { it in validChars }
        }

        // session is created per build
        @Volatile
        private var sessionFlagFile: File? = null

        // session files are deleted at org.jetbrains.kotlin.gradle.plugin.KotlinGradleBuildServices.buildFinished
        @Synchronized
        internal fun getOrCreateSessionFlagFile(project: Project): File {
            val log = project.logger
            if (sessionFlagFile == null || !sessionFlagFile!!.exists()) {
                val sessionFilesDir = sessionsDir(project).apply { mkdirs() }
                sessionFlagFile = newTmpFile(prefix = "kotlin-compiler-", suffix = ".salive", directory = sessionFilesDir)
                log.kotlinDebug { CREATED_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeToRoot(project) }
            } else {
                log.kotlinDebug { EXISTING_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeToRoot(project) }
            }

            return sessionFlagFile!!
        }

        internal fun sessionsDir(project: Project): File =
            File(File(project.rootProject.buildDir, "kotlin"), "sessions")
    }
}

