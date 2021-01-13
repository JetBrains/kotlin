/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.PRODUCTION
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.library.*
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinJsIrLink @Inject constructor(
    objectFactory: ObjectFactory
): Kotlin2JsCompile(KotlinJsOptionsImpl(), objectFactory) {

    class Configurator(compilation: KotlinCompilationData<*>): Kotlin2JsCompile.Configurator<KotlinJsIrLink>(compilation) {

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
    internal lateinit var compilation: KotlinCompilation<*>

    // Link tasks are not affected by compiler plugin
    override val pluginClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    lateinit var mode: KotlinJsBinaryMode

    // Not check sources, only klib module
    @Internal
    override fun getSource(): FileTree = super.getSource()

    private val buildDir = project.buildDir

    private val compileClasspathConfiguration by lazy {
        project.configurations.getByName(compilation.compileDependencyConfigurationName)
    }

    @get:SkipWhenEmpty
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val entryModule: DirectoryProperty

    override fun skipCondition(): Boolean {
        return !entryModule.get().asFile.exists()
    }

    override fun setupCompilerArgs(args: K2JSCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        when (mode) {
            PRODUCTION -> {
                kotlinOptions.configureOptions(ENABLE_DCE, GENERATE_D_TS)
            }
            DEVELOPMENT -> {
                kotlinOptions.configureOptions(GENERATE_D_TS)
                CacheBuilder(
                    buildDir,
                    compileClasspathConfiguration,
                    libraryFilter,
                    compilerRunner,
                    { createCompilerArgs() },
                    computedCompilerClasspath,
                    logger,
                    objects.fileCollection(),
                    reportingSettings
                ).buildCompilerArgs()
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
    private val compileClasspath: Configuration,
    private val libraryFilter: (File) -> Boolean,
    private val compilerRunner: GradleCompilerRunner,
    private val compilerArgsFactory: () -> K2JSCompilerArguments,
    private val computedCompilerClasspath: List<File>,
    private val logger: Logger,
    private val outputFiles: FileCollection,
    private val reportingSettings: ReportingSettings

) {
    val rootCacheDirectory by lazy {
        buildDir.resolve("klib/cache")
    }

    fun buildCompilerArgs(): List<String> {
        val visitedDependencies = mutableSetOf<ResolvedDependency>()
        val allCacheDirectories = mutableSetOf<String>()

        compileClasspath.resolvedConfiguration.firstLevelModuleDependencies
            .forEach { dependency ->
                ensureDependencyCached(dependency, visitedDependencies)
                (listOf(dependency) + getAllDependencies(dependency))
                    .forEach {
                        val cacheDirectory = getCacheDirectory(rootCacheDirectory, dependency)
                        if (cacheDirectory.exists()) {
                            allCacheDirectories.add(cacheDirectory.normalize().absolutePath)
                        }
                    }
            }

        return allCacheDirectories
            .map { "-Xcache-directory=${it}" }
    }

    private fun ensureDependencyCached(
        dependency: ResolvedDependency,
        visitedDependency: MutableSet<ResolvedDependency>
    ) {
        if (dependency in visitedDependency) return
        visitedDependency.add(dependency)

        dependency.children
            .forEach { ensureDependencyCached(it, visitedDependency) }

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
            if (cacheDirectory.listFilesOrEmpty().isNotEmpty())
                continue

            val compilerArgs = compilerArgsFactory()
            compilerArgs.includes = library.file.normalize().absolutePath
            compilerArgs.outputFile = cacheDirectory.resolve("${library.name}.js").normalize().absolutePath
            compilerArgs.irProduceJs = true
            dependenciesCacheDirectories.forEach {
                compilerArgs.freeArgs += "-Xcache-directory=${it.absolutePath}"
            }

            compilerArgs.libraries = getAllDependencies(dependency)
                .flatMap { it.moduleArtifacts }
                .map { it.file }
                .filter { it.exists() && libraryFilter(it) }
                .distinct()
                .joinToString(File.pathSeparator) { it.normalize().absolutePath }

            val messageCollector = GradlePrintingMessageCollector(logger, false)
            val outputItemCollector = OutputItemsCollectorImpl()
            val environment = GradleCompilerEnvironment(
                computedCompilerClasspath,
                messageCollector,
                outputItemCollector,
                outputFiles = outputFiles,
                reportingSettings = reportingSettings
            )

            compilerRunner
                .runJsCompilerAsync(
                    emptyList(),
                    emptyList(),
                    compilerArgs,
                    environment
                )
        }
    }
}