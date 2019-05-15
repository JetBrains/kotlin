/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.DefaultTask
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.*
import org.gradle.deployment.internal.Deployment
import org.gradle.deployment.internal.DeploymentHandle
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.process.internal.ExecHandle
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.injected
import java.io.File
import javax.inject.Inject

open class KotlinWebpack : DefaultTask(), RequiresNpmDependencies {
    private val versions by lazy { project.nodeJs.versions }

    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    @Input
    @SkipWhenEmpty
    open lateinit var entry: File

    open val configFile: File
        @OutputFile get() = project.npmProject.nodeWorkDir.resolve("webpack.config.js")

    @Input
    var saveEvaluatedConfigFile: Boolean = true

    open val outputPath: File
        @OutputDirectory get() = project.buildDir.resolve("lib")

    open val configDirectory: File?
        @Optional @InputDirectory get() = project.projectDir.resolve("webpack.config.d").takeIf { it.isDirectory }

    @Input
    var report: Boolean = false

    open val reportDir: File
        @OutputDirectory get() = project.reportsDir.resolve("webpack").resolve(entry.nameWithoutExtension)

    open val evaluatedConfigFile: File
        @OutputFile get() = reportDir.resolve("webpack.config.evaluated.js")

    @Input
    var bin: String = "webpack"

    @Input
    var sourceMaps: Boolean = true

    @Input
    @Optional
    var devServer: KotlinWebpackConfigWriter.DevServer? = null

    override val requiredNpmDependencies: Collection<NpmPackageVersion>
        get() = mutableListOf<NpmPackageVersion>().also {
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

    private fun createRunner() = KotlinWebpackRunner(
        project,
        configFile,
        execHandleFactory,
        bin,
        KotlinWebpackConfigWriter(
            entry = entry,
            reportEvaluatedConfigFile = if (saveEvaluatedConfigFile) evaluatedConfigFile else null,
            outputPath = outputPath,
            configDirectory = configDirectory,
            bundleAnalyzerReportDir = if (report) reportDir else null,
            devServer = devServer,
            sourceMaps = sourceMaps,
            sourceMapsRuntime = sourceMaps
        )
    )

    @TaskAction
    fun execute() {
        NpmResolver.resolve(project)
        NpmResolver.checkRequiredDependencies(project, this)

        val runner = createRunner()

        if (project.gradle.startParameter.isContinuous) {
            val deploymentRegistry = services.get(DeploymentRegistry::class.java)
            val deploymentHandle = deploymentRegistry.get("webpack", Handle::class.java)
            if (deploymentHandle == null) {
                deploymentRegistry.start("webpack", DeploymentRegistry.ChangeBehavior.BLOCK, Handle::class.java, runner)
            }
        } else {
            runner.execute()
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