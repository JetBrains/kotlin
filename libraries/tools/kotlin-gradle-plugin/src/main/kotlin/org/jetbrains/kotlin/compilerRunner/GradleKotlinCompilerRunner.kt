/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.ownModuleName
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTaskData
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.archivePathCompatible
import org.jetbrains.kotlin.gradle.utils.newTmpFile
import org.jetbrains.kotlin.gradle.utils.relativeOrCanonical
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
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

/*
Using real taskProvider cause "field 'taskProvider' from type 'org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner':
value 'fixed(class org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated, task ':compileKotlin')'
is not assignable to 'org.gradle.api.tasks.TaskProvider'" exception
 */
internal open class GradleCompilerRunner(protected val taskProvider: GradleCompileTaskProvider) {

    internal val pathProvider = taskProvider.path
    internal val loggerProvider = taskProvider.logger
    internal val buildDirProvider = taskProvider.buildDir
    internal val projectDirProvider = taskProvider.projectDir
    internal val projectRootDirProvider = taskProvider.rootDir
    internal val sessionDirProvider = taskProvider.sessionsDir
    internal val projectNameProvider = taskProvider.projectName
    internal val incrementalModuleInfoProvider = taskProvider.buildModulesInfo

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
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.commonSources = commonSources.map { it.absolutePath }.toTypedArray()
        args.javaSourceRoots = javaSourceRoots.map { it.absolutePath }.toTypedArray()
        args.javaPackagePrefix = javaPackagePrefix
        runCompilerAsync(KotlinCompilerClass.JVM, args, environment)
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
        environment: GradleCompilerEnvironment
    ) {
        if (compilerArgs.version) {
            loggerProvider.lifecycle(
                "Kotlin version " + loadCompilerVersion(environment.compilerClasspath) +
                        " (JRE " + System.getProperty("java.runtime.version") + ")"
            )
            compilerArgs.version = false
        }
        val argsArray = ArgumentUtils.convertArgumentsToStringList(compilerArgs).toTypedArray()

        // compilerArgs arguments may have some attributes which are overrided by freeCompilerArguments.
        // Here we perform the work which is repeated in compiler in order to obtain correct values. This extra work could be avoided when
        // compiler would report metrics by itself via JMX
        KotlinBuildStatsService.applyIfInitialised {
            if (compilerArgs is K2JVMCompilerArguments) {
                val args = K2JVMCompilerArguments()
                parseCommandLineArguments(argsArray.toList(), args)
                KotlinBuildStatsService.getInstance()?.report(BooleanMetrics.JVM_COMPILER_IR_MODE, args.useIR)
                KotlinBuildStatsService.getInstance()?.report(StringMetrics.JVM_DEFAULTS, args.jvmDefault)
            }
        }

        val incrementalCompilationEnvironment = environment.incrementalCompilationEnvironment
        val modulesInfo = incrementalCompilationEnvironment?.let { incrementalModuleInfoProvider.get().info }
        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(
                loggerProvider,
                projectDirProvider,
                buildDirProvider,
                projectNameProvider,
                projectRootDirProvider,
                sessionDirProvider
            ),
            compilerFullClasspath = environment.compilerFullClasspath,
            compilerClassName = compilerClassName,
            compilerArgs = argsArray,
            isVerbose = compilerArgs.verbose,
            incrementalCompilationEnvironment = incrementalCompilationEnvironment,
            incrementalModuleInfo = modulesInfo,
            outputFiles = environment.outputFiles.toList(),
            taskPath = pathProvider,
            buildReportMode = environment.buildReportMode,
            kotlinScriptExtensions = environment.kotlinScriptExtensions,
            allWarningsAsErrors = compilerArgs.allWarningsAsErrors
        )
        TaskLoggers.put(pathProvider, loggerProvider)
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
            isDebugEnabled: Boolean
        ): CompileServiceSession? {
            val compilerId = CompilerId.makeCompilerId(compilerFullClasspath)
            val additionalJvmParams = arrayListOf<String>()
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

                if (project.kotlinExtensionOrNull == null)
                    continue

                val isMultiplatformProject = project.multiplatformExtensionOrNull != null

                KotlinCompileTaskData.getTaskDataContainer(project).forEach { taskData ->
                    val compilation = taskData.compilation
                    val target = taskData.compilation.target
                    val module = IncrementalModuleEntry(
                        project.path,
                        compilation.ownModuleName,
                        project.buildDir,
                        taskData.buildHistoryFile
                    )
                    dirToModule[taskData.destinationDir.get()] = module

                    taskData.javaOutputDir?.let { dirToModule[it] = module }
                    nameToModules.getOrPut(module.name) { HashSet() }.add(module)

                    if (compilation.platformType == KotlinPlatformType.js) {
                        jarForSourceSet(project, compilation.name)?.let {
                            jarToModule[it] = module
                        }
                    }

                    if (compilation.isMain()) {
                        if (isMultiplatformProject) {
                            try {
                                //It could cause task reconfiguration. TODO: fix it some day if need
                                // But using project.locateTask(taskName).configure cause an Exception for call from ImmutableActionSet

                                val archiveTask = project.tasks.getByName(target.artifactsTaskName) as AbstractArchiveTask
                                jarToModule[archiveTask.archivePathCompatible.canonicalFile] = module
                            } catch (e: UnknownTaskException) {
                                //ignore not found task
                            }
                        } else {
                            if (target is KotlinWithJavaTarget<*>) {
                                val jar = project.tasks.getByName(target.artifactsTaskName) as Jar
                                jarToClassListFile[jar.archivePathCompatible.canonicalFile] = target.defaultArtifactClassesListFile.get()
                            }
                        }
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
            return jarTask?.archiveFile?.get()?.asFile
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
        internal fun getOrCreateClientFlagFile(log: Logger, projectName: String): File {
            if (clientIsAliveFlagFile == null || !clientIsAliveFlagFile!!.exists()) {
                clientIsAliveFlagFile = newTmpFile(prefix = "kotlin-compiler-in-${projectName}-", suffix = ".alive")
                log.kotlinDebug { CREATED_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            } else {
                log.kotlinDebug { EXISTING_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.canonicalPath }
            }

            return clientIsAliveFlagFile!!
        }

        internal fun String.normalizeForFlagFile(): String {
            val validChars = ('a'..'z') + ('0'..'9') + "-_"
            return filter { it in validChars }
        }

        // session is created per build
        @Volatile
        private var sessionFlagFile: File? = null

        // session files are deleted at org.jetbrains.kotlin.gradle.plugin.KotlinGradleBuildServices.buildFinished
        @Synchronized
        internal fun getOrCreateSessionFlagFile(log: Logger, sessionsDir: File, projectRootDir: File): File {
            if (sessionFlagFile == null || !sessionFlagFile!!.exists()) {
                val sessionFilesDir = sessionsDir.apply { mkdirs() }
                sessionFlagFile = newTmpFile(prefix = "kotlin-compiler-", suffix = ".salive", directory = sessionFilesDir)
                log.kotlinDebug { CREATED_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeOrCanonical(projectRootDir) }
            } else {
                log.kotlinDebug { EXISTING_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeOrCanonical(projectRootDir) }
            }

            return sessionFlagFile!!
        }

        internal fun sessionsDir(project: Project): File =
            File(File(project.rootProject.buildDir, "kotlin"), "sessions")
    }
}

