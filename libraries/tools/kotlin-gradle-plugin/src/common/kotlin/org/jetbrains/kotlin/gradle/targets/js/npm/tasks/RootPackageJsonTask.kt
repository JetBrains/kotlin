/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironmentTask
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager

@DisableCachingByDefault
abstract class RootPackageJsonTask :
    DefaultTask(),
    NodeJsEnvironmentTask,
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    @get:OutputFile
    val rootPackageJsonFile: Property<RegularFile> = project.objects.fileProperty()

    @TaskAction
    fun resolve() {
        npmResolutionManager.get().prepare(logger, nodeJsEnvironment.get(), packageManagerEnv.get())
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}