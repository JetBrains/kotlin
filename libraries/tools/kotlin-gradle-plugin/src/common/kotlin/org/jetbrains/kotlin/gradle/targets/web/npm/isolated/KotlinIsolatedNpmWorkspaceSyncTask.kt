/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.isolated

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings

/**
 * Puts the compiled JS files of a compilation into its npm workspace of the shared npm root project.
 *
 * This is the Isolated Projects compatible counterpart of the legacy sync of the
 * [org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink] outputs into the npm root project,
 * performed by [org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary.linkSyncTask]:
 * the JS runtime can only resolve the npm dependencies of a compilation when it executes
 * its files in the context of the npm workspace where those dependencies are installed.
 *
 * The npm workspace is owned by [KotlinAggregatedNpmWorkspaceService], and its location is only known
 * after [KotlinIsolatedNpmInstallTask], so the files are copied at execution time by the service,
 * and the task declares no outputs and is never up to date.
 */
@DisableCachingByDefault(because = "Copies files into a directory owned by a build service")
internal abstract class KotlinIsolatedNpmWorkspaceSyncTask : DefaultTask(), UsesKotlinAggregatedNpmWorkspaceService {

    /**
     * The directories with the compiled JS files of the compilation.
     */
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    abstract val from: ConfigurableFileCollection

    /**
     * The name of the npm package of the compilation, which is the name of its npm workspace.
     */
    @get:Input
    abstract val npmProjectName: Property<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun sync() {
        aggregatedNpmWorkspaceService.get().syncCompiledJsFiles(npmProjectName.get(), from.files)
    }
}
