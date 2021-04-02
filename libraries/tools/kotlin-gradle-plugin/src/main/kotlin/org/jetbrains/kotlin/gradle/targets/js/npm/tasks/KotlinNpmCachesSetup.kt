/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import javax.inject.Inject

abstract class KotlinNpmCachesSetup : DefaultTask() {
    @get:Inject
    open val fileHasher: FileHasher
        get() = throw UnsupportedOperationException()

    @Transient
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val gradleNodeModules = nodeJs.npmResolutionManager.resolver.gradleNodeModulesProvider
    private val compositeNodeModules = nodeJs.npmResolutionManager.resolver.compositeNodeModulesProvider

    @TaskAction
    fun setup() {
        gradleNodeModules.get().fileHasher = fileHasher
        compositeNodeModules.get().fileHasher = fileHasher
    }

    companion object {
        const val NAME = "kotlinNpmCachesSetup"
    }
}