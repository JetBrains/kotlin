/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import java.io.File

open class KotlinNpmInstallTask : DefaultTask() {
    init {
        check(project == project.rootProject)
    }

    private val nodeJs get() = NodeJsRootPlugin.apply(project.rootProject)
    private val resolutionManager get() = nodeJs.npmResolutionManager

    @Suppress("unused")
    @get:InputFiles
    val packageJsonFiles: Collection<File>
        get() = resolutionManager.packageJsonFiles

    @get:OutputFile
    val yarnLock: File
        get() = nodeJs.rootPackageDir.resolve("yarn.lock")

    @TaskAction
    fun resolve() {
        resolutionManager.install()
    }

    companion object {
        const val NAME = "kotlinNpmInstall"
    }
}