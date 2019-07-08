/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.api.tasks.*
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.injected
import java.io.File
import javax.inject.Inject

open class KotlinWebpack : DefaultTask(), RequiresNpmDependencies {
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    @Internal
    override lateinit var compilation: KotlinJsCompilation

    @Suppress("unused")
    val compilationId: String
        @Input get() = compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationName
        }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    val entry: File
        get() = compilation.compileKotlinTask.outputFile

    @Suppress("unused")
    val runtimeClasspath: FileCollection
        @InputFiles get() = compilation.compileDependencyFiles

    open val configFile: File
        @OutputFile get() = compilation.npmProject.dir.resolve("webpack.config.js")

    @Input
    var saveEvaluatedConfigFile: Boolean = true

    @get:Internal
    @Deprecated("use destinationDirectory instead", ReplaceWith("destinationDirectory"))
    val outputPath: File
        get() = destinationDirectory!!

    private val baseConventions: BasePluginConvention?
        get() = project.convention.plugins["base"] as BasePluginConvention?

    @get:Internal
    var destinationDirectory: File? = null
        get() = field ?: project.buildDir.resolve(baseConventions!!.distsDirName)

    @get:Internal
    var archiveBaseName: String? = null
        get() = field ?: baseConventions?.archivesBaseName

    @get:Internal
    var archiveAppendix: String? = null

    @get:Internal
    var archiveVersion: String? = null
        get() = field ?: project.version.toString()

    @get:Internal
    var archiveClassifier: String? = null

    @get:Internal
    var archiveExtension: String? = "js"

    @get:Internal
    var archiveFileName: String? = null
        get() = field ?: defaultArchiveFileName

    private val defaultArchiveFileName: String
        get() {
            // [baseName]-[appendix]-[version]-[classifier].[extension]
            val baseFileName = listOf(archiveBaseName, archiveAppendix, archiveVersion, archiveClassifier)
                .filter { it != null && it.isNotBlank() }
                .joinToString("-")
            return baseFileName + if (archiveExtension == null) "" else ".$archiveExtension"
        }

    @get:OutputFile
    open val archiveFile: File
        get() = destinationDirectory!!.resolve(archiveFileName!!)

    open val configDirectory: File?
        @Optional @InputDirectory get() = project.projectDir.resolve("webpack.config.d").takeIf { it.isDirectory }

    @Input
    var report: Boolean = false

    open val reportDir: File
        @OutputDirectory get() = project.reportsDir.resolve("webpack").resolve(entry.nameWithoutExtension)

    open val evaluatedConfigFile: File
        @OutputFile get() = reportDir.resolve("webpack.config.evaluated.js")

    @Input
    var bin: String = "webpack/bin/webpack.js"

    @Input
    var sourceMaps: Boolean = true

    @Input
    @Optional
    var devServer: KotlinWebpackConfigWriter.DevServer? = null

    private fun createRunner() = KotlinWebpackRunner(
        compilation.npmProject,
        configFile,
        execHandleFactory,
        bin,
        KotlinWebpackConfigWriter(
            entry = entry,
            reportEvaluatedConfigFile = if (saveEvaluatedConfigFile) evaluatedConfigFile else null,
            outputPath = destinationDirectory,
            outputFileName = archiveFileName,
            configDirectory = configDirectory,
            bundleAnalyzerReportDir = if (report) reportDir else null,
            devServer = devServer,
            sourceMaps = sourceMaps,
            sourceMapsRuntime = sourceMaps
        )
    )

    override val nodeModulesRequired: Boolean
        @Internal get() = true

    override val requiredNpmDependencies: Collection<NpmPackageVersion>
        @Internal get() = mutableListOf<NpmPackageVersion>().also {
            it.add(versions.webpack)
            it.add(versions.webpackCli)

            if (report) {
                it.add(versions.webpackBundleAnalyzer)
            }

            if (sourceMaps) {
                it.add(versions.sourceMapLoader)
                it.add(versions.sourceMapSupport)
            }

            if (devServer != null) {
                it.add(versions.webpackDevServer)
            }
        }

    @TaskAction
    fun doExecute() {
        nodeJs.npmResolutionManager.checkRequiredDependencies(this)

        val runner = createRunner()

        if (project.gradle.startParameter.isContinuous) {
            val continuousRunner = runner

            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("webpack", Handle::class.java)
            if (deploymentHandle == null) {
                deploymentRegistry.start("webpack", DeploymentRegistry.ChangeBehavior.BLOCK, Handle::class.java, continuousRunner)
            }
        } else {
            runner.copy(
                configWriter = runner.configWriter.copy(
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