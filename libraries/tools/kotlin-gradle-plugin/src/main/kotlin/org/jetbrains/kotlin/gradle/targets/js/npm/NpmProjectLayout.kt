/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

enum class NpmProjectLayout {
    PROJECT_DIR {
        override val allowYarnWorkspaces: Boolean
            get() = true

        override fun newNpmProject(project: Project) =
            NpmProject(project, project.projectDir, searchInParents = true)
    },
    ROOT_PROJECT_BUILD_DIR {
        override val allowYarnWorkspaces: Boolean
            get() = true

        override fun newNpmProject(project: Project): NpmProject {
            val nodeJsWorldDir = project.rootProject.buildDir.resolve("nodejs-project")

            val workDir =
                if (project == project.rootProject) nodeJsWorldDir
                else {
                    val projectPackage = project.path.removePrefix(":").replace(":", File.separator)
                    nodeJsWorldDir.resolve("packages").resolve(projectPackage)
                }

            return NpmProject(project, workDir, searchInParents = true)
        }
    },
    BUILD_DIR {
        override fun newNpmProject(project: Project) =
            NpmProject(project, project.buildDir, searchInParents = false)
    };

    open val allowYarnWorkspaces: Boolean
        get() = false

    abstract fun newNpmProject(project: Project): NpmProject
}