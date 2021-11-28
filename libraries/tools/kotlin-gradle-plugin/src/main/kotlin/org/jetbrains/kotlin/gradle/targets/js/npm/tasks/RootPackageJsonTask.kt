/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import java.io.File

open class RootPackageJsonTask : DefaultTask() {
    init {
        check(project == project.rootProject)

        onlyIf {
            resolutionManager.isConfiguringState()
        }
    }

    @Transient
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val resolutionManager = nodeJs.npmResolutionManager

    @get:OutputFile
    val rootPackageJson: File by lazy {
        nodeJs.rootPackageDir.resolve(NpmProject.PACKAGE_JSON)
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    val packageJsonFiles: Collection<File> by lazy {
        resolutionManager.packageJsonFiles
    }

    @TaskAction
    fun resolve() {
        resolutionManager.prepare(logger)
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}