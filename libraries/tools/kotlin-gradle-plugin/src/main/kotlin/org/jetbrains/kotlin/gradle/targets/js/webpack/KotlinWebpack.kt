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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.injected
import java.io.File
import javax.inject.Inject

open class KotlinWebpack : DefaultTask() {
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

    open val configDirectory: File
        @InputDirectory get() = project.projectDir.resolve("webpack.config.d")

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
    var devServer: KotlinWebpackConfig.DevServer? = null

    internal fun createRunner() = KotlinWebpackRunner(
        project,
        configFile,
        execHandleFactory,
        bin,
        KotlinWebpackConfig(
            entry,
            if (saveEvaluatedConfigFile) evaluatedConfigFile else null,
            outputPath,
            configDirectory,
            if (report) reportDir else null,
            devServer,
            sourceMaps = sourceMaps
        )
    )

    @TaskAction
    fun execute() {
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

    companion object {
        fun configure(compilation: KotlinCompilationToRunnableFiles<*>) {
            val project = compilation.target.project
            val npmProject = project.npmProject

            compilation.dependencies {
                runtimeOnly(npm("webpack", "4.29.6"))
                runtimeOnly(npm("webpack-cli", "3.3.0"))
                runtimeOnly(npm("webpack-bundle-analyzer", "3.3.2"))

                // for source map support only
                runtimeOnly(npm("source-map-loader", "0.2.4"))
                runtimeOnly(npm("source-map-support", "0.5.12"))
            }

            project.createOrRegisterTask<KotlinWebpack>("webpack") {
                val compileKotlinTask = compilation.compileKotlinTask
                compileKotlinTask as Kotlin2JsCompile

                it.dependsOn(compileKotlinTask)

                it.entry = npmProject.moduleOutput(compileKotlinTask)

                it.outputs.upToDateWhen { false }
            }

            compilation.dependencies {
                runtimeOnly(npm("webpack-dev-server", "3.3.1"))
            }

            project.createOrRegisterTask<KotlinWebpack>("run") {
                val compileKotlinTask = compilation.compileKotlinTask
                compileKotlinTask as Kotlin2JsCompile

                it.dependsOn(compileKotlinTask)

                it.bin = "webpack-dev-server"
                it.entry = npmProject.moduleOutput(compileKotlinTask)

                val projectDir = compilation.target.project.projectDir.canonicalPath
                it.devServer = KotlinWebpackConfig.DevServer(
                    contentBase = listOf("$projectDir/src/main/resources")
                )

                it.outputs.upToDateWhen { false }
            }
        }
    }
}