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
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.injected
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

open class KotlinWebpack
@Inject
constructor(
    @Internal
    override val compilation: KotlinJsCompilation
) : DefaultTask(), RequiresNpmDependencies {
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    @Suppress("unused")
    val compilationId: String
        @Input get() = compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationName
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
    val entryProperty: RegularFileProperty = project.newFileProperty {
        compilation.compileKotlinTask.outputFile
    }

    init {
        onlyIf {
            entry.exists()
        }
    }

    @get:Internal
    internal var resolveFromModulesFirst: Boolean = false

    @Suppress("unused")
    val runtimeClasspath: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles

    open val configFile: File
        @OutputFile get() = compilation.npmProject.dir.resolve("webpack.config.js")

    @Input
    var saveEvaluatedConfigFile: Boolean = true

    @Nested
    val output: KotlinWebpackOutput = KotlinWebpackOutput(
        library = baseConventions?.archivesBaseName,
        libraryTarget = KotlinWebpackOutput.Target.UMD
    )

    @get:Internal
    @Deprecated("use destinationDirectory instead", ReplaceWith("destinationDirectory"))
    val outputPath: File
        get() = destinationDirectory

    private val baseConventions: BasePluginConvention?
        get() = project.convention.plugins["base"] as BasePluginConvention?

    @get:Internal
    internal var _destinationDirectory: File? = null

    @get:Internal
    var destinationDirectory: File
        get() = _destinationDirectory ?: project.buildDir.resolve(baseConventions!!.distsDirName)
        set(value) {
            _destinationDirectory = value
        }

    @get:Internal
    var outputFileName: String by property {
        baseConventions?.archivesBaseName + ".js"
    }

    @get:OutputFile
    open val outputFile: File
        get() = destinationDirectory.resolve(outputFileName)

    open val configDirectory: File?
        @Optional @InputDirectory get() = project.projectDir.resolve("webpack.config.d").takeIf { it.isDirectory }

    @Input
    var report: Boolean = false

    open val reportDir: File
        @Internal get() = reportDirProvider.get()

    open val reportDirProvider: Provider<File>
        @OutputDirectory get() = entryProperty
            .map { it.asFile.nameWithoutExtension }
            .map {
                project.reportsDir.resolve("webpack").resolve(it)
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

    @Nested
    val cssSupport: KotlinWebpackCssSupport = KotlinWebpackCssSupport()

    @Input
    @Optional
    var devServer: KotlinWebpackConfig.DevServer? = null

    @Input
    var devtool: String = WebpackDevtool.EVAL_SOURCE_MAP

    @Incubating
    @Internal
    var generateConfigOnly: Boolean = false

    private fun createRunner() = KotlinWebpackRunner(
        compilation.npmProject,
        configFile,
        execHandleFactory,
        bin,
        args,
        nodeArgs,
        KotlinWebpackConfig(
            mode = mode,
            entry = entry,
            reportEvaluatedConfigFile = if (saveEvaluatedConfigFile) evaluatedConfigFile else null,
            output = output,
            outputPath = destinationDirectory,
            outputFileName = outputFileName,
            configDirectory = configDirectory,
            bundleAnalyzerReportDir = if (report) reportDir else null,
            cssSupport = cssSupport,
            devServer = devServer,
            devtool = devtool,
            sourceMaps = sourceMaps,
            resolveFromModulesFirst = resolveFromModulesFirst
        )
    )

    override val nodeModulesRequired: Boolean
        @Internal get() = true

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = createRunner().config.getRequiredDependencies(versions)

    @TaskAction
    fun doExecute() {
        nodeJs.npmResolutionManager.checkRequiredDependencies(this)

        val runner = createRunner()

        if (generateConfigOnly) {
            runner.config.save(configFile)
            return
        }

        if (project.gradle.startParameter.isContinuous) {
            val continuousRunner = runner

            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("webpack", Handle::class.java)
            if (deploymentHandle == null) {
                deploymentRegistry.start("webpack", DeploymentRegistry.ChangeBehavior.BLOCK, Handle::class.java, continuousRunner)
            }
        } else {
            runner.copy(
                config = runner.config.copy(
                    progressReporter = true,
                    progressReporterPathFilter = nodeJs.rootPackageDir.absolutePath
                )
            ).execute()
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
