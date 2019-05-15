/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import java.io.File

object Yarn : NpmApi {
    override fun setup(project: Project) {
        YarnPlugin.apply(project).executeSetup()
    }

    val Project.packageJsonHashFile: File
        get() = buildDir.resolve("package.json.hash")

    fun yarnExec(
        project: Project,
        description: String,
        vararg args: String,
        npmProject: NpmProject = NpmProject[project]
    ) {
        val nodeJsEnv = NodeJsPlugin.apply(project).root.environment
        val yarnEnv = YarnPlugin.apply(project).environment

        val nodeWorkDir = npmProject.nodeWorkDir

        project.execWithProgress(description) { exec ->
            exec.executable = nodeJsEnv.nodeExecutable
            exec.args = listOf(yarnEnv.home.resolve("bin/yarn.js").absolutePath) + args
            exec.workingDir = nodeWorkDir
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

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun resolveProject(npmPackage: NpmResolver.NpmPackage) {
        val project = npmPackage.project
        if (!project.yarn.useWorkspaces) {
            YarnAvoidance(project).updateIfNeeded {
                yarnExec(project, NpmApi.resolveOperationDescription("yarn for ${project.path}"))
                yarnLockReadTransitiveDependencies(NpmProject[project].nodeWorkDir, npmPackage.npmDependencies)
            }
        }
    }

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun hookRootPackage(
        rootProject: Project,
        rootPackageJson: PackageJson,
        allWorkspaces: Collection<NpmResolver.NpmPackage>
    ): Boolean {
        if (rootProject.yarn.useWorkspaces) {
            rootPackageJson.private = true
            rootPackageJson.workspaces = allWorkspaces
                .filter { it.project != rootProject }
                .map { it.project.npmProject.nodeWorkDir.relativeTo(rootProject.npmProject.nodeWorkDir).path }

            return true
        }

        return false
    }

//    override fun shouldHoistGradleNodeModules(project: Project): Boolean =
//        project.rootProject.yarn.useWorkspaces

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun resolveRootProject(
        rootProject: Project,
        subprojects: MutableList<NpmResolver.NpmPackage>
    ) {
        check(rootProject == rootProject.rootProject)

        if (rootProject.yarn.useWorkspaces) {
            resolveWorkspaces(rootProject, subprojects)
        }
    }

    private fun resolveWorkspaces(
        rootProject: Project,
        subprojects: MutableList<NpmResolver.NpmPackage>
    ) {
        val upToDateChecks = subprojects.map {
            YarnAvoidance(it.project)
        }

        if (upToDateChecks.all { it.upToDate }) return

        yarnExec(rootProject, NpmApi.resolveOperationDescription("yarn"))
        yarnLockReadTransitiveDependencies(NpmProject[rootProject].nodeWorkDir, subprojects.flatMap { it.npmDependencies })

        upToDateChecks.forEach {
            it.commit()
        }
    }

    override fun cleanProject(project: Project) {
        super.cleanProject(project)
        project.packageJsonHashFile.delete()
    }
}