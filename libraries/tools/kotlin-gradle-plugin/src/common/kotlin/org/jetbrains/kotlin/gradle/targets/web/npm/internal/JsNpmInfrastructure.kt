/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.internal

import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.nodeJsRoot
import org.jetbrains.kotlin.gradle.targets.js.internal.jsToolingProject
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled

/**
 * The infrastructure that resolves and installs the npm dependencies of a Kotlin/JS or Kotlin/WasmJS compilation.
 *
 * There are two mutually exclusive implementations:
 * [Legacy] is driven by the root project, and [Isolated] is compatible with Gradle Isolated Projects.
 *
 * Only the operations that both implementations can provide are declared here.
 * Everything that is specific to the legacy infrastructure is available on [Legacy] only,
 * so the call sites that still require it must narrow the type explicitly (see [legacyOrNull]).
 *
 * @see isIsolatedNpmResolutionEnabled
 */
internal sealed interface JsNpmInfrastructure {

    /**
     * Declares that the npm dependencies required by [task] must be installed before it runs.
     */
    fun addTaskRequirements(task: RequiresNpmDependenciesTask)

    /**
     * Makes [task] depend on the installation of the npm dependencies.
     */
    fun dependsOnNpmInstall(task: Task)

    /**
     * Makes [task] run after the installation of the npm dependencies, without depending on it.
     */
    fun mustRunAfterNpmInstall(task: Task)

    /**
     * The legacy infrastructure: all the npm dependencies of the build are resolved and installed
     * by the root project, which is incompatible with Gradle Isolated Projects.
     */
    class Legacy(val nodeJsRoot: BaseNodeJsRootExtension) : JsNpmInfrastructure {
        override fun addTaskRequirements(task: RequiresNpmDependenciesTask) {
            nodeJsRoot.taskRequirements.addTaskRequirements(task)
        }

        override fun dependsOnNpmInstall(task: Task) {
            task.dependsOn(nodeJsRoot.npmInstallTaskProvider)
            task.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
        }

        override fun mustRunAfterNpmInstall(task: Task) {
            task.mustRunAfter(nodeJsRoot.npmInstallTaskProvider)
        }
    }

    /**
     * The Isolated Projects compatible infrastructure.
     *
     * Each project produces the `package.json` files of its own compilations,
     * and the project that applies `KotlinSharedNpmProjectPlugin` assembles and installs
     * the shared npm root project out of them.
     *
     * Therefore, a project can neither register its requirements in, nor depend on a task of,
     * the project that performs the installation.
     */
    object Isolated : JsNpmInfrastructure {
        override fun addTaskRequirements(task: RequiresNpmDependenciesTask) = Unit

        override fun dependsOnNpmInstall(task: Task) = Unit

        override fun mustRunAfterNpmInstall(task: Task) = Unit
    }
}

/**
 * The [JsNpmInfrastructure.Legacy] infrastructure, or `null` if the Isolated Projects compatible one is used.
 */
internal val JsNpmInfrastructure.legacyOrNull: JsNpmInfrastructure.Legacy?
    get() = this as? JsNpmInfrastructure.Legacy

internal fun KotlinJsIrCompilation.jsNpmInfrastructure(): JsNpmInfrastructure =
    if (project.isIsolatedNpmResolutionEnabled) {
        JsNpmInfrastructure.Isolated
    } else {
        JsNpmInfrastructure.Legacy(nodeJsRoot())
    }

internal fun KotlinJsIrTarget.jsNpmInfrastructure(): JsNpmInfrastructure =
    if (project.isIsolatedNpmResolutionEnabled) {
        JsNpmInfrastructure.Isolated
    } else {
        JsNpmInfrastructure.Legacy(
            webTargetVariant(
                jsVariant = { NodeJsRootPlugin.apply(project.jsToolingProject()) },
                wasmVariant = { WasmNodeJsRootPlugin.apply(project.jsToolingProject()) },
            )
        )
    }
