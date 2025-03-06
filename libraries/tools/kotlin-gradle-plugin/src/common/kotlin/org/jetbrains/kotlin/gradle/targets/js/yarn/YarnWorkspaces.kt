/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.PreparedKotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

class YarnWorkspaces internal constructor(
    execOps: ExecOperations,
    objects: ObjectFactory,
) : YarnBasics(
    execOps = execOps,
    objects = objects,
) {

    /**
     * Manually creating new instances of this class is deprecated.
     *
     * An instance of [YarnWorkspaces] can be found from
     * [Yarn.yarnWorkspaces][org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn.yarnWorkspaces].
     *
     * An instance of [Yarn] can be found using the extensions
     * [WasmYarnRootExtension][org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension]
     * and
     * [YarnRootExtension][org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension].
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn.yarnWorkspaces
     */
    @Deprecated(
        message = "Manually creating instances of this class is deprecated. " +
                "An instance can be obtained via Yarn.yarnWorkspaces, and Yarn can be obtained using WasmYarnRootExtension or YarnRootExtension. " +
                "Scheduled for removal in Kotlin 2.4.",
        level = DeprecationLevel.ERROR,
    )
    @Suppress("UNREACHABLE_CODE", "unused")
    constructor() : this(
        execOps = error("Cannot create instance of YarnWorkspaces. Constructor is deprecated, see Kdoc for details."),
        objects = error("Cannot create instance of YarnWorkspaces. Constructor is deprecated, see Kdoc for details."),
    )

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
        services: ServiceRegistry,
        logger: Logger,
        nodeJs: NodeJsEnvironment,
        packageManagerEnvironment: YarnEnvironment,
        cliArgs: List<String>,
    ) {
        val nodeJsWorldDir = nodeJs.rootPackageDir.getFile()

        yarnExec(
            logger,
            nodeJs,
            packageManagerEnvironment,
            nodeJsWorldDir,
            NpmApiExecution.resolveOperationDescription("yarn"),
            cliArgs
        )
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
