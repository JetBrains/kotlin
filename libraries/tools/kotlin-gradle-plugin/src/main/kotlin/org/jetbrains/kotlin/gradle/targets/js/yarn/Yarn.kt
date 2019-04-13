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
        npmProjectLayout: NpmProjectLayout = NpmProjectLayout[project]
    ) {
        val nodeJsEnv = NodeJsPlugin.apply(project).environment
        val yarnEnv = YarnPlugin.apply(project).environment

        val packageJsonHashFile = project.packageJsonHashFile
        val packageJsonHash = if (packageJsonHashFile.exists()) packageJsonHashFile.readText() else null

        val hasher = (project as ProjectInternal).services.get(FileHasher::class.java)
        val hash = hasher.hash(npmProjectLayout.packageJsonFile).toByteArray().toHexString()

        if (packageJsonHash == hash) return

        packageJsonHashFile.writeText(hash)

        val nodeWorkDir = npmProjectLayout.nodeWorkDir

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
                        resolveRecursively(
                            NpmDependency(
                                src.project,
                                dep.group,
                                dep.packageName,
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
            yarnExec(project, NpmApi.resolveOperationDescription("yarn for ${project.path}"))
            yarnLockReadTransitiveDependencies(NpmProjectLayout[project].nodeWorkDir, npmPackage.npmDependencies)
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
                .map { it.project.projectDir.relativeTo(rootProject.rootDir).path }

            return true
        }

        return false
    }

    override fun getHoistGradleNodeModules(project: Project): Boolean =
        project.rootProject.yarn.useWorkspaces

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun resolveRootProject(
        rootProject: Project,
        subprojects: MutableList<NpmResolver.NpmPackage>
    ) {
        check(rootProject == rootProject.rootProject)

        if (rootProject.yarn.useWorkspaces) {
            yarnExec(rootProject, NpmApi.resolveOperationDescription("yarn"))
            yarnLockReadTransitiveDependencies(NpmProjectLayout[rootProject].nodeWorkDir, subprojects.flatMap { it.npmDependencies })
        } else {
            if (subprojects.any { it.project != rootProject }) {
                // todo: proofread message
                rootProject.logger.warn(
                    "Build contains sub projects with NPM dependencies. " +
                            "It is recommended to enable yarn workspaces to store common NPM dependencies in root project. " +
                            "To enable it add this to your root project: \n" +
                            "nodeJs { manageNodeModules = true } \n" +
                            "Note: with `manageNodeModules` enabled, your `node_modules` and `package.json` files will be managed by " +
                            "Gradle, will be overridden during build and should be ignored in VCS."
                )
            }
        }
    }

    override fun cleanProject(project: Project) {
        super.cleanProject(project)
        project.packageJsonHashFile.delete()
    }
}