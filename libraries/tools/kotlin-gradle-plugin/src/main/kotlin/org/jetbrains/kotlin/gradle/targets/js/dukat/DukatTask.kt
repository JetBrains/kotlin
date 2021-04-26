/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import java.io.File

abstract class DukatTask(
    @Internal
    @Transient
    override val compilation: KotlinJsCompilation
) : DefaultTask(), RequiresNpmDependencies {
    @get:Internal
    @Transient
    protected val nodeJs = NodeJsRootPlugin.apply(project.rootProject)

    private val resolutionManager = nodeJs.npmResolutionManager

    @get:Internal
    val compilationName by lazy {
        compilation.disambiguatedName
    }

    private val npmProject by lazy { compilation.npmProject }

    @get:Internal
    override val nodeModulesRequired: Boolean
        get() = true

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> by lazy {
        emptySet<RequiredKotlinJsDependency>()
    }

    /**
     * [ExternalsOutputFormat] what to generate, sources or binaries
     */
    @Input
    var externalsOutputFormat: ExternalsOutputFormat = ExternalsOutputFormat.SOURCE

    private val projectPath = project.path

    private val compilationResolution
        get() = resolutionManager.requireInstalled(
            services,
            logger
        )[projectPath][compilationName]

    @get:Internal
    val dts: List<DtsResolver.Dts>
        get() {
            val dtsResolver = DtsResolver(compilationResolution.npmProject)
            return dtsResolver.getAllDts(
                compilationResolution.externalNpmDependencies,
                considerGeneratingFlag
            )
        }

    /**
     * Package name for the generated file (by default filename.d.ts renamed to filename.d.kt)
     */
    @Input
    @Optional
    var qualifiedPackageName: String? = null

    /**
     * Collection of d.ts files
     */
    @get:Internal
    val dTsFiles: List<File>
        get() = dts.map { it.file }

    @get:Input
    val inputs
        get() = dts.map { it.inputKey }

    /**
     * Destination directory for files with converted declarations
     */
    @get:OutputDirectory
    abstract val destinationDir: File

    @get:Internal
    internal abstract val considerGeneratingFlag: Boolean

    @get:Internal
    val operation: String = "Generating Kotlin/JS external declarations"

    @TaskAction
    open fun run() {
        resolutionManager.checkRequiredDependencies(this, services, logger, projectPath)

        destinationDir.deleteRecursively()

        if (dTsFiles.isEmpty()) {
            return
        }

        DukatRunner(
            npmProject,
            dTsFiles,
            externalsOutputFormat,
            destinationDir,
            qualifiedPackageName,
            null,
            operation
        ).execute(services)
    }
}

internal const val DUKAT_TASK_GROUP = "Dukat"