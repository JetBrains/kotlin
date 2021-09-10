/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.copyFreeCompilerArgsToArgs
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.PRODUCTION
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.SourceRoots
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.gradle.utils.getAllDependencies
import org.jetbrains.kotlin.gradle.utils.getCacheDirectory
import org.jetbrains.kotlin.gradle.utils.getDependenciesCacheDirectories
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsIrLink @Inject constructor(
    objectFactory: ObjectFactory
) : Kotlin2JsCompile(KotlinJsOptionsImpl(), objectFactory) {

    class Configurator(compilation: KotlinCompilationData<*>) : Kotlin2JsCompile.Configurator<KotlinJsIrLink>(compilation) {

        override fun configure(task: KotlinJsIrLink) {
            super.configure(task)

            task.entryModule.fileProvider(
                (compilation as KotlinJsIrCompilation).output.classesDirs.elements.map { it.single().asFile }
            ).disallowChanges()
            task.destinationDirectory.fileProvider(task.outputFileProperty.map { it.parentFile }).disallowChanges()
        }
    }

    @Transient
    @get:Internal
    internal lateinit var compilation: KotlinCompilationData<*>

    @get:Input
    internal val incrementalJsIr: Boolean = PropertiesProvider(project).incrementalJsIr

    // Link tasks are not affected by compiler plugin
    override val pluginClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    lateinit var mode: KotlinJsBinaryMode

    // Not check sources, only klib module
    @Internal
    override fun getSource(): FileTree = super.getSource()

    private val buildDir = project.buildDir

    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val entryModule: DirectoryProperty

    override fun getDestinationDir(): File {
        return if (kotlinOptions.outputFile == null) {
            super.getDestinationDir()
        } else {
            outputFile.parentFile
        }
    }

    override fun skipCondition(): Boolean {
        return !entryModule.get().asFile.exists()
    }

    override fun callCompilerAsync(
        args: K2JSCompilerArguments,
        sourceRoots: SourceRoots,
        changedFiles: ChangedFiles,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        KotlinBuildStatsService.applyIfInitialised {
            it.report(BooleanMetrics.JS_IR_INCREMENTAL, incrementalJsIr)
        }
        if (incrementalJsIr) {
            val visitedCompilations = mutableSetOf<KotlinCompilation<*>>()
            val allCacheDirectories = mutableSetOf<File>()

            val cacheBuilder = CacheBuilder(
                buildDir,
                compilation as KotlinCompilation<*>,
                kotlinOptions,
                libraryFilter,
                compilerRunner.get(),
                { createCompilerArgs() },
                { objects.fileCollection() },
                defaultCompilerClasspath,
                logger,
                reportingSettings()
            )
            val cacheArgs = visitCompilation(
                compilation as KotlinCompilation<*>,
                cacheBuilder,
                visitedCompilations,
                allCacheDirectories
            )

            args.cacheDirectories = cacheArgs.joinToString(File.pathSeparator) {
                it.normalize().absolutePath
            }
        }
        super.callCompilerAsync(args, sourceRoots, changedFiles, taskOutputsBackup)
    }

    private fun visitCompilation(
        compilation: KotlinCompilation<*>,
        cacheBuilder: CacheBuilder,
        visitedCompilations: MutableSet<KotlinCompilation<*>>,
        visitedCacheDirectories: MutableSet<File>
    ): List<File> {
        if (compilation in visitedCompilations) return emptyList()
        visitedCompilations.add(compilation)

        val associatedCaches = compilation.associateWith
            .flatMap {
                visitCompilation(
                    it,
                    cacheBuilder,
                    visitedCompilations,
                    visitedCacheDirectories
                )
            }

        return cacheBuilder
            .buildCompilerArgs(
                project.configurations.getByName(compilation.compileDependencyConfigurationName),
                compilation.output.classesDirs,
                compilation,
                associatedCaches
            )
    }

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        when (mode) {
            PRODUCTION -> {
                kotlinOptions.configureOptions(ENABLE_DCE, GENERATE_D_TS)
            }
            DEVELOPMENT -> {
                kotlinOptions.configureOptions(GENERATE_D_TS)
            }
        }
        super.setupCompilerArgs(args, defaultsOnly, ignoreClasspathResolutionErrors)
    }

    private fun KotlinJsOptions.configureOptions(vararg additionalCompilerArgs: String) {
        freeCompilerArgs += additionalCompilerArgs.toList() +
                PRODUCE_JS +
                "$ENTRY_IR_MODULE=${entryModule.get().asFile.canonicalPath}"
    }
}

internal class CacheBuilder(
    private val buildDir: File,
    private val rootCompilation: KotlinCompilation<*>,
    private val kotlinOptions: KotlinJsOptions,
    private val libraryFilter: (File) -> Boolean,
    private val compilerRunner: GradleCompilerRunner,
    private val compilerArgsFactory: () -> K2JSCompilerArguments,
    private val objectFilesFactory: () -> FileCollection,
    private val computedCompilerClasspath: FileCollection,
    private val logger: Logger,
    private val reportingSettings: ReportingSettings
) {
    val rootCacheDirectory by lazy {
        buildDir.resolve("klib/cache")
    }

    private val visitedDependencies = mutableSetOf<ResolvedDependency>()
    private val visitedFiles = mutableSetOf<File>()
    private val visitedCacheDirectories = mutableSetOf<File>()

    private val objectFiles
        get() = objectFilesFactory()

    fun buildCompilerArgs(
        compileClasspath: Configuration,
        additionalForResolve: FileCollection?,
        compilation: KotlinCompilation<*>,
        associatedCaches: List<File>
    ): List<File> {

        val allCacheDirectories = mutableListOf<File>()
        val visitedDependenciesForCache = mutableSetOf<ResolvedDependency>()

        compileClasspath.resolvedConfiguration.firstLevelModuleDependencies
            .forEach { dependency ->
                ensureDependencyCached(
                    dependency
                )
                if (dependency !in visitedDependenciesForCache) {
                    (listOf(dependency) + getAllDependencies(dependency))
                        .filter { it !in visitedDependenciesForCache }
                        .forEach { dependencyForCache ->
                            visitedDependenciesForCache.add(dependencyForCache)
                            val cacheDirectory = getCacheDirectory(rootCacheDirectory, dependencyForCache)
                            if (cacheDirectory.exists()) {
                                allCacheDirectories.add(cacheDirectory)
                            }
                        }
                }
            }

        if (compilation != rootCompilation) {
            additionalForResolve?.files?.forEach { file ->
                val cacheDirectory = rootCacheDirectory.resolve(file.name)
                cacheDirectory.mkdirs()
                runCompiler(
                    file,
                    compileClasspath.files,
                    cacheDirectory,
                    (allCacheDirectories + associatedCaches).distinct()
                )
                allCacheDirectories.add(cacheDirectory)
            }
        }

        return associatedCaches + allCacheDirectories
            .filter { it !in visitedCacheDirectories }
            .also { visitedCacheDirectories.addAll(it) }
    }

    private fun ensureDependencyCached(
        dependency: ResolvedDependency
    ) {
        if (dependency in visitedDependencies) return
        visitedDependencies.add(dependency)

        dependency.children
            .forEach { ensureDependencyCached(it) }

        val artifactsToAddToCache = dependency.moduleArtifacts
            .filter { libraryFilter(it.file) }

        if (artifactsToAddToCache.isEmpty()) return

        val dependenciesCacheDirectories = getDependenciesCacheDirectories(
            rootCacheDirectory,
            dependency
        ) ?: return

        val cacheDirectory = getCacheDirectory(rootCacheDirectory, dependency)
        cacheDirectory.mkdirs()

        for (library in artifactsToAddToCache) {
            runCompiler(
                library.file,
                getAllDependencies(dependency)
                    .flatMap { it.moduleArtifacts }
                    .map { it.file },
                cacheDirectory,
                dependenciesCacheDirectories
            )
        }
    }

    fun runCompiler(
        file: File,
        dependencies: Collection<File>,
        cacheDirectory: File,
        dependenciesCacheDirectories: Collection<File>
    ) {
        if (file in visitedFiles) return
        val compilerArgs = compilerArgsFactory()
        kotlinOptions.copyFreeCompilerArgsToArgs(compilerArgs)
        var prevIndex: Int? = null
        compilerArgs.freeArgs = compilerArgs.freeArgs
            .filterIndexed { index, arg ->
                !listOf("-source-map-base-dirs", "-source-map-prefix").any {
                    if (prevIndex != null) {
                        prevIndex = null
                        return@any true
                    }
                    if (arg.startsWith(it)) {
                        prevIndex = index
                        return@any true
                    }

                    false
                }
            }

        compilerArgs.freeArgs = compilerArgs.freeArgs
            .filterNot { arg ->
                IGNORED_ARGS.any {
                    arg.startsWith(it)
                }
            }

        visitedFiles.add(file)
        compilerArgs.includes = file.normalize().absolutePath
        compilerArgs.outputFile = cacheDirectory.normalize().absolutePath
        if (dependenciesCacheDirectories.isNotEmpty()) {
            compilerArgs.cacheDirectories = dependenciesCacheDirectories.joinToString(File.pathSeparator)
        }
        compilerArgs.irBuildCache = true

        compilerArgs.libraries = dependencies
            .filter { it.exists() && libraryFilter(it) }
            .distinct()
            .filterNot { it == file }
            .joinToString(File.pathSeparator) { it.normalize().absolutePath }

        val messageCollector = GradlePrintingMessageCollector(logger, false)
        val outputItemCollector = OutputItemsCollectorImpl()
        val environment = GradleCompilerEnvironment(
            computedCompilerClasspath,
            messageCollector,
            outputItemCollector,
            outputFiles = objectFiles,
            reportingSettings = reportingSettings
        )

        compilerRunner
            .runJsCompilerAsync(
                emptyList(),
                emptyList(),
                compilerArgs,
                environment,
                null
            )?.await()
    }

    companion object {
        private val IGNORED_ARGS = listOf(
            ENTRY_IR_MODULE,
            PRODUCE_JS,
            PRODUCE_UNZIPPED_KLIB,
            ENABLE_DCE,
            GENERATE_D_TS
        )
    }
}