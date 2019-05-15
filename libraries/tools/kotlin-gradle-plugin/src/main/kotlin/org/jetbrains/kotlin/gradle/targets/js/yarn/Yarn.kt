/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import java.io.File

object Yarn : NpmApi {
    override fun setup(project: Project) {
        YarnPlugin.apply(project).executeSetup()
    }

    val NpmProject.packageJsonHashFile: File
        get() = dir.resolve("package.json.hash")

    fun yarnExec(
        project: Project,
        dir: File,
        description: String,
        vararg args: String
    ) {
        val nodeJsEnv = NodeJsPlugin.apply(project).root.environment
        val yarnEnv = YarnPlugin.apply(project).environment

        project.execWithProgress(description) { exec ->
            exec.executable = nodeJsEnv.nodeExecutable
            exec.args = listOf(yarnEnv.home.resolve("bin/yarn.js").absolutePath) + args
            exec.workingDir = dir
        }

    }

    private fun yarnLockReadTransitiveDependencies(
        nodeWorkDir: File,
        srcDependenciesList: Collection<NpmDependency>
    ) {
        val yarnLock = nodeWorkDir.resolve("yarn.lock")
        if (yarnLock.isFile) {
            val byKey = YarnLock.parse(yarnLock).entries.associateBy { it.key }
            val visited = mutableSetOf<NpmDependency>()

            fun resolveRecursively(src: NpmDependency): NpmDependency {
                if (src in visited) return src
                visited.add(src)

                val key = YarnLock.key(src.key, src.version)
                val deps = byKey[key]
                if (deps != null) {
                    src.dependencies.addAll(deps.dependencies.map { dep ->
                        val scopedName = dep.scopedName

                        resolveRecursively(
                            NpmDependency(
                                src.project,
                                scopedName.scope,
                                scopedName.name,
                                dep.version ?: "*"
                            )
                        )
                    })
                } else {
                    // todo: [WARN] cannot find $key in yarn.lock
                }

                return src
            }

            srcDependenciesList.forEach { src ->
                resolveRecursively(src)
            }
        }
    }

    override fun resolveProject(resolvedNpmProject: NpmProjectPackage) {
        val project = resolvedNpmProject.project
        if (!project.yarn.useWorkspaces) {
            YarnAvoidance(resolvedNpmProject.npmProject).updateIfNeeded {
                yarnExec(project, resolvedNpmProject.npmProject.dir, NpmApi.resolveOperationDescription("yarn for ${project.path}"))
                yarnLockReadTransitiveDependencies(resolvedNpmProject.npmProject.dir, resolvedNpmProject.npmDependencies)
            }
        }
    }

    override fun resolveRootProject(rootProject: Project, subProjects: MutableList<NpmProjectPackage>) {
        check(rootProject == rootProject.rootProject)

        if (rootProject.yarn.useWorkspaces) {
            resolveWorkspaces(rootProject, subProjects)
        }
    }

    private fun resolveWorkspaces(
        rootProject: Project,
        npmProjects: MutableList<NpmProjectPackage>
    ) {
        val upToDateChecks = npmProjects.map {
            YarnAvoidance(it.npmProject)
        }

        if (upToDateChecks.all { it.upToDate }) return

        val nodeJsWorldDir = rootProject.nodeJs.root.rootPackageDir

        saveRootProjectWorkspacesPackageJson(rootProject, npmProjects, nodeJsWorldDir)

        yarnExec(rootProject, nodeJsWorldDir, NpmApi.resolveOperationDescription("yarn"))
        yarnLockReadTransitiveDependencies(nodeJsWorldDir, npmProjects.flatMap { it.npmDependencies })

        upToDateChecks.forEach {
            it.commit()
        }
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProject: Project,
        npmProjects: MutableList<NpmProjectPackage>,
        nodeJsWorldDir: File
    ) {
        val rootPackageJson = PackageJson(rootProject.name, rootProject.version.toString())
        rootPackageJson.private = true
        rootPackageJson.workspaces = npmProjects
            .map { it.npmProject.dir.relativeTo(nodeJsWorldDir).path }

        rootProject.nodeJs.packageJsonHandlers.forEach {
            it(rootPackageJson)
        }

        rootPackageJson.saveTo(
            nodeJsWorldDir.resolve(NpmProject.PACKAGE_JSON)
        )
    }
}