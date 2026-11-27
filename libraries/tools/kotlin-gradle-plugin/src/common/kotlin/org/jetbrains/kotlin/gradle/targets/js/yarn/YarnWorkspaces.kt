/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

@ObsoleteNpmDependencyModel
class YarnWorkspaces internal constructor(
    execOps: ExecOperations,
    objects: ObjectFactory,
    mutexFile: File,
    /**
     * Lock file which Yarn is expected to create during the installation.
     * If it is set and does not exist after the installation, the installation fails (KT-82693).
     */
    private val expectedLockFile: Provider<RegularFile>,
) : YarnBasics(
    execOps = execOps,
    objects = objects,
    mutexFile = mutexFile,
) {

    override fun preparedFiles(nodeJs: NodeJsEnvironment): Collection<File> {
        return listOf(
            nodeJs
                .rootPackageDir
                .getFile()
                .resolve(NpmProject.PACKAGE_JSON)
        )
    }

    override fun prepareRootProject(
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: YarnEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        subProjects: Collection<PreparedKotlinCompilationNpmResolution>,
    ) {
        return prepareRootPackageJson(
            nodeJs,
            rootProjectName,
            rootProjectVersion,
            subProjects,
            packageManagerEnvironment.yarnResolutions
                .associate { it.path to it.toVersionString() },
        )
    }

    private fun prepareRootPackageJson(
        nodeJs: NodeJsEnvironment,
        rootProjectName: String,
        rootProjectVersion: String,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
    ) {
        val rootPackageJsonFile = preparedFiles(nodeJs).single()

        saveRootProjectWorkspacesPackageJson(
            rootProjectName,
            rootProjectVersion,
            npmProjects,
            resolutions,
            rootPackageJsonFile
        )
    }

    override fun resolveRootProject(
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: YarnEnvironment,
        cliArgs: List<String>,
    ) {
        val nodeJsWorldDir = nodeJs.rootPackageDir.map { it.asFile }

        packageManagerExec(
            logger,
            nodeJs,
            packageManagerEnvironment,
            nodeJsWorldDir,
            NpmApiExecution.resolveOperationDescription("yarn"),
            cliArgs
        )

        val expectedLockFile = expectedLockFile.orNull?.asFile
        if (expectedLockFile != null && !expectedLockFile.exists()) {
            throw GradleException(
                "Yarn finished without errors, but did not create '$expectedLockFile'. " +
                        "Most likely the Yarn process exited before installing the dependencies. " +
                        "Try to rerun the build. " +
                        "If the problem persists, please submit an issue at https://kotl.in/issue " +
                        "and attach the build log produced with '--debug'. " +
                        "Note that the default package manager is going to be switched to npm (https://kotl.in/issue/KT-84662). " +
                        "You can switch to npm now by setting 'kotlin.js.yarn=false' in 'gradle.properties'."
            )
        }
    }

    private fun saveRootProjectWorkspacesPackageJson(
        rootProjectName: String,
        rootProjectVersion: String,
        npmProjects: Collection<PreparedKotlinCompilationNpmResolution>,
        resolutions: Map<String, String>,
        rootPackageJsonFile: File,
    ) {
        val nodeJsWorldDir = rootPackageJsonFile.parentFile
        val rootPackageJson = PackageJson(rootProjectName, rootProjectVersion)
        rootPackageJson.private = true

        val npmProjectWorkspaces = npmProjects.map { it.npmProjectDir.getFile().relativeTo(nodeJsWorldDir).path }
        val importedProjectWorkspaces =
            NpmImportedPackagesVersionResolver(npmProjects, nodeJsWorldDir).resolveAndUpdatePackages()

        rootPackageJson.workspaces = npmProjectWorkspaces + importedProjectWorkspaces
        rootPackageJson.customField("resolutions", resolutions)
        rootPackageJson.saveTo(
            rootPackageJsonFile
        )
    }
}
