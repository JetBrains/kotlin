/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import java.io.File

abstract class AbstractDukatTask : AbstractTask(), RequiresNpmDependencies {
    private val nodeJs get() = NodeJsRootPlugin.apply(project.rootProject)

    @get:Internal
    override lateinit var compilation: KotlinJsCompilation

    @get:Internal
    override val nodeModulesRequired: Boolean
        get() = true

    @get:Internal
    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = listOf(nodeJs.versions.dukat)

    /**
     * Package name for the generated file (by default filename.d.ts renamed to filename.d.kt)
     */
    @Input
    @Optional
    var qualifiedPackageName: String? = null

    /**
     * Collection of d.ts files
     */
    abstract val dTsFiles: List<File>

    /**
     * Destination directory for files with converted declarations
     */
    @get:OutputDirectory
    abstract val destDir: File

    @get:Internal
    val operation: String = "Generating Kotlin/JS external declarations"

    @TaskAction
    open fun run() {
        nodeJs.npmResolutionManager.checkRequiredDependencies(this)

        DukatExecutor(
            compilation,
            dTsFiles,
            destDir,
            qualifiedPackageName,
            null,
            operation
        ).execute()
    }
}