/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

class DukatProjectVisitor(val project: Project) {
    fun hookImplicitNpmDependencies(npmDependencies: MutableSet<NpmDependency>) {
        if (!project.nodeJs.root.experimental.discoverTypes) return

        val types = npmDependencies.map {
            NpmDependency(
                project,
                "types",
                it.name,
                "*",
                NpmDependency.Scope.OPTIONAL
            )
        }

        npmDependencies.addAll(types)
    }

    fun addToolDependencies(dependencies: DependencySet) {
        dependencies.add(project.nodeJs.versions.dukat.createDependency(project, NpmDependency.Scope.DEV))
    }
}