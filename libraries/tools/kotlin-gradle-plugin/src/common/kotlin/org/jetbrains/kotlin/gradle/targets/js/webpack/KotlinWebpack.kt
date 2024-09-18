/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWebpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.archivesName
import org.jetbrains.kotlin.gradle.utils.injected
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinWebpack
@Inject
constructor(
    @Internal
    @Transient
    final override val compilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
) : DefaultTask(), RequiresNpmDependencies, WebpackRulesDsl, UsesBuildMetricsService {
    @Transient
    private val nodeJs = project.rootProject.kotlinNodeJsRootExtension
    private val versions = nodeJs.versions
    private val rootPackageDir by lazy { nodeJs.rootPackageDirectory }

    private val npmProject = compilation.npmProject

    override val rules: KotlinWebpackRulesContainer =
        project.objects.webpackRulesContainer()

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    private val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(BuildMetricsReporterImpl())

    @Suppress("unused")
    @get:Input
    val compilationId: String by lazy {
        compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationName
        }
    }

    @Input
    var mode: Mode = Mode.DEVELOPMENT

    @get:Internal
    abstract val inputFilesDirectory: DirectoryProperty

    @get:Input
    abstract val entryModuleName: Property<String>

    @get:Internal
    val npmProjectDir: Provider<File>
        get() = inputFilesDirectory.map { it.asFile.parentFile }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:NormalizeLineEndings
    val inputFiles: FileTree
        get() = objects.fileTree()
            // in webpack.config.js there is path relative to npmProjectDir (kotlin/<module>.js).
            // And we need have relative path in build cache
            // That's why we use npmProjectDir with filter instead of just inputFilesDirectory,
            // if we would use inputFilesDirectory, we will get in cache just file names,
            // and if directory is changed to kotlin2, webpack config will be invalid.
            .from(npmProjectDir)
            .matching {
                it.include { element: FileTreeElement ->
                    val inputFilesDirectory = inputFilesDirectory.get().asFile
                    element.file == inputFilesDirectory ||
                            element.file.parentFile == inputFilesDirectory
                }
            }

    @get:Input
    abstract val esModules: Property<Boolean>

    @get:Internal
    val entry: Provider<RegularFile>
        get() = inputFilesDirectory.map {
            it.file(entryModuleName.get() + if (esModules.get()) ".mjs" else ".js")
        }

    init {
        this.onlyIf {
            entry.get().asFile.exists()
        }
    }

    @get:Internal
    internal var resolveFromModulesFirst: Boolean = false

    @get:OutputFile
    open val configFile: Provider<File> =
        npmProjectDir.map { it.resolve("webpack.config.js") }

    @Nested
    val output: KotlinWebpackOutput = KotlinWebpackOutput(
        library = project.archivesName.orNull,
        libraryTarget = KotlinWebpackOutput.Target.UMD,
    )

    @get:Internal
    @Deprecated("Use `outputDirectory` instead", ReplaceWith("outputDirectory"))
    var destinationDirectory: File
        get() = outputDirectory.asFile.get()
        set(value) {
            outputDirectory.set(value)
        }

    @get:OutputDirectory
    @get:Optional
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    @Deprecated("Use `mainOutputFileName` instead", ReplaceWith("mainOutputFileName"))
    var outputFileName: String
        get() = mainOutputFileName.get()
        set(value) {
            mainOutputFileName.set(value)
        }

    @get:Internal
    abstract val mainOutputFileName: Property<String>

    @get:Internal
    @Deprecated("Use `mainOutputFile` instead", ReplaceWith("mainOutputFile"))
    open val outputFile: File
        get() = mainOutputFile.get().asFile

    @get:Internal
    val mainOutputFile: Provider<RegularFile> =
        objects.providerWithLazyConvention { outputDirectory.file(mainOutputFileName) }.flatMap { it }

    private val projectDir = project.projectDir

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    open val configDirectory: File?
        get() = projectDir.resolve("webpack.config.d").takeIf { it.isDirectory }

    @Input
    var debug: Boolean = false

    @Input
    var bin: String = "webpack/bin/webpack.js"

    @Input
    var args: MutableList<String> = mutableListOf()

    @Input
    var nodeArgs: MutableList<String> = mutableListOf()

    @Input
    var sourceMaps: Boolean = true

    @Input
    @Optional
    val devServerProperty: Property<KotlinWebpackConfig.DevServer> = project.objects.property(KotlinWebpackConfig.DevServer::class.java)

    @get:Internal
    @Deprecated(
        "This property is deprecated and will be removed in future. Use devServerProperty instead",
        replaceWith = ReplaceWith("devServerProperty")
    )
    var devServer: KotlinWebpackConfig.DevServer
        get() = devServerProperty.get()
        set(value) = devServerProperty.set(value)

    @Input
    @Optional
    var watchOptions: KotlinWebpackConfig.WatchOptions? = null

    @Input
    var devtool: String = WebpackDevtool.EVAL_SOURCE_MAP

    @Incubating
    @Internal
    var generateConfigOnly: Boolean = false

    fun webpackConfigApplier(body: Action<KotlinWebpackConfig>) {
        webpackConfigAppliers.add(body)
    }

    @get:Nested
    internal val webpackConfigAppliers: MutableList<Action<KotlinWebpackConfig>> =
        mutableListOf()

    private val platformType by project.provider {
        compilation.platformType
    }

    /**
     * [forNpmDependencies] is used to avoid querying [outputDirectory] before task execution.
     * Otherwise, Gradle will fail the build.
     */
    private fun createWebpackConfig(forNpmDependencies: Boolean = false) = KotlinWebpackConfig(
        npmProjectDir = npmProjectDir,
        mode = mode,
        entry = if (forNpmDependencies) null else entry.get().asFile,
        output = output,
        outputPath = if (forNpmDependencies) null else outputDirectory.getOrNull()?.asFile,
        outputFileName = mainOutputFileName.get(),
        configDirectory = configDirectory,
        rules = rules,
        devServer = devServerProperty.orNull,
        devtool = devtool,
        sourceMaps = sourceMaps,
        resolveFromModulesFirst = resolveFromModulesFirst,
    )

    private fun createRunner(): KotlinWebpackRunner {
        val config = createWebpackConfig()

        if (platformType == KotlinPlatformType.wasm) {
            config.experiments += listOf(
                "asyncWebAssembly",
                "topLevelAwait"
            )
        }

        webpackConfigAppliers
            .forEach { it.execute(config) }

        val webpackArgs = args.run {
            val port = devServerProperty.orNull?.port
            if (debug && port != null) plus(listOf("--port", port.toString()))
            else this
        }

        return KotlinWebpackRunner(
            npmProject,
            logger,
            configFile.get(),
            execHandleFactory,
            bin,
            webpackArgs,
            nodeArgs,
            config
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = createWebpackConfig(true).getRequiredDependencies(versions)

    private val isContinuous = project.gradle.startParameter.isContinuous

    @TaskAction
    fun doExecute() {
        val runner = createRunner()

        if (generateConfigOnly) {
            runner.config.save(configFile.get())
            return
        }

        if (isContinuous) {
            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("webpack", Handle::class.java)
            if (deploymentHandle == null) {
                deploymentRegistry.start("webpack", DeploymentRegistry.ChangeBehavior.BLOCK, Handle::class.java, runner)
            }
        } else {
            runner.copy(
                config = runner.config.copy(
                    progressReporter = true,
                    progressReporterPathFilter = rootPackageDir.getFile()
                )
            ).execute(services)

            val buildMetrics = metrics.get()
            outputDirectory.get().asFile.walkTopDown()
                .filter { it.isFile }
                .filter { it.extension == "js" }
                .map { it.length() }
                .sum()
                .let {
                    buildMetrics.addMetric(GradleBuildPerformanceMetric.BUNDLE_SIZE, it)
                }

            buildMetricsService.orNull?.also { it.addTask(path, this.javaClass, buildMetrics) }
        }
    }

    internal open class Handle @Inject constructor(val runner: KotlinWebpackRunner) : DeploymentHandle {
        var process: ExecHandle? = null

        override fun isRunning() = process != null

        override fun start(deployment: Deployment) {
            process = runner.start()
        }

        override fun stop() {
            process?.abort()
        }
    }

}
