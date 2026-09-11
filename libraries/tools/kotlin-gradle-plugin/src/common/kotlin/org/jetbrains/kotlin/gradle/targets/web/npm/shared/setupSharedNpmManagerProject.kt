/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.registerTask

/**
 * Sets up the consumer side of the shared-npm-project flow on the root project for one
 * platform, see [org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin].
 */
internal fun setupSharedNpmManagerProject(
    project: Project,
    platform: SharedNpmPlatform,
) {
    val sharedDependenciesResolver = project.createResolvableNpmDependenciesReportConfiguration(platform)

    project.registerTask<KotlinSetupSharedNpmProjectTask>(platform.setupTaskName) { task ->
        task.description = "Assembles the shared-npm-project of the '${platform.targetId}' target from the subprojects' package.json files."
        task.packageJsonFiles.from(
            sharedDependenciesResolver.incoming.artifactView { it.lenient(true) }.files
        )
        task.rootPackageName.set(project.name)
        task.rootPackageVersion.set(project.version.toString())
        task.outputDirectory.set(project.layout.buildDirectory.dir("${platform.rootDirName}/shared-npm-project"))
    }
}
