/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import java.io.File

abstract class YarnBasics : NpmApi {
    override fun setup(project: Project) {
        YarnPlugin.apply(project).executeSetup()
    }

    fun yarnExec(
        project: Project,
        dir: File,
        description: String,
        vararg args: String
    ) {
        val nodeJs = NodeJsRootPlugin.apply(project)
        val nodeJsEnv = nodeJs.environment
        val yarnEnv = YarnPlugin.apply(project).environment

        project.execWithProgress(description) { exec ->
            exec.executable = nodeJsEnv.nodeExecutable
            exec.args = listOf(yarnEnv.home.resolve("bin/yarn.js").absolutePath) + args
            exec.workingDir = dir
        }
    }

    protected fun yarnLockReadTransitiveDependencies(
        nodeWorkDir: File,
        srcDependenciesList: Collection<NpmDependency>
    ) {
        val yarnLock = nodeWorkDir.resolve("yarn.lock")
        if (yarnLock.isFile) {
            val byKey = YarnLock.parse(yarnLock).entries.associateBy { it.key }
            val visited = mutableMapOf<NpmDependency, NpmDependency>()

            fun resolveRecursively(src: NpmDependency): NpmDependency {
                val copy = visited[src]
                if (copy != null) {
                    src.resolvedVersion = copy.resolvedVersion
                    src.integrity = copy.integrity
                    src.dependencies.addAll(copy.dependencies)
                    return src
                }
                visited[src] = src

                val key = YarnLock.key(src.key, src.version)
                val deps = byKey[key]
                if (deps != null) {
                    src.resolvedVersion = deps.version
                    src.integrity = deps.integrity
                    src.dependencies.addAll(deps.dependencies.map { dep ->
                        val scopedName = dep.scopedName
                        val child = NpmDependency(
                            src.project,
                            scopedName.scope,
                            scopedName.name,
                            dep.version ?: "*"
                        )
                        child.parent = src

                        resolveRecursively(child)

                        child
                    })
                } else {
                    error("Cannot find $key in yarn.lock")
                }

                return src
            }

            srcDependenciesList.forEach { src ->
                resolveRecursively(src)
            }
        }
    }
}