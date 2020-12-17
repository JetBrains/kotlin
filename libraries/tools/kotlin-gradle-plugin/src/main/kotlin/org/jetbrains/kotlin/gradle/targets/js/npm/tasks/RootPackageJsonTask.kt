/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.utils.disableTaskOnConfigurationCacheBuild
import java.io.File

open class RootPackageJsonTask : DefaultTask() {
    init {
        check(project == project.rootProject)

        outputs.upToDateWhen {
            false
        }

        onlyIf {
            resolutionManager.isConfiguringState()
        }
    }

    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val resolutionManager get() = nodeJs.npmResolutionManager

    init {
        // TODO: temporary workaround for configuration cache enabled builds
//        disableTaskOnConfigurationCacheBuild { resolutionManager.toString() }
    }

    @get:OutputFile
    val rootPackageJson: File
        get() = nodeJs.rootPackageDir.resolve(NpmProject.PACKAGE_JSON)

    @TaskAction
    fun resolve() {
        resolutionManager.prepare(logger)
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}