/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.BuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsVersion
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.PreInstalledNodeJsToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * A [NodeJsToolchainService] that uses a pre-installed Node.js instead of downloading one.
 */
@OptIn(ExperimentalNodeJsToolchainDsl::class)
internal abstract class PreInstalledNodeJsToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val execOperations: ExecOperations,
) : PreInstalledNodeJsToolchainService {

    private val logger = Logging.getLogger(PreInstalledNodeJsToolchainService::class.java)

    private val detectedNodeJsCache = ConcurrentHashMap<String, Pair<NodeJsVersion, BuildPlatform>>()

    override fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable> {

        return parameters.nodeJsExecutable.map { command ->
            val (installedVersion, installedPlatform) = detectedNodeJsCache[command]
                ?: detectInstalledNodeJs(command).also { detectedNodeJsCache.putIfAbsent(command, it) }

            logger.reportDiagnosticWhenNodeJsVersionUnsupported(installedVersion)

            val requestedVersion = nodeJsRequest.version.orNull
            if (requestedVersion != null && requestedVersion.normalized != installedVersion.normalized) {
                logger.warn(
                    "Node.js $installedVersion found by '$command' does not match the requested " +
                            "version $requestedVersion. The requested version cannot be provisioned, because " +
                            "the Node.js toolchain is configured to use a pre-installed Node.js. " +
                            "Please update the pre-installed Node.js or configure the Kotlin Gradle Plugin to download Node.js by setting kotlin.js.nodejs.toolchain=DOWNLOAD."
                )
            }
            nodeJsRequest.platform.orNull?.let { platform ->
                if (platform != installedPlatform) {
                    logger.warn(
                        "Node.js found by '$command' runs on $installedPlatform, but $platform was requested."
                    )
                }
            }

            objects.newInstance<NodeJsExecutable>().apply {
                executable.set(command)
                version.set(installedVersion)
                platform.set(installedPlatform)
            }
        }
    }

    /**
     * Asks Node.js itself about its version and platform, which is both accurate and cheap - it is a single
     * process, and Node.js reports the very same `platform`/`arch` names as the distribution archives do.
     */
    private fun detectInstalledNodeJs(command: String): Pair<NodeJsVersion, BuildPlatform> {
        val output = ByteArrayOutputStream()
        try {
            execOperations.exec { spec ->
                spec.executable(command)
                spec.args("-p", DETECT_SCRIPT)
                spec.standardOutput = output
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Cannot run '$command' to detect the pre-installed Node.js. " +
                        "Make sure Node.js is installed and available on the PATH.",
                e
            )
        }

        val parts = output.toString(Charsets.UTF_8.name()).trim().split(DELIMITER)
        check(parts.size == 3) { "Unexpected output of '$command -p $DETECT_SCRIPT': $parts" }

        val (version, os, arch) = parts
        return NodeJsVersion(version) to BuildPlatform(if (os == "win32") WINDOWS_OS_NAME else os, arch)
    }

    companion object {
        private const val DELIMITER: Char = ' '
        private const val DETECT_SCRIPT = "[process.versions.node, process.platform, process.arch].join('$DELIMITER')"

        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (PreInstalledNodeJsToolchainService.Parameters) -> Unit,
        ): Provider<PreInstalledNodeJsToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(
                nodeJsServiceName,
                PreInstalledNodeJsToolchainServiceImpl::class.java,
            ) { spec ->
                spec.parameters.nodeJsExecutable.convention("node")
                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<PreInstalledNodeJsToolchainServiceImpl> {
            return registerIfAbsent(
                project.gradle,
            ) { parameters ->
                parameters.nodeJsExecutable.set(project.kotlinPropertiesProvider.nodeJsToolchainLocalPath.orElse("node"))
            }
        }
    }
}
