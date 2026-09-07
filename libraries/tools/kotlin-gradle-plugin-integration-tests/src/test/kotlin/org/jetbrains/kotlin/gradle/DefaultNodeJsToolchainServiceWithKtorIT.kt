/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.awaitInitialization
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [DefaultNodeJsToolchainService] - the Node.js toolchain that downloads and installs Node.js.
 *
 * The distributions are served by a local Ktor server instead of the official Node.js distribution, so that
 * the tests are hermetic and fast, and a distribution can be broken on purpose. Only the layout of a served
 * distribution is real - the `node` it contains is a stub, and the tests never run it.
 *
 * A download is only verified against the published checksums when it comes from the official Node.js
 * distribution, so the verification is intentionally out of the scope of these tests.
 */
@DisplayName("Node.js toolchain provisioning")
@JsGradlePluginTests
class DefaultNodeJsToolchainServiceWithKtorIT : KGPBaseTest() {
    companion object {
        private const val REQUEST_DEFAULT_NODE_JS_TOOLCHAIN = "-Pkotlin.js.nodejs.toolchain=DOWNLOAD"
        private const val REQUEST_NODE_JS_INSTALLATION_DIR = "-Pkotlin.js.nodejs.toolchain.default.install.path"

        private const val REQUEST_NODE_JS_DOWNLOAD_URL = "-Pkotlin.js.nodejs.toolchain.default.download.url"
    }

    @DisplayName("Each requested Node.js distribution is downloaded only once")
    @GradleTest
    fun testEachDistributionIsDownloadedOnce(gradleVersion: GradleVersion) {
        runWithNodeJsDistributionServer { server ->
            val additionalNodeJsVersion = "250.250.250"
            nodeJsToolchainProject(
                gradleVersion,
                server,
                requestedVersions = listOf(
                    NODE_JS_VERSION,
                    additionalNodeJsVersion,
                    NODE_JS_VERSION,
                ),
            ) { installationDir ->
                assertEquals(
                    listOf(
                        archiveRequest(NODE_JS_VERSION, hostPlatform),
                        archiveRequest(additionalNodeJsVersion, hostPlatform),
                    ).sorted(),
                    server.downloadRequests.sorted(),
                )
                assertEquals(
                    listOf(
                        hostPlatform.distributionName(NODE_JS_VERSION),
                        hostPlatform.distributionName(NODE_JS_VERSION).lockFileDir(),
                        hostPlatform.distributionName(additionalNodeJsVersion),
                        hostPlatform.distributionName(additionalNodeJsVersion).lockFileDir(),
                    ).sorted(),
                    installationDir.installedDistributions(),
                )
            }
        }
    }

    @DisplayName("A Node.js distribution installed by another build is reused")
    @GradleTest
    fun testInstallationIsSharedBetweenBuilds(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        runWithNodeJsDistributionServer { server ->
            val requests = listOf(NODE_JS_VERSION)

            nodeJsToolchainProject(gradleVersion, server, requests, installationsDir = tempDir) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, hostPlatform)), server.downloadRequests)
            }

            nodeJsToolchainProject(gradleVersion, server, requests, installationsDir = tempDir) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, hostPlatform)), server.downloadRequests)
            }
        }
    }

    @DisplayName("A Node.js installation is not overridden")
    @GradleTest
    fun testInstallationsAreNotOverwritten(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        runWithNodeJsDistributionServer { server ->
            val incompleteInstallation = tempDir
                .resolve(hostPlatform.distributionName(NODE_JS_VERSION))
                .createDirectories()
            val leftover = incompleteInstallation.resolve("leftover.txt").also { it.writeText("existed node js should not be overwritten") }

            nodeJsToolchainProject(gradleVersion, server, requestedVersions = listOf(NODE_JS_VERSION), installationsDir = tempDir, buildAction = BuildActions.buildAndFail) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, hostPlatform)), server.downloadRequests)
                assertTrue { leftover.exists() }
            }
        }
    }

    @DisplayName("The platform of a request overrides the default one")
    @GradleTest
    fun testRequestedPlatformOverridesTheDefaultOne(gradleVersion: GradleVersion) {
        val requestedPlatform = BuildPlatform("linux", "arm64")
        runWithNodeJsDistributionServer { server ->
            nodeJsToolchainProject(
                gradleVersion,
                server,
                requestedVersions = listOf(NODE_JS_VERSION),
                requestedPlatform = requestedPlatform,
            ) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, requestedPlatform)), server.downloadRequests)
            }
        }
    }

    @DisplayName("An offline build reuses an already installed Node.js distribution")
    @GradleTest
    fun testOfflineBuildReusesInstallation(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        runWithNodeJsDistributionServer { server ->
            val requests = listOf(NODE_JS_VERSION)

            nodeJsToolchainProject(gradleVersion, server, requests, installationsDir = tempDir) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, hostPlatform)), server.downloadRequests)
            }

            nodeJsToolchainProject(
                gradleVersion,
                server,
                requests,
                installationsDir = tempDir,
                buildOptions = defaultBuildOptions.copy(freeArgs = defaultBuildOptions.freeArgs + "--offline")
            ) {
                assertEquals(listOf(archiveRequest(NODE_JS_VERSION, hostPlatform)), server.downloadRequests)
            }
        }
    }

    @DisplayName("An offline build fails when the requested Node.js distribution is not installed")
    @GradleTest
    fun testOfflineBuildFailsWhenNothingIsInstalled(gradleVersion: GradleVersion) {
        runWithNodeJsDistributionServer { server ->
            nodeJsToolchainProject(
                gradleVersion,
                server,
                requestedVersions = listOf(NODE_JS_VERSION),
                buildAction = BuildActions.buildAndFail,
                buildOptions = defaultBuildOptions.copy(freeArgs = defaultBuildOptions.freeArgs + "--offline"),
                buildAssertions = {
                    assertOutputContains("Node.js $NODE_JS_VERSION for $hostPlatform is not installed")
                    assertOutputContains("the build is running in offline mode")
                }) {
                assertEquals(emptyList(), server.downloadRequests)
            }
        }
    }

    @DisplayName("A download of an unavailable Node.js distribution is retried and then fails")
    @GradleTest
    fun testUnavailableDistributionFailsAfterRetries(gradleVersion: GradleVersion) {
        runWithNodeJsDistributionServer(distribution = { _, _ -> null }) { server ->
            nodeJsToolchainProject(
                gradleVersion,
                server,
                requestedVersions = listOf(NODE_JS_VERSION),
                buildAction = BuildActions.buildAndFail,
                buildAssertions = {
                    val archiveUrl = "${server.downloadBaseUrl}/${archiveRequest(NODE_JS_VERSION, hostPlatform)}"
                    assertOutputContains("Cannot download $archiveUrl")
                    assertOutputContains("$archiveUrl returned HTTP 404 Not Found")
                }
            ) {}
        }
    }

    @DisplayName("A Node.js version older than the supported one is installed with a warning")
    @GradleTest
    fun testUnsupportedVersionIsInstalledWithAWarning(gradleVersion: GradleVersion) {
        runWithNodeJsDistributionServer { server ->
            nodeJsToolchainProject(gradleVersion, server, requestedVersions = listOf(UNSUPPORTED_NODE_JS_VERSION), buildAssertions = {
                assertOutputContains(
                    "Node.js $UNSUPPORTED_NODE_JS_VERSION is not supported by the Kotlin Gradle Plugin. " +
                            "The minimal supported version is 18."
                )
            }) { installationDir ->
                assertEquals(
                    listOf(hostPlatform.distributionName(UNSUPPORTED_NODE_JS_VERSION), hostPlatform.distributionName(UNSUPPORTED_NODE_JS_VERSION).lockFileDir()).sorted(),
                    installationDir.installedDistributions().sorted(),
                )
            }
        }
    }

    @DisplayName("Nothing is provisioned by a build that does not need Node.js")
    @GradleTest
    fun testNothingIsProvisionedWithoutARequest(gradleVersion: GradleVersion) {
        runWithNodeJsDistributionServer { server ->
            // The provisioning task is configured but not executed, and node js is not installed.
            nodeJsToolchainProject(
                gradleVersion,
                server,
                buildTask = "help",
                requestedVersions = listOf(NODE_JS_VERSION)
            ) { installationDir ->
                assertEquals(emptyList(), server.downloadRequests)
                assertEquals(emptyList(), installationDir.installedDistributions())
            }
        }
    }

    /**
     * The directory all the Node.js installations of a test are shared through.
     *
     * It is deliberately outside of the test projects: in a real build it is a machine-wide directory,
     * shared by all the builds on the machine.
     */
    private val TestProject.defaultInstallationsDir: Path get() = projectPath.resolve("nodejs-toolchain")

    private fun nodeJsToolchainProject(
        gradleVersion: GradleVersion,
        server: NodeJsDistributionServer,
        requestedVersions: List<String>,
        buildTask: String? = null,
        installationsDir: Path? = null,
        requestedPlatform: BuildPlatform = hostPlatform,
        buildAction: BuildAction = BuildActions.build,
        buildOptions: BuildOptions = defaultBuildOptions,
        buildAssertions: BuildResult.() -> Unit = {},
        installationAssertions: (Path) -> Unit = {},
    ): TestProject = project(
        "empty",
        gradleVersion,
        buildOptions = buildOptions
    ) {
        addKgpToBuildScriptCompilationClasspath()

        buildScriptInjection {
            // The Node.js toolchain service is registered, and wired into every task that uses it, by the
            // Node.js plugin. It is the only plugin applied, so that nothing but the toolchain is under test.
            project.plugins.apply(NodeJsPlugin::class.java)

            abstract class ProvisionNodeJsTask @Inject constructor(
                objects: ObjectFactory,
            ) : DefaultTask(), UsesNodeJsToolchainService {
                @get:Internal
                val version: Property<String> = objects.property<String>()

                @get:Internal
                val buildPlatform: Property<BuildPlatform> = objects.property<BuildPlatform>()

                @get:Input
                val nodeJsRequest: Provider<NodeJsRequest> = version.zip(buildPlatform) { version, platform ->
                    objects.newInstance(NodeJsRequest::class.java).also {
                        it.version.set(NodeJsVersion(version))
                        it.platform.set(platform)
                    }
                }

                @TaskAction
                fun action() {
                    val nodeJsExecutable = nodeJsToolchainService.get().request(nodeJsRequest.get())
                    val executable = File(nodeJsExecutable.get().executable.get())
                    check(executable.isFile) { "'$executable' is not a provisioned Node.js executable" }
                }
            }

            requestedVersions.forEachIndexed { requestIndex, version ->
                project.tasks.register(provisionTaskName(requestIndex, version), ProvisionNodeJsTask::class.java) { task ->
                    task.version.set(version)
                    task.buildPlatform.set(requestedPlatform)
                }
            }
        }

        val installationsPath = installationsDir ?: defaultInstallationsDir

        buildAction(
            buildTask?.let { arrayOf(it) }
                ?: requestedVersions.mapIndexed(::provisionTaskName).toTypedArray(),
            buildOptions.copy(
                freeArgs = buildOptions.freeArgs + listOf(
                    REQUEST_DEFAULT_NODE_JS_TOOLCHAIN,
                    "$REQUEST_NODE_JS_INSTALLATION_DIR=${installationsPath.absolutePathString()}",
                    "$REQUEST_NODE_JS_DOWNLOAD_URL=${server.downloadBaseUrl}",
                )
            ),
            buildAssertions
        )

        installationAssertions(installationsPath)
    }

    /**
     * The names of everything inside the [installationsDir], except for the lock directory the installer
     * guards the concurrent installations with.
     */
    private fun Path.installedDistributions(): List<String> =
        takeIf { it.exists() }
            ?.listDirectoryEntries()
            ?.map { it.name }
            ?.filter { it != ".locks" }
            ?.sorted()
            ?: emptyList()

    /**
     * Runs [action] with a server that serves Node.js distributions the same way the official one does,
     * at `http://localhost:<port>/dist/v<version>/<archive name>`.
     *
     * @param distribution the served archive, or `null` to respond with `404`, as the official distribution
     * does for a version that does not exist.
     */
    private fun runWithNodeJsDistributionServer(
        distribution: (archiveName: String, version: String) -> ByteArray? = { archiveName, _ ->
            nodeJsDistributionArchive(archiveName)
        },
        action: (NodeJsDistributionServer) -> Unit,
    ) {
        val downloadRequests = CopyOnWriteArrayList<String>()
        var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
        try {
            server = embeddedServer(CIO, host = "localhost", port = 0) {
                routing {
                    get("/isReady") {
                        call.respond(HttpStatusCode.OK)
                    }
                    get("/dist/v{version}/{archive}") {
                        val version = call.parameters["version"]!!
                        val archiveName = call.parameters["archive"]!!
                        downloadRequests.add("v$version/$archiveName")

                        val archive = distribution(archiveName, version)
                        if (archive == null) {
                            call.respond(HttpStatusCode.NotFound, "No such Node.js distribution")
                        } else {
                            call.respondBytes(archive, ContentType.Application.OctetStream)
                        }
                    }
                }
            }.start()
            val port = runBlocking { server.engine.resolvedConnectors().single().port }
            awaitInitialization(port)
            action(NodeJsDistributionServer(port, downloadRequests))
        } finally {
            server?.stop(1000, 1000)
        }
    }

    private class NodeJsDistributionServer(
        port: Int,
        private val requests: List<String>,
    ) {
        val downloadBaseUrl: String = "http://localhost:$port/dist"

        /** The requested archives, `v<version>/<archive name>` each, in the order they were requested in. */
        val downloadRequests: List<String> get() = requests.toList()
    }
}

private const val PROVISION_TASK_NAME = "provisionNodeJs"

/**
 * The name of the task provisioning the [version] requested by the [requestIndex]-th request.
 *
 * The index keeps the names unique when the same version is requested more than once.
 */
private fun provisionTaskName(requestIndex: Int, version: String): String = "$PROVISION_TASK_NAME-$requestIndex-$version"

private const val NODE_JS_VERSION = "24.16.0"


/** A version older than the minimal one the Kotlin Gradle Plugin supports. */
private const val UNSUPPORTED_NODE_JS_VERSION = "16.20.2"

/** The operating system name the Windows Node.js distributions are published under. */
private const val WINDOWS_OS = "win"

/**
 * The platform the distributions are requested for.
 *
 * It is the platform of the host, because that is the one the toolchain requests by default. The operating
 * system decides both the format of the archive and the layout of the installation, and, unlike a Windows
 * one, a Unix installation is post-processed by the installer.
 */
private val hostPlatform: BuildPlatform = BuildPlatform(
    os = when {
        HostManager.hostIsMingw -> WINDOWS_OS
        HostManager.hostIsMac -> "darwin"
        else -> "linux"
    },
    arch = when (val arch = System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "arm64"
        else -> if ("64" in arch) "x64" else "x86"
    },
)

/**
 * The naming of the official distributions, spelled out independently of the production code,
 * for example `node-v24.16.0-linux-x64`.
 */
private fun BuildPlatform.distributionName(version: String): String = "node-v$version-$os-$arch"

private fun BuildPlatform.archiveName(version: String): String =
    "${distributionName(version)}." + if (os == WINDOWS_OS) "zip" else "tar.gz"

private fun archiveRequest(version: String, platform: BuildPlatform): String =
    "v$version/${platform.archiveName(version)}"

/**
 * An archive with the same layout as the official Node.js distribution [archiveName], but with a stub
 * instead of a real `node` executable.
 *
 * @param rootDirName the single directory the archive contains, by default the one the installer expects.
 */
private fun nodeJsDistributionArchive(
    archiveName: String,
    rootDirName: String = archiveName.removeSuffix(".zip").removeSuffix(".tar.gz"),
): ByteArray =
    if (archiveName.endsWith(".zip")) {
        windowsDistributionArchive(rootDirName)
    } else {
        unixDistributionArchive(rootDirName)
    }

private fun unixDistributionArchive(rootDirName: String): ByteArray {
    val archive = ByteArrayOutputStream()
    TarArchiveOutputStream(GZIPOutputStream(archive)).use { tar ->
        // `node` is intentionally not executable, so that the installer has to make it executable.
        tar.addEntry("$rootDirName/bin/node", NODE_JS_STUB)
        // `npm` and `npx` are symlinks in the official archive. Symlinks do not survive the unpacking,
        // so they are served as the plain files the unpacking produces.
        tar.addEntry("$rootDirName/bin/npm", "")
        tar.addEntry("$rootDirName/bin/npx", "")
        tar.addEntry("$rootDirName/lib/node_modules/npm/bin/npm-cli.js", NPM_CLI_STUB)
        tar.addEntry("$rootDirName/lib/node_modules/npm/bin/npx-cli.js", NPM_CLI_STUB)
    }
    return archive.toByteArray()
}

private fun windowsDistributionArchive(rootDirName: String): ByteArray {
    val archive = ByteArrayOutputStream()
    ZipOutputStream(archive).use { zip ->
        zip.addEntry("$rootDirName/bin/node", NODE_JS_STUB)
        zip.addEntry("$rootDirName/node.exe", NODE_JS_STUB)
        zip.addEntry("$rootDirName/npm.cmd", "")
        zip.addEntry("$rootDirName/node_modules/npm/bin/npm-cli.js", NPM_CLI_STUB)
    }
    return archive.toByteArray()
}

private fun TarArchiveOutputStream.addEntry(path: String, content: String) {
    val bytes = content.toByteArray()
    putArchiveEntry(TarArchiveEntry(path).also { it.size = bytes.size.toLong() })
    write(bytes)
    closeArchiveEntry()
}

private fun ZipOutputStream.addEntry(path: String, content: String) {
    putNextEntry(ZipEntry(path))
    write(content.toByteArray())
    closeEntry()
}

private fun String.lockFileDir() = ".${this}.lock"

private const val NODE_JS_STUB = "#!/bin/sh\necho 'not a real Node.js'\n"
private const val NPM_CLI_STUB = "// not a real npm\n"
