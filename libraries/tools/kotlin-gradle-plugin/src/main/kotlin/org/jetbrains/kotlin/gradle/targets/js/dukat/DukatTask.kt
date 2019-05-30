/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import java.io.File
import javax.xml.ws.Action

open class DukatTask : AbstractTask(), RequiresNpmDependencies {
    override lateinit var compilation: KotlinJsCompilation

    override val nodeModulesRequired: Boolean
        get() = true

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = listOf(project.nodeJs.versions.dukat)

    /**
     * Package name for the generated file (by default filename.d.ts renamed to filename.d.kt)
     */
    @Input
    @Optional
    var qualifiedPackageName: String? = null

    /**
     * Collection of d.ts files
     */
    @InputFiles
    lateinit var dTsFiles: FileCollection

    /**
     * Destination directory for files with converted declarations
     */
    @OutputDirectory
    lateinit var destDir: File

    /**
     * js-interop JVM engine
     * "graal" (default) or "j2v8"
     */
    @Input
    @Optional
    var jsInteropJvmEngine: String? = null

    val operation: String = "Generating Kotlin/JS external declarations"

    @Action
    fun run() {
        NpmResolver.checkRequiredDependencies(project, this)
        DukatExecutor(
            compilation,
            dTsFiles.files,
            destDir,
            qualifiedPackageName,
            jsInteropJvmEngine,
            operation
        ).execute()
    }
}