/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.TeamCityMessageCommonClient
import org.jetbrains.kotlin.gradle.internal.execWithErrorLogger
import org.jetbrains.kotlin.gradle.internal.newBuildOpLogger
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessageOutputStreamHandler
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.getValue
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class KotlinSwc
@Inject
internal constructor(
    @Internal
    @Transient
    final override val compilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
    private val execOps: ExecOperations,
) : DefaultTask(), RequiresNpmDependencies {
    @get:Internal
    internal abstract val versions: Property<NpmVersions>

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = with(versions.get()) {
            setOf(swcCli, swcCore)
        }

    @Input
    var mode: Mode = Mode.DEVELOPMENT

    @Input
    var bin: String = "@swc/cli/bin/swc.js"

    @Input
    val config: Property<KotlinSwcConfig> = project.objects.property(KotlinSwcConfig::class.java)

    @get:Internal
    abstract val inputFilesDirectory: DirectoryProperty


    private val npmProject = compilation.npmProject
    private val npmProjectDir by project.provider { compilation.npmProject.dir }

    @get:Internal
    internal abstract val npmToolingEnvDir: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val outputDirectory: DirectoryProperty

    @get:OutputFile
    open val configFile: Provider<File> =
        npmProjectDir.map { it.asFile.resolve(".swcrc") }


    @TaskAction
    fun doExecute() {
        val progressLogger = objects.newBuildOpLogger()
        val errorClient = configureClient(LogType.ERROR, progressLogger)
        val standardClient = configureClient(LogType.LOG, progressLogger)

        execWithErrorLogger(
            progressLogger,
            description = "swc",
            execOps = execOps,
            errorClient = errorClient,
            standardClient = standardClient,
        ) { execSpec ->
            configureExec(
                execSpec,
                standardClient,
                errorClient,
            )
        }
    }

    private fun configureExec(
        execSpec: ExecSpec,
        errorClient: TeamCityMessageCommonClient,
        standardClient: TeamCityMessageCommonClient,
    ) {
        execSpec.standardOutput = TCServiceMessageOutputStreamHandler(
            client = standardClient,
            onException = { },
            logger = standardClient.log
        )

        execSpec.errorOutput = TCServiceMessageOutputStreamHandler(
            client = errorClient,
            onException = { },
            logger = errorClient.log
        )

        val configValue = config.get()
        configValue.save(configFile.get())

        val args = buildList {
            add("--config-file=${configFile.get().absolutePath}")
            add("--env-name=${mode.code}")
            add("--out-dir=${outputDirectory.get().asFile.absolutePath}")
            add("--out-file-extension=${configValue.fileExtension}")
            add(inputFilesDirectory.get().asFile.absolutePath)
        }

        execSpec.workingDir(npmProject.dir)
        execSpec.executable(npmProject.nodeExecutable)

        val modules = NpmProjectModules(npmToolingEnvDir.getFile())

        execSpec.args = listOf(modules.require(bin)) + args
    }

    private fun configureClient(clientType: LogType, progressLogger: ProgressLogger?): TeamCityMessageCommonClient {
        val client = TeamCityMessageCommonClient(clientType, logger)

        if (progressLogger != null) {
            client.progressLogger = progressLogger
        }

        return client
    }
}