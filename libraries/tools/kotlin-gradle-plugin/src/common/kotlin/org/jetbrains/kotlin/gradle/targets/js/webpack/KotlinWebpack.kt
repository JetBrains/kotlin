/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileResolver
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
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.distsDirectory
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWebpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.injected
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

abstract class KotlinWebpack
@Inject
constructor(
    @Internal
    @Transient
    override val compilation: KotlinJsCompilation,
    objects: ObjectFactory
) : DefaultTask(), RequiresNpmDependencies, WebpackRulesDsl {
    @Transient
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions
    private val resolutionManager = nodeJs.npmResolutionManager
    private val rootPackageDir by lazy { nodeJs.rootPackageDir }

    private val npmProject = compilation.npmProject

    private val projectPath = project.path

    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    override val rules: KotlinWebpackRulesContainer =
        project.objects.webpackRulesContainer()

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    @get:Internal
    internal abstract val buildMetricsService: Property<BuildMetricsService?>

    @get:Internal
    val metrics: Property<BuildMetricsReporter> = project.objects
        .property(BuildMetricsReporterImpl())

    @Suppress("unused")
    @get:Input
    val compilationId: String by lazy {
        compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationPurpose
        }
    }

    @Input
    var mode: Mode = Mode.DEVELOPMENT

    @get:Internal
    var entry: File
        get() = entryProperty.asFile.get()
        set(value) {
            entryProperty.set(value)
        }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    @get:NormalizeLineEndings
    val entryProperty: RegularFileProperty = objects
        .fileProperty()
        .fileProvider(
            compilation.compileTaskProvider.flatMap { it.outputFileProperty }
        )

    init {
        onlyIf {
            entry.exists()
        }
    }

    @get:Internal
    internal var resolveFromModulesFirst: Boolean = false

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    val runtimeClasspath: FileCollection by lazy {
        compilation.compileDependencyFiles
    }

    @get:OutputFile
    open val configFile: File by lazy {
        npmProject.dir.resolve("webpack.config.js")
    }

    @Input
    var saveEvaluatedConfigFile: Boolean = true

    @Nested
    val output: KotlinWebpackOutput = KotlinWebpackOutput(
        library = project.archivesName.orNull,
        libraryTarget = KotlinWebpackOutput.Target.UMD,
        globalObject = "this"
    )

    @get:Internal
    @Deprecated("use destinationDirectory instead", ReplaceWith("destinationDirectory"))
    val outputPath: File
        get() = destinationDirectory

    @get:Internal
    internal var _destinationDirectory: File? = null

    private val defaultDestinationDirectory by lazy {
        project.distsDirectory.asFile.get()
    }

    @get:Internal
    var destinationDirectory: File
        get() = _destinationDirectory ?: defaultDestinationDirectory
        set(value) {
            _destinationDirectory = value
        }

    private val defaultOutputFileName by lazy {
        project.archivesName.orNull + ".js"
    }

    @get:Internal
    var outputFileName: String by property {
        defaultOutputFileName
    }

    @get:OutputFile
    open val outputFile: File
        get() = destinationDirectory.resolve(outputFileName)

    private val projectDir = project.projectDir

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    open val configDirectory: File?
        get() = projectDir.resolve("webpack.config.d").takeIf { it.isDirectory }

    @Input
    var report: Boolean = false

    private val projectReportsDir = project.reportsDir

    open val reportDir: File
        @Internal get() = reportDirProvider.get()

    @get:OutputDirectory
    open val reportDirProvider: Provider<File> by lazy {
        entryProperty
            .map { it.asFile.nameWithoutExtension }
            .map {
                projectReportsDir.resolve("webpack").resolve(it)
            }
    }

    open val evaluatedConfigFile: File
        @Internal get() = evaluatedConfigFileProvider.get()

    open val evaluatedConfigFileProvider: Provider<File>
        @OutputFile get() = reportDirProvider.map { it.resolve("webpack.config.evaluated.js") }

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
    var devServer: KotlinWebpackConfig.DevServer? = null

    @Input
    var devtool: String = WebpackDevtool.EVAL_SOURCE_MAP

    @Incubating
    @Internal
    var generateConfigOnly: Boolean = false

    @Nested
    val synthConfig = KotlinWebpackConfig(
        rules = project.objects.webpackRulesContainer(),
    )

    @Input
    val webpackMajorVersion = PropertiesProvider(project).webpackMajorVersion

    fun webpackConfigApplier(body: KotlinWebpackConfig.() -> Unit) {
        synthConfig.body()
        webpackConfigAppliers.add(body)
    }

    private val webpackConfigAppliers: MutableList<(KotlinWebpackConfig) -> Unit> =
        mutableListOf()

    private val platformType by project.provider {
        compilation.platformType
    }

    /**
     * [forNpmDependencies] is used to avoid querying [destinationDirectory] before task execution.
     * Otherwise, Gradle will fail the build.
     */
    private fun createWebpackConfig(forNpmDependencies: Boolean = false) = KotlinWebpackConfig(
        mode = mode,
        entry = if (forNpmDependencies) null else entry,
        reportEvaluatedConfigFile = if (!forNpmDependencies && saveEvaluatedConfigFile) evaluatedConfigFile else null,
        output = output,
        outputPath = if (forNpmDependencies) null else destinationDirectory,
        outputFileName = outputFileName,
        configDirectory = configDirectory,
        bundleAnalyzerReportDir = if (!forNpmDependencies && report) reportDir else null,
        rules = rules,
        devServer = devServer,
        devtool = devtool,
        sourceMaps = sourceMaps,
        resolveFromModulesFirst = resolveFromModulesFirst,
        webpackMajorVersion = webpackMajorVersion
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
            .forEach { it(config) }

        return KotlinWebpackRunner(
            npmProject,
            logger,
            configFile,
            execHandleFactory,
            bin,
            args,
            nodeArgs,
            config
        )
    }

    override val nodeModulesRequired: Boolean
        @Internal get() = true

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = createWebpackConfig(true).getRequiredDependencies(versions)

    private val isContinuous = project.gradle.startParameter.isContinuous

    @TaskAction
    fun doExecute() {
        resolutionManager.checkRequiredDependencies(task = this)

        val runner = createRunner()

        if (generateConfigOnly) {
            runner.config.save(configFile)
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
                    progressReporterPathFilter = rootPackageDir.absolutePath
                )
            ).execute(services)

            val buildMetrics = metrics.get()
            destinationDirectory.walkTopDown()
                .filter { it.isFile }
                .filter { it.extension == "js" }
                .map { it.length() }
                .sum()
                .let {
                    buildMetrics.addMetric(BuildPerformanceMetric.BUNDLE_SIZE, it)
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
