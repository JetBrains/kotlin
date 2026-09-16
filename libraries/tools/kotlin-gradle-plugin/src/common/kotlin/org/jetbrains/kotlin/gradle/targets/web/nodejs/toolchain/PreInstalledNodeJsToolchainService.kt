/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsServiceName
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class PreInstalledNodeJsToolchainService @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val execOperations: ExecOperations,
) : NodeJsToolchainService<PreInstalledNodeJsToolchainService.Parameters> {

    abstract class Parameters : NodeJsToolchainService.Parameters {
        abstract val nodeJsExecutable: Property<String>
    }

    private val logger = Logging.getLogger(PreInstalledNodeJsToolchainService::class.java)

    override fun request(configure: NodeJsRequest.() -> Unit): Provider<NodeJsExecutable> {
        val request = objects.newInstance<NodeJsRequest>()
        request.configure()

        val requestedVersion = request.version
        val requestedPlatform = request.platform

        return providers.provider {
            val command = parameters.nodeJsExecutable.get()
            val (installedVersion, installedPlatform) = detectInstalledNodeJs(command)

            val requested = requestedVersion.orNull
            if (requested != null && requested.normalized != installedVersion.normalized) {
                logger.warn(
                    "w: Node.js $installedVersion found by '$command' does not match the requested " +
                            "version $requested. The requested version cannot be provisioned, because " +
                            "the Node.js toolchain is configured to use a pre-installed Node.js."
                )
            }
            requestedPlatform.orNull?.let { platform ->
                if (platform != installedPlatform) {
                    logger.warn(
                        "w: Node.js found by '$command' runs on $installedPlatform, " +
                                "but $platform was requested."
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
        private const val DETECT_SCRIPT = "[process.versions.node, process.platform, process.arch].join(`$DELIMITER`)"

        internal fun registerIfAbsent(project: Project): Provider<PreInstalledNodeJsToolchainService> {
            return project.gradle.sharedServices.registerIfAbsent(
                nodeJsServiceName,
                PreInstalledNodeJsToolchainService::class.java
            ) { spec ->
                spec.parameters.nodeJsExecutable.set(project.kotlinPropertiesProvider.nodeJsToolchainLocalPath.getOrElse("node"))
            }
        }
    }
}
