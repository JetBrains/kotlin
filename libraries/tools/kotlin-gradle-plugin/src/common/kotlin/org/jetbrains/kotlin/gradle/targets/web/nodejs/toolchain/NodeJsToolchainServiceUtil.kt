/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalNodeJsToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.statistics.NodeJsToolchainServiceMetrics
import org.jetbrains.kotlin.gradle.targets.js.nodejs.computeNodeBinDir
import org.jetbrains.kotlin.gradle.tasks.nodejs.UsesNodeJsToolchainService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File

private val serviceClass = NodeJsToolchainService::class.java
internal val nodeJsServiceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

internal fun <P : NodeJsToolchainService.Parameters, T : NodeJsToolchainService<P>> registerNodeJsToolchainServiceIfAbsent(
    gradle: Gradle,
    nodeJsToolchainClass: Class<T>,
    configureBuildServiceParameters: (P) -> Unit,
): Provider<out NodeJsToolchainService<out NodeJsToolchainService.Parameters>> {
    gradle.sharedServices.registrations.findByName(nodeJsServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<NodeJsToolchainService<out NodeJsToolchainService.Parameters>>
    }

    @Suppress("UNCHECKED_CAST")
    return when (nodeJsToolchainClass) {
        DefaultNodeJsToolchainService::class.java -> DefaultNodeJsToolchainServiceImpl.registerIfAbsent(
            gradle,
            configureBuildServiceParameters as (DefaultNodeJsToolchainService.Parameters) -> Unit
        )
        PreInstalledNodeJsToolchainService::class.java -> PreInstalledNodeJsToolchainServiceImpl.registerIfAbsent(
            gradle,
            configureBuildServiceParameters as (PreInstalledNodeJsToolchainService.Parameters) -> Unit
        )
        DisabledNodeJsToolchainService::class.java -> DisabledNodeJsToolchainServiceImpl.registerIfAbsent(gradle)
        else -> gradle.sharedServices.registerIfAbsent(nodeJsServiceName, nodeJsToolchainClass) { spec ->
            configureBuildServiceParameters(spec.parameters)
        }
    }
}

private fun registerIfAbsent(
    project: Project,
): Provider<out NodeJsToolchainService<out NodeJsToolchainService.Parameters>> {
    project.gradle.sharedServices.registrations.findByName(nodeJsServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<NodeJsToolchainService<out NodeJsToolchainService.Parameters>>
    }

    return when (project.kotlinPropertiesProvider.nodeJsToolchainMode) {
        NodeJsToolchainMode.DOWNLOAD -> DefaultNodeJsToolchainServiceImpl.registerIfAbsent(project)
        NodeJsToolchainMode.PREINSTALLED -> PreInstalledNodeJsToolchainServiceImpl.registerIfAbsent(project)
        NodeJsToolchainMode.DISABLE -> DisabledNodeJsToolchainServiceImpl.registerIfAbsent(project.gradle)
    }
}


/**
 * Registers the [NodeJsToolchainService] selected for the build, and makes every
 * [UsesNodeJsToolchainService] task in [project] use it.
 */
internal fun registerNodeJsToolchainServiceIfAbsent(
    project: Project,
) {
    val serviceProvider = registerIfAbsent(project)

    SingleActionPerProject.run(project, UsesNodeJsToolchainService::class.java.name) {
        project.tasks.withType<UsesNodeJsToolchainService>().configureEach { task ->
            task.nodeJsToolchainService.value(serviceProvider).disallowChanges()
            task.usesService(serviceProvider)
        }
        NodeJsToolchainServiceMetrics.collectServiceCreated(project, serviceProvider)
    }

}

/**
 * The oldest Node.js version the Kotlin Gradle Plugin is tested against.
 *
 * Requesting an older version is not forbidden, but produces a warning.
 */
private const val MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION = 18

internal fun Logger.reportDiagnosticWhenNodeJsVersionUnsupported(installedVersion: NodeJsVersion) {
    val installedMajorVersion = installedVersion.majorVersion
    if (installedMajorVersion != null && installedMajorVersion < MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION) {
        warn(
            "Node.js ${installedVersion.normalized} is not supported by the Kotlin Gradle Plugin. " +
                    "The minimal supported version is $MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION. " +
                    "Please use Node.js $MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION or a newer version."
        )
    }
}

/**
 * The Node.js executable inside an installed distribution.
 */
internal fun nodeJsExecutableFile(installationDir: File, platform: BuildPlatform): File {
    val binDir = computeNodeBinDir(installationDir.toPath(), platform.isWindows).toFile()
    return binDir.resolve(if (platform.isWindows) "node.exe" else "node")
}

internal enum class NodeJsToolchainMode {

    /**
     * Node.js distributions are downloaded and installed by [DefaultNodeJsToolchainServiceImpl].
     */
    DOWNLOAD,

    /**
     * A pre-installed Node.js is used by [PreInstalledNodeJsToolchainService], and nothing is downloaded.
     */
    PREINSTALLED,
    DISABLE,
    ;
}

private val DEFAULT_NODE_JS_VERSION = "24.16.0"
internal fun Project.requestDefaultNodeJs(): Provider<NodeJsRequest> = provider {
    objects.newInstance<NodeJsRequest>().also {
        it.version.convention(NodeJsVersion(DEFAULT_NODE_JS_VERSION))
        it.platform.convention(providers.detectBuildPlatform())
    }
}
