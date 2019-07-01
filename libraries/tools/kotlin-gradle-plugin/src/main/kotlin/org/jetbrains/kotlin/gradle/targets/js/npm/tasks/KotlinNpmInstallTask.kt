/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinNpmResolver
import java.io.File

open class KotlinNpmInstallTask : DefaultTask() {
    private val resolver: KotlinNpmResolver
        get() = KotlinNpmResolver.getResolver(project)

    init {
        check(project == project.rootProject)
    }

    @get:InputFiles
    val packageJsonFiles: Collection<File>
        get() = resolver.projectResolvers.values.flatMap { projectResolver ->
            projectResolver.byCompilation.keys.map { compilation ->
                compilation.npmProject.packageJsonFile
            }
        }

    @TaskAction
    fun resolve() {
        resolver.installAndClose()
    }

    companion object {
        const val NAME = "kotlinNpmInstall"
    }
}