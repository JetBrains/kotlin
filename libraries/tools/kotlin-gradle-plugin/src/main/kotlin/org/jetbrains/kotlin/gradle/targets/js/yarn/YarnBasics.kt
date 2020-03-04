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
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

abstract class YarnBasics : NpmApi {

    private val resolvedDependencies = mutableMapOf<NpmDependency, Set<File>>()

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
        val yarnPlugin = YarnPlugin.apply(project)

        project.execWithProgress(description) { exec ->
            exec.executable = nodeJs.requireConfigured().nodeExecutable
            exec.args = listOf(yarnPlugin.requireConfigured().home.resolve("bin/yarn.js").absolutePath) +
                    args +
                    if (project.logger.isDebugEnabled) "--verbose" else ""
            exec.workingDir = dir
        }

    }

    override fun resolveDependency(
        resolvedNpmProject: KotlinCompilationNpmResolution,
        dependency: NpmDependency,
        transitive: Boolean
    ): Set<File> {
        val files = resolvedDependencies[dependency]

        if (files != null) {
            return files
        }

        val npmProject = resolvedNpmProject.npmProject

        val all = mutableSetOf<File>()

        npmProject.resolve(dependency.key)?.let {
            if (it.isFile) all.add(it)
            if (it.path.endsWith(".js")) {
                val baseName = it.path.removeSuffix(".js")
                val metaJs = File(baseName + ".meta.js")
                if (metaJs.isFile) all.add(metaJs)
                val kjsmDir = File(baseName)
                if (kjsmDir.isDirectory) {
                    kjsmDir.walkTopDown()
                        .filter { it.extension == "kjsm" }
                        .forEach { all.add(it) }
                }
            }
        }

        if (transitive) {
            dependency.dependencies.forEach {
                resolveDependency(
                    resolvedNpmProject,
                    it,
                    transitive
                ).also { files ->
                    all.addAll(files)
                }
            }
        }

        resolvedDependencies[dependency] = all

        return all
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
                    ?: if (src.version == "*") byKey.entries
                        .firstOrNull { it.key.startsWith(YarnLock.key(src.key, "")) }
                        ?.value
                    else null

                if (deps != null) {
                    src.resolvedVersion = deps.version
                    src.integrity = deps.integrity
                    src.dependencies.addAll(deps.dependencies.map { dep ->
                        val scopedName = dep.scopedName
                        val child = NpmDependency(
                            src.project,
                            scopedName.toString(),
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