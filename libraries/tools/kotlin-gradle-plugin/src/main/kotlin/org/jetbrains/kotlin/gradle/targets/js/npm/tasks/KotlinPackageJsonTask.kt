/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinNpmResolver
import java.io.File

class KotlinPackageJsonTask : DefaultTask() {
    @Internal
    lateinit var compilation: KotlinJsCompilation

    private val compilationResolver: KotlinCompilationNpmResolver
        get() = KotlinNpmResolver.getResolver(project.rootProject).projectResolvers[project]!!.byCompilation[compilation]!!

    @get:InputFiles
    val configuration: Configuration
        get() = compilationResolver.aggregatedConfiguration

    @get:OutputFile
    val packageJson: File
        get() = compilation.npmProject.packageJsonFile

    @TaskAction
    fun resolve() {
        compilationResolver.projectPackage
    }
}