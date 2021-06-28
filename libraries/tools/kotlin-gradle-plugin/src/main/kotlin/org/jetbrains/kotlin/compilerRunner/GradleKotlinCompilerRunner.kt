/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.internal.build.IncludedBuildState
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import org.jetbrains.kotlin.gradle.tasks.InspectClassesForMultiModuleIC
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.archivePathCompatible
import org.jetbrains.kotlin.gradle.utils.newTmpFile
import org.jetbrains.kotlin.gradle.utils.relativeOrCanonical
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.mergeIncrementalModuleInfo
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.*
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
internal open class GradleCompilerRunner(
    protected val taskProvider: GradleCompileTaskProvider,
    protected val jdkToolsJar: File?
) {

    internal val pathProvider = taskProvider.path.get()
    internal val loggerProvider = taskProvider.logger.get()
    internal val buildDirProvider = taskProvider.buildDir.get().asFile
    internal val projectDirProvider = taskProvider.projectDir.get()
    internal val projectRootDirProvider = taskProvider.rootDir.get()
    internal val sessionDirProvider = taskProvider.sessionsDir.get()
    internal val projectNameProvider = taskProvider.projectName.get()
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
        environment: GradleCompilerEnvironment,
        jdkHome: File
    ) {
        args.freeArgs += sourcesToCompile.map { it.absolutePath }
        args.commonSources = commonSources.map { it.absolutePath }.toTypedArray()
        args.javaSourceRoots = javaSourceRoots.map { it.absolutePath }.toTypedArray()
        args.javaPackagePrefix = javaPackagePrefix
        if (args.jdkHome == null) args.jdkHome = jdkHome.absolutePath
        loggerProvider.kotlinInfo("Kotlin compilation 'jdkHome' argument: ${args.jdkHome}")
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
                KotlinBuildStatsService.getInstance()?.apply {
                    val args = K2JVMCompilerArguments()
                    parseCommandLineArguments(argsArray.toList(), args)
                    report(BooleanMetrics.JVM_COMPILER_IR_MODE, args.useIR)
                    report(StringMetrics.JVM_DEFAULTS, args.jvmDefault)
                    report(StringMetrics.USE_OLD_BACKEND, args.useOldBackend.toString())
                }
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
            val jarToAbiSnapshot = HashMap<File, File>()

            val multiplatformProjectTasks = mutableMapOf<Project, MutableSet<String>>()

            gradle.taskGraph.allTasks.forEach { task ->
                val project = task.project
                if (project.multiplatformExtensionOrNull != null) {
                    // Just record this, we'll process them later
                    val tasksInProject = multiplatformProjectTasks[project] ?: mutableSetOf()
                    tasksInProject.add(task.name)
                    multiplatformProjectTasks[project] = tasksInProject
                }

                if (task is AbstractKotlinCompile<*>) {
                    val module = IncrementalModuleEntry(
                        project.path,
                        task.moduleName.get(),
                        project.buildDir,
                        task.buildHistoryFile.get().asFile,
                        task.abiSnapshotFile.get().asFile
                    )
                    dirToModule[task.destinationDir] = module
                    task.javaOutputDir.orNull?.asFile?.let { dirToModule[it] = module }
                    nameToModules.getOrPut(module.name) { HashSet() }.add(module)

                    if (task is Kotlin2JsCompile) {
                        jarForSourceSet(project, task.sourceSetName.get())?.let {
                            jarToModule[it] = module
                        }
                    }
//                    for (target in task.targets) {
//                        if (target is KotlinWithJavaTarget<*>) {
//                            val jar = project.tasks.getByName(target.artifactsTaskName) as Jar
//                            jarToClassListFile[jar.archivePathCompatible.canonicalFile] = target.defaultArtifactClassesListFile.get()
//                            //configure abiSnapshot mapping for jars
//                            jarToAbiSnapshot[jar.archivePathCompatible.canonicalFile] =
//                                target.buildDir.get().file(task.abiSnapshotRelativePath).asFile
//                        }
//                    }
                } else if (task is InspectClassesForMultiModuleIC) {
                    jarToClassListFile[File(task.archivePath.get())] = task.classesListFile
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
                            kotlinTask.moduleName.get(),
                            project.buildDir,
                            kotlinTask.buildHistoryFile.get().asFile,
                            kotlinTask.abiSnapshotFile.get().asFile
                        )
                        val jarTask = project.tasks.findByName(target.artifactsTaskName) as? AbstractArchiveTask ?: continue
                        jarToModule[jarTask.archivePathCompatible.canonicalFile] = module
                        if (target is KotlinWithJavaTarget<*>) {
                            val jar = project.tasks.getByName(target.artifactsTaskName) as Jar
                            jarToClassListFile[jar.archivePathCompatible.canonicalFile] = target.defaultArtifactClassesListFile.get()
                            //configure abiSnapshot mapping for jars
                            jarToAbiSnapshot[jar.archivePathCompatible.canonicalFile] =
                                target.buildDir.get().file(kotlinTask.abiSnapshotRelativePath).get().asFile
                        }

                    }
                }
            }

            val incrementalModuleInfo = IncrementalModuleInfo(
                projectRoot = gradle.rootProject.projectDir,
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

            var mergedIncrementalModuleInfo = incrementalModuleInfo

            if (gradle.rootProject.gradle.includedBuilds.isNotEmpty()) {
                val includedBuilds = gradle.rootProject.gradle.includedBuilds
                    .filter { it is IncludedBuildState }
                    .map { it as IncludedBuildState }

                includedBuilds.forEach { build ->
                    deserializeIncrementalModule(build.configuredBuild)?.apply {
                        mergedIncrementalModuleInfo = mergeIncrementalModuleInfo(
                            mainModuleInfo = mergedIncrementalModuleInfo,
                            includedBuildModuleInfo = this
                        )
                    }
                }
            }

            return mergedIncrementalModuleInfo.also {
                cachedGradle = WeakReference(gradle)
                cachedModulesInfo = it
                serializeIncrementalModule(gradle, it)
            }
        }

        private const val INCREMENTAL_TASK_DATA = "kotlin.incremental.taskdata.incremental-module-info"

        // Since the kotlin-gradle-plugin in main-build and included-build are not loaded by the same classloader.
        // We need to serialize IncrementalModuleInfo to memory to avoid class cast error.
        private fun serializeIncrementalModule(gradle: Gradle, incrementalModuleInfo: IncrementalModuleInfo) {
            val ext = gradle.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java)
            val output = ByteArrayOutputStream()

            ObjectOutputStream(output).writeObject(incrementalModuleInfo)
            ext.set(INCREMENTAL_TASK_DATA, output.toByteArray())
        }

        private fun deserializeIncrementalModule(gradle: Gradle): IncrementalModuleInfo? {
            val ext = gradle.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java)
            if (!ext.has(INCREMENTAL_TASK_DATA)) {
                return null
            }
            val input = ByteArrayInputStream(ext.get(INCREMENTAL_TASK_DATA) as ByteArray)
            return ObjectInputStream(input).readObject() as? IncrementalModuleInfo
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

        internal fun sessionsDir(rootProjectBuildDir: File): File =
            File(File(rootProjectBuildDir, "kotlin"), "sessions")
    }
}

