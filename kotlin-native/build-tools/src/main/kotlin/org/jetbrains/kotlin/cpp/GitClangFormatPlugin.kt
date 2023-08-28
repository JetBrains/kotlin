/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin

/**
 * Plugin for [GitClangFormat] that creates a task `clangFormat` that will format project sources in the current branch.
 */
open class GitClangFormatPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<NativeDependenciesPlugin>()
        val nativeDependencies = target.extensions.getByType<NativeDependenciesExtension>()

        val directory = target.layout.projectDirectory.toString()
        target.tasks.register<GitClangFormat>("clangFormat") {
            description = "Run clang-format in $directory"
            group = TASK_GROUP
            parent.convention("origin/master")
            this.directory.convention(directory)
            interactive.convention(false)
            // Needs LLVM toolchain.
            dependsOn(nativeDependencies.llvmDependency)
        }
    }

    companion object {
        @JvmStatic
        val TASK_GROUP = "development support"
    }
}