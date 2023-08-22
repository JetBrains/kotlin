/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.btapi.GradleBuildToolsApiCompilerRunner
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.filterExtractProps
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.BuildFinishedListenerService
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdService
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.lang.ref.WeakReference


const val CREATED_CLIENT_FILE_PREFIX = "Created client-is-alive flag file: "
const val EXISTING_CLIENT_FILE_PREFIX = "Existing client-is-alive flag file: "
const val CREATED_SESSION_FILE_PREFIX = "Created session-is-alive flag file: "
const val EXISTING_SESSION_FILE_PREFIX = "Existing session-is-alive flag file: "
const val DELETED_SESSION_FILE_PREFIX = "Deleted session-is-alive flag file: "
const val COULD_NOT_CONNECT_TO_DAEMON_MESSAGE = "Could not connect to Kotlin compile daemon"

internal fun createGradleCompilerRunner(
    taskProvider: GradleCompileTaskProvider,
    toolsJar: File?,
    compilerExecutionSettings: CompilerExecutionSettings,
    buildMetricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    workerExecutor: WorkerExecutor,
    runViaBuildToolsApi: Boolean,
    cachedClassLoadersService: Property<ClassLoadersCachingBuildService>,
    buildFinishedListenerService: Provider<BuildFinishedListenerService>,
    buildIdService: Provider<BuildIdService>,
): GradleCompilerRunner {
    return if (runViaBuildToolsApi) {
        GradleBuildToolsApiCompilerRunner(
            taskProvider,
            toolsJar,
            compilerExecutionSettings,
            buildMetricsReporter,
            workerExecutor,
            cachedClassLoadersService,
            buildFinishedListenerService,
            buildIdService,
        )
    } else {
        GradleCompilerRunnerWithWorkers(
            taskProvider,
            toolsJar,
            compilerExecutionSettings,
            buildMetricsReporter,
            workerExecutor,
        )
    }
}

/*
Using real taskProvider cause "field 'taskProvider' from type 'org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner':
value 'fixed(class org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated, task ':compileKotlin')'
is not assignable to 'org.gradle.api.tasks.TaskProvider'" exception
 */
internal open class GradleCompilerRunner(
    protected val taskProvider: GradleCompileTaskProvider,
    protected val jdkToolsJar: File?,
    protected val compilerExecutionSettings: CompilerExecutionSettings,
    protected val buildMetrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
) {

    internal val pathProvider = taskProvider.path.get()
    internal val loggerProvider = taskProvider.logger.get()
    internal val buildDirProvider = taskProvider.buildDir.get().asFile
    internal val projectDirProvider = taskProvider.projectDir.get()
    internal val projectCacheDirProvider = taskProvider.projectCacheDir.get()
    internal val sessionDirProvider = taskProvider.sessionsDir.get()
    internal val projectNameProvider = taskProvider.projectName.get()
    internal val incrementalModuleInfoProvider = taskProvider.buildModulesInfo
    internal val errorsFile = taskProvider.errorsFile.get()

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runJvmCompilerAsync(
        args: K2JVMCompilerArguments,
        environment: GradleCompilerEnvironment,
        jdkHome: File,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue? {
        if (args.jdkHome == null && !args.noJdk) args.jdkHome = jdkHome.absolutePath
        loggerProvider.kotlinInfo("Kotlin compilation 'jdkHome' argument: ${args.jdkHome}")
        return runCompilerAsync(KotlinCompilerClass.JVM, args, environment, taskOutputsBackup)
    }

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runJsCompilerAsync(
        args: K2JSCompilerArguments,
        environment: GradleCompilerEnvironment,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue? {
        return runCompilerAsync(KotlinCompilerClass.JS, args, environment, taskOutputsBackup)
    }

    /**
     * Compiler might be executed asynchronously. Do not do anything requiring end of compilation after this function is called.
     * @see [GradleKotlinCompilerWork]
     */
    fun runMetadataCompilerAsync(
        args: K2MetadataCompilerArguments,
        environment: GradleCompilerEnvironment
    ): WorkQueue? {
        return runCompilerAsync(KotlinCompilerClass.METADATA, args, environment)
    }

    private fun runCompilerAsync(
        compilerClassName: String,
        compilerArgs: CommonCompilerArguments,
        environment: GradleCompilerEnvironment,
        taskOutputsBackup: TaskOutputsBackup? = null
    ): WorkQueue? {
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
            when (compilerArgs) {
                is K2JVMCompilerArguments -> {
                    KotlinBuildStatsService.getInstance()?.apply {
                        val args = K2JVMCompilerArguments()
                        parseCommandLineArguments(argsArray.toList(), args)
                        report(StringMetrics.JVM_DEFAULTS, args.jvmDefault)
                        report(StringMetrics.USE_FIR, args.useK2.toString())

                        val pluginPatterns = listOf(Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_ALL_OPEN, "kotlin-allopen-.*jar"),
                                                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_NO_ARG, "kotlin-noarg-.*jar"),
                                                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_SAM_WITH_RECEIVER, "kotlin-sam-with-receiver-.*jar"),
                                                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_LOMBOK, "kotlin-lombok-.*jar"),
                                                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_PARSELIZE, "kotlin-parcelize-compiler-.*jar"),
                                                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_ATOMICFU, "atomicfu-.*jar")
                        )
                        val pluginJars = args.pluginClasspaths?.map { it.replace("\\", "/").split("/").last() }
                        if (pluginJars != null) {
                            for (pluginPattern in pluginPatterns) {
                                if (pluginJars.any { it.matches(pluginPattern.second.toRegex())}) {
                                    report(pluginPattern.first, true)
                                }
                            }
                        }
                    }
                }
                is K2JSCompilerArguments -> {
                    KotlinBuildStatsService.getInstance()?.apply {
                        val args = K2JSCompilerArguments()
                        parseCommandLineArguments(argsArray.toList(), args)
                        if (!args.isPreIrBackendDisabled() || args.irProduceJs) {
                            report(BooleanMetrics.JS_SOURCE_MAP, args.sourceMap)
                        }
                        if (args.irProduceJs) {
                            report(StringMetrics.JS_PROPERTY_LAZY_INITIALIZATION, args.irPropertyLazyInitialization.toString())
                        }
                    }
                }
            }
        }

        val incrementalCompilationEnvironment = environment.incrementalCompilationEnvironment
        val modulesInfo = incrementalCompilationEnvironment?.let { incrementalModuleInfoProvider.orNull?.info }
        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(
                loggerProvider,
                projectDirProvider,
                buildDirProvider,
                projectNameProvider,
                projectCacheDirProvider,
                sessionDirProvider
            ),
            compilerFullClasspath = environment.compilerFullClasspath(jdkToolsJar),
            compilerClassName = compilerClassName,
            compilerArgs = argsArray,
            isVerbose = compilerArgs.verbose,
            incrementalCompilationEnvironment = incrementalCompilationEnvironment,
            incrementalModuleInfo = modulesInfo,
            outputFiles = environment.outputFiles.toList(),
            taskPath = pathProvider,
            reportingSettings = environment.reportingSettings,
            kotlinScriptExtensions = environment.kotlinScriptExtensions,
            allWarningsAsErrors = compilerArgs.allWarningsAsErrors,
            compilerExecutionSettings = compilerExecutionSettings,
            errorsFile = errorsFile,
            kotlinPluginVersion = getKotlinPluginVersion(loggerProvider),
            //no need to log warnings in MessageCollector hear it will be logged by compiler
            kotlinLanguageVersion = parseLanguageVersion(compilerArgs.languageVersion, compilerArgs.useK2)
        )
        TaskLoggers.put(pathProvider, loggerProvider)
        return runCompilerAsync(
            workArgs,
            taskOutputsBackup
        )
    }

    protected open fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue? {
        try {
            buildMetrics.addTimeMetric(GradleBuildPerformanceMetric.CALL_WORKER)
            val kotlinCompilerRunnable = GradleKotlinCompilerWork(workArgs)
            kotlinCompilerRunnable.run()
        } catch (e: FailedCompilationException) {
            // Restore outputs only for CompilationErrorException or OOMErrorException (see GradleKotlinCompilerWorkAction.execute)
            if (taskOutputsBackup != null && (e is CompilationErrorException || e is OOMErrorException)) {
                buildMetrics.measure(GradleBuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
                    taskOutputsBackup.restoreOutputs()
                }
            }

            throw e
        }

        return null
    }

    companion object {
        @Synchronized
        internal fun getDaemonConnectionImpl(
            clientIsAliveFlagFile: File,
            sessionIsAliveFlagFile: File,
            compilerFullClasspath: List<File>,
            messageCollector: MessageCollector,
            daemonJvmArgs: List<String>?,
            isDebugEnabled: Boolean
        ): CompileServiceSession? {
            val compilerId = CompilerId.makeCompilerId(compilerFullClasspath)
            val daemonJvmOptions = configureDaemonJVMOptions(
                inheritMemoryLimits = true,
                inheritOtherJvmOptions = false,
                inheritAdditionalProperties = true
            ).also { opts ->
                if (!daemonJvmArgs.isNullOrEmpty()) {
                    opts.jvmParams.addAll(
                        daemonJvmArgs.filterExtractProps(opts.mappers, "", opts.restMapper)
                    )
                }
            }

            return KotlinCompilerRunnerUtils.newDaemonConnection(
                compilerId, clientIsAliveFlagFile, sessionIsAliveFlagFile,
                messageCollector = messageCollector,
                isDebugEnabled = isDebugEnabled,
                daemonJVMOptions = daemonJvmOptions
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
            val jarToAbiSnapshot = HashMap<File, File>()

            val multiplatformProjectTasks = mutableMapOf<Project, MutableSet<String>>()

            gradle.taskGraph.allTasks.forEach { task ->
                val project = task.project

                /*
                Ignoring isolated classpath:
                An inaccessible project is not a fatal issue here. Missing information will "only" lead to non-incremental
                compilation. We expect the user to be warned about this misconfiguration during configuration phase.
                 */
                val multiplatformExtension = try {
                    project.multiplatformExtensionOrNull
                } catch (e: IsolatedKotlinClasspathClassCastException) {
                    null
                }

                if (multiplatformExtension != null) {
                    // Just record this, we'll process them later
                    val tasksInProject = multiplatformProjectTasks[project] ?: mutableSetOf()
                    tasksInProject.add(task.name)
                    multiplatformProjectTasks[project] = tasksInProject
                }

                if (task is AbstractKotlinCompile<*>) {
                    val module = IncrementalModuleEntry(
                        project.path,
                        task.ownModuleName.get(),
                        project.buildDir,
                        task.buildHistoryFile.get().asFile,
                        task.abiSnapshotFile.get().asFile
                    )
                    dirToModule[task.destinationDirectory.get().asFile] = module
                    task.javaOutputDir.orNull?.asFile?.let { dirToModule[it] = module }
                    nameToModules.getOrPut(module.name) { HashSet() }.add(module)

                    if (task is Kotlin2JsCompile) {
                        (jarForJavaSourceSet(project, task.sourceSetName.get()) ?: jarForSingleTargetJs(project, task.sourceSetName.get()))?.let {
                            jarToModule[it] = module
                        }
                    }
                } else if (task is InspectClassesForMultiModuleIC) {
                    jarToClassListFile[File(task.archivePath.get())] = task.classesListFile.get().asFile
                }
            }

            for ((project, tasksInProject) in multiplatformProjectTasks) {
                project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlinExt ->
                    for (target in kotlinExt.targets) {
                        val mainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME) ?: continue

                        if (mainCompilation.compileKotlinTaskName !in tasksInProject || target.artifactsTaskName !in tasksInProject) {
                            // tasks are not part of the task graph, skip
                            continue
                        }

                        val kotlinTask = mainCompilation.compileKotlinTask as? AbstractKotlinCompile<*> ?: continue
                        val module = IncrementalModuleEntry(
                            project.path,
                            kotlinTask.ownModuleName.get(),
                            project.buildDir,
                            kotlinTask.buildHistoryFile.get().asFile,
                            kotlinTask.abiSnapshotFile.get().asFile
                        )
                        val jarTask = project.tasks.findByName(target.artifactsTaskName) as? AbstractArchiveTask ?: continue
                        jarToModule[jarTask.archivePathCompatible.normalize().absoluteFile] = module
                        if (target is KotlinWithJavaTarget<*, *>) {
                            val jar = project.tasks.getByName(target.artifactsTaskName) as Jar
                            jarToClassListFile[jar.archivePathCompatible.normalize().absoluteFile] =
                                target.defaultArtifactClassesListFile.get()
                            //configure abiSnapshot mapping for jars
                            jarToAbiSnapshot[jar.archivePathCompatible.normalize().absoluteFile] =
                                target.buildDir.get().file(kotlinTask.abiSnapshotRelativePath).get().asFile
                        }

                    }
                }
            }

            return IncrementalModuleInfo(
                rootProjectBuildDir = gradle.rootProject.buildDir,
                dirToModule = dirToModule,
                nameToModules = nameToModules,
                jarToClassListFile = jarToClassListFile,
                jarToModule = jarToModule,
                jarToAbiSnapshot = jarToAbiSnapshot
            ).also {
                cachedGradle = WeakReference(gradle)
                cachedModulesInfo = it
            }
        }

        private fun jarForJavaSourceSet(
            project: Project,
            sourceSetName: String
        ): File? {
            val sourceSets = project.variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSetsIfAvailable ?: return null
            val sourceSet = sourceSets.findByName(sourceSetName) ?: return null

            val jarTask = project.tasks.findByName(sourceSet.jarTaskName) as? Jar
            return jarTask?.archiveFile?.get()?.asFile
        }

        private fun jarForSingleTargetJs(
            project: Project,
            sourceSetName: String,
        ): File? {
            if (sourceSetName != KotlinCompilation.MAIN_COMPILATION_NAME) return null
            val jarTaskName = project.extensions.findByType<KotlinJsProjectExtension>()?.js()?.artifactsTaskName

            val jarTask = jarTaskName?.let { project.tasks.findByName(jarTaskName) } as? Zip
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
                log.kotlinDebug { CREATED_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.normalize().absoluteFile }
            } else {
                log.kotlinDebug { EXISTING_CLIENT_FILE_PREFIX + clientIsAliveFlagFile!!.normalize().absoluteFile }
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
        internal fun getOrCreateSessionFlagFile(log: Logger, sessionsDir: File, projectCacheDirProvider: File): File {
            if (sessionFlagFile == null || !sessionFlagFile!!.exists()) {
                val sessionFilesDir = sessionsDir.apply { mkdirs() }
                sessionFlagFile = newTmpFile(prefix = "kotlin-compiler-", suffix = ".salive", directory = sessionFilesDir)
                log.kotlinDebug { CREATED_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeOrAbsolute(projectCacheDirProvider) }
            } else {
                log.kotlinDebug { EXISTING_SESSION_FILE_PREFIX + sessionFlagFile!!.relativeOrAbsolute(projectCacheDirProvider) }
            }

            return sessionFlagFile!!
        }

        internal fun sessionsDir(projectCacheDir: File): File =
            File(File(projectCacheDir, "kotlin"), "sessions")
    }
}

