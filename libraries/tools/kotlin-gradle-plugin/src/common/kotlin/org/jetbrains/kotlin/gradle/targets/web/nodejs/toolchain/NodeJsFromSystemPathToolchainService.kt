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
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * A [NodeJsToolchainService] that uses a pre-installed Node.js instead of downloading one.
 *
 * Nothing is ever downloaded, which makes it a first-class mode for air-gapped, sandboxed
 * and containerised environments: it is enough to have `node` available on the `PATH`,
 * or to point [Parameters.command] at a Node.js executable.
 *
 * The pre-installed Node.js is trusted as configured and is not verified. Its actual version is detected
 * by running `node`, and a mismatch with the requested version is reported as a warning - the requested
 * version cannot be satisfied by any other means.
 */
abstract class NodeJsFromSystemPathToolchainService @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val execOperations: ExecOperations,
) : NodeJsToolchainService<NodeJsFromSystemPathToolchainService.Parameters> {

    abstract class Parameters : NodeJsToolchainService.Parameters {

        /**
         * The Node.js executable to use.
         *
         * Defaults to `node`, which is resolved using the `PATH` environment variable.
         */
        abstract val command: Property<String>
    }

    private val logger = Logging.getLogger(NodeJsFromSystemPathToolchainService::class.java)

    override fun request(configure: NodeJsRequest.() -> Unit): Provider<NodeJsExecutable> {
        val request = objects.newInstance<NodeJsRequest>()
        request.configure()

        val requestedVersion = request.version
        val requestedPlatform = request.platform

        return providers.provider {
            val command = parameters.command.getOrElse(DEFAULT_COMMAND)
            val installed = detectInstalledNodeJs(command)

            val requested = requestedVersion.orNull
            if (requested != null && requested.normalized != installed.version.normalized) {
                logger.warn(
                    "w: Node.js ${installed.version} found by '$command' does not match the requested " +
                            "version $requested. The requested version cannot be provisioned, because " +
                            "the Node.js toolchain is configured to use a pre-installed Node.js."
                )
            }
            requestedPlatform.orNull?.let { platform ->
                if (platform != installed.platform) {
                    logger.warn(
                        "w: Node.js found by '$command' runs on ${installed.platform}, " +
                                "but $platform was requested."
                    )
                }
            }

            objects.newInstance<NodeJsExecutable>().apply {
                executable.set(command)
                version.set(installed.version)
                platform.set(installed.platform)
            }
        }
    }

    /**
     * Asks Node.js itself about its version and platform, which is both accurate and cheap - it is a single
     * process, and Node.js reports the very same `platform`/`arch` names as the distribution archives do.
     */
    private fun detectInstalledNodeJs(command: String): InstalledNodeJs {
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

        val parts = output.toString(Charsets.UTF_8.name()).trim().split(' ')
        check(parts.size == 3) { "Unexpected output of '$command -p $DETECT_SCRIPT': $parts" }

        val (version, os, arch) = parts
        return InstalledNodeJs(
            version = NodeJsVersion(version),
            // Node.js reports Windows as `win32`, while the distributions are named `win`.
            platform = BuildPlatform(if (os == "win32") WINDOWS_OS_NAME else os, arch),
        )
    }

    private data class InstalledNodeJs(
        val version: NodeJsVersion,
        val platform: BuildPlatform,
    )

    companion object {
        private const val DEFAULT_COMMAND = "node"

        private const val DETECT_SCRIPT = "[process.versions.node, process.platform, process.arch].join(' ')"

        internal fun registerIfAbsent(project: Project): Provider<NodeJsFromSystemPathToolchainService>{
            return project.gradle.sharedServices.registerIfAbsent(nodeJsServiceName, NodeJsFromSystemPathToolchainService::class.java)
        }
    }
}
