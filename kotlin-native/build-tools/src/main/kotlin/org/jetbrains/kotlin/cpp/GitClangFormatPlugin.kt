/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * Plugin for [GitClangFormat] that creates a task `clangFormat` that will format project sources in the current branch.
 */
open class GitClangFormatPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val directory = target.layout.projectDirectory.toString()
        target.tasks.register<GitClangFormat>("clangFormat") {
            description = "Run clang-format in $directory"
            group = TASK_GROUP
            parent.convention("origin/master")
            this.directory.convention(directory)
            interactive.convention(false)
            // Needs LLVM toolchain.
            // TODO: It does not need every dependency ever, only LLVM toolchain for the host.
            dependsOn(":kotlin-native:dependencies:update")
        }
    }

    companion object {
        @JvmStatic
        val TASK_GROUP = "development support"
    }
}