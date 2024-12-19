/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.web.nodejs.AbstractNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.whenEvaluated

/**
 * A utility class that applies the configuration for resolving NPM dependencies
 * within given Gradle projects. This class wires required tasks and dependencies
 * to projects using the provided resolution logic and ensures correct task execution order.
 *
 * @property nodeJsRootApply A function that initializes the `AbstractNodeJsRootExtension`
 * for the root Node.js project configuration.
 * @property singleNodeJsApply A function to apply per-project Node.js-specific configurations.
 */
internal class NpmResolverPluginApplier(
    private val nodeJsRootApply: (Project) -> AbstractNodeJsRootExtension,
    private val singleNodeJsApply: (Project) -> Unit,
) {
    fun apply(project: Project) {
        val nodeJsRoot = nodeJsRootApply(project)
        singleNodeJsApply(project)
        nodeJsRoot.resolver.addProject(project)
        project.whenEvaluated {
            project.tasks.implementing(RequiresNpmDependencies::class)
                .configureEach { task ->
                    if (task.enabled) {
                        task as RequiresNpmDependencies
                        // KotlinJsTest delegates npm dependencies to testFramework,
                        // which can be defined after this configure action
                        if (task !is KotlinJsTest) {
                            nodeJsRoot.taskRequirements.addTaskRequirements(task)

                            if (task.requiredNpmDependencies.isNotEmpty()) {
                                task.dependsOn(
                                    nodeJsRoot.npmInstallTaskProvider,
                                )

                                task.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
                            }
                        }
                    }
                }
        }
    }
}