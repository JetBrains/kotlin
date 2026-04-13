/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.FetchSyntheticImportProjectPackages
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.ConvertSyntheticSwiftPMImportProjectIntoDefFile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PackageResolvedSynchronization
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.XCTestHelpers
import org.jetbrains.kotlin.gradle.testbase.assertFileExists
import org.jetbrains.kotlin.gradle.testbase.boot
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.GradleMetadata
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.Variant
import org.jetbrains.kotlin.gradle.uklibs.VariantFile
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.uklibs.dumpKlibMetadataSignatures
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText
import kotlin.io.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("INVISIBLE_REFERENCE")
const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME = GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME

// region Local Swift Package Creation Utilities

enum class SwiftPackageSourceLanguage {
    SWIFT_WITH_OBJC,
    OBJC,
    CXX_WITH_C_HEADER,
    CXX,
    SWIFT,
}

@Suppress("INVISIBLE_REFERENCE")
const val SYNTHETIC_IMPORT_DYLIB =
    GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_DYLIB

fun createLocalSwiftPackage(
    localPackageDir: Path,
    packageName: String = "LocalSwiftPackage",
    sourceLanguage: SwiftPackageSourceLanguage = SwiftPackageSourceLanguage.SWIFT_WITH_OBJC,
) {
    localPackageDir.createDirectories()
    val sourcesDir = localPackageDir.resolve("Sources/$packageName")
    writePackageManifest(localPackageDir, packageName, ".target(name: \"$packageName\")")
    writeLocalPackageSources(sourcesDir, packageName, sourceLanguage)
}

fun createLocalSwiftPackageWithResources(
    localPackageDir: Path,
    packageName: String = "LocalSwiftPackage",
    resourceFileName: String = "greeting.txt",
    resourceContent: String = "Hello from SPM resource",
) {
    localPackageDir.createDirectories()
    val sourcesDir = localPackageDir.resolve("Sources/$packageName")
    sourcesDir.createDirectories()

    // Create the resource file
    sourcesDir.resolve(resourceFileName).writeText(resourceContent)

    // Write Package.swift with resource processing
    writePackageManifest(
        localPackageDir, packageName,
        """
            .target(
                name: "$packageName",
                resources: [
                    .process("$resourceFileName"),
                ]
            ),
        """.trimIndent()
    )

    // Write Swift source that exposes a resource accessor
    sourcesDir.resolve("$packageName.swift").writeText(
        """
            import Foundation

            @objc public class ResourceAccessor: NSObject {
                @objc public static func resourceContent() -> String {
                    guard let url = Bundle.module.url(forResource: "${resourceFileName.substringBeforeLast(".")}", withExtension: "${resourceFileName.substringAfterLast(".")}") else {
                        return "RESOURCE_NOT_FOUND"
                    }
                    return (try? String(contentsOf: url)) ?? "RESOURCE_READ_ERROR"
                }

                @objc public static func resourceBundle() -> Bundle {
                    return Bundle.module
                }
            }
        """.trimIndent()
    )
}

internal fun writePackageManifest(
    localPackageDir: Path,
    packageName: String,
    targetDefinition: String,
) {
    localPackageDir.resolve("Package.swift").writeText(
        """
            // swift-tools-version: 5.9
            import PackageDescription

            let package = Package(
                name: "$packageName",
                platforms: [.iOS(.v15)],
                products: [
                    .library(name: "$packageName", targets: ["$packageName"]),
                ],
                targets: [
                    ${targetDefinition.prependIndent("            ")}
                ]
            )
        """.trimIndent()
    )
}

internal fun writeLocalPackageSources(
    sourcesDir: Path,
    packageName: String,
    sourceLanguage: SwiftPackageSourceLanguage,
) {
    sourcesDir.createDirectories()
    when (sourceLanguage) {
        SwiftPackageSourceLanguage.CXX_WITH_C_HEADER -> {
            val includeDir = sourcesDir.resolve("include").createDirectories()
            includeDir.resolve("$packageName.h").writeText(
                """
                    #ifndef ${packageName}_h
                    #define ${packageName}_h

                    #ifdef __cplusplus
                    extern "C" {
                    #endif

                    const char* cxx_greeting(void);

                    #ifdef __cplusplus
                    }
                    #endif

                    #endif /* ${packageName}_h */
                """.trimIndent()
            )
            sourcesDir.resolve("$packageName.cpp").writeText(
                """
                    #include "include/$packageName.h"
                    #include <string>

                    const char* cxx_greeting(void) {
                        std::string msg = "Hello from C++";
                        return msg.c_str();
                    }
                """.trimIndent()
            )
        }
        SwiftPackageSourceLanguage.CXX -> {
            val includeDir = sourcesDir.resolve("include").createDirectories()
            includeDir.resolve("$packageName.h").writeText(
                """
                    #ifndef ${packageName}_h
                    #define ${packageName}_h

                    #include <string>

                    std::string cxx_greeting();

                    #endif /* ${packageName}_h */
                """.trimIndent()
            )
            sourcesDir.resolve("$packageName.cpp").writeText(
                """
                    #include "include/$packageName.h"

                    std::string cxx_greeting() {
                        return "Hello from C++";
                    }
                """.trimIndent()
            )
        }
        SwiftPackageSourceLanguage.OBJC -> {
            val includeDir = sourcesDir.resolve("include").createDirectories()
            includeDir.resolve("$packageName.h").writeText(
                """
                    #import <Foundation/Foundation.h>

                    @interface LocalHelper : NSObject
                    + (NSString *)greeting;
                    @end
                """.trimIndent()
            )
            sourcesDir.resolve("$packageName.m").writeText(
                """
                    #import "$packageName.h"

                    @implementation LocalHelper
                    + (NSString *)greeting {
                        return @"Hello from LocalHelper";
                    }
                    @end
                """.trimIndent()
            )
        }
        SwiftPackageSourceLanguage.SWIFT_WITH_OBJC -> {
            sourcesDir.resolve("$packageName.swift").writeText(swiftSourceContent())
        }
        SwiftPackageSourceLanguage.SWIFT -> {
            sourcesDir.resolve("$packageName.swift").writeText(
                """
                    import Foundation
                
                    public class PureSwiftHelper {
                        public static func greeting() -> String {
                            return "Hello from PureSwiftHelper"
                        }
                    }
                """.trimIndent()
            )
        }
    }
}

internal fun swiftSourceContent(): String = """
    import Foundation

    @objc public class LocalHelper: NSObject {
        @objc public static func greeting() -> String {
            return "Hello from LocalHelper"
        }
        
        public static func invisible() -> String {
            return "This function is not visible in cinterop"
        }
    }
""".trimIndent()

fun createLocalSwiftPackageWithBinaryTarget(
    localPackageDir: Path,
    packageName: String,
    xcframeworkPath: Path
) {
    localPackageDir.createDirectories()
    writePackageManifest(
        localPackageDir = localPackageDir,
        packageName = packageName,
        targetDefinition = """
            .binaryTarget(
                name: "$packageName",
                path: "${xcframeworkPath.fileName}"
            ),
        """.trimIndent(),
    )
}

// endregion


/**
 * Creates a minimal SwiftPM package repo with N tags (each tag is its own commit).
 * Product == target == package name (simplifies SPM consumption).
 */
internal fun createSwiftPmGitRepoWithTags(
    reposRoot: Path,
    packageName: String,
    tags: List<String>,
    fileByTag: Map<String, Map<String, String>> = emptyMap(),
): Path {
    val repoDir = reposRoot.resolve(packageName).createDirectories()

    runGit("init", "-q", repoDir = repoDir)
    runGit("config", "user.email", "spm@test", repoDir = repoDir)
    runGit("config", "user.name", "spm-test", repoDir = repoDir)
    val commandResult = runProcess(
        listOf("touch", "git-daemon-export-ok"),
        workingDir = repoDir.toFile(),
    )

    if (!commandResult.isSuccessful) throw IllegalStateException("Failed to create git-daemon-export-ok file: ${commandResult.output}")

    writePackageManifest(repoDir, packageName, ".target(name: \"$packageName\")")

    // Seed sources dir
    repoDir.resolve("Sources/$packageName").createDirectories()
    repoDir.resolve("Sources/$packageName/$packageName.swift").writeText(
        "public struct $packageName { public static let v = \"seed\" }\n"
    )

    runGit("add", ".", repoDir = repoDir)
    runGit("commit", "--quiet", "-m", "init", repoDir = repoDir)

    tags.forEach { tag ->
        // If caller provided per-tag content, apply it; otherwise just update a value.
        val files = fileByTag[tag]
            ?: mapOf("Sources/$packageName/$packageName.swift" to "public struct $packageName { public static let v = \"$tag\" }\n")

        files.forEach { (rel, content) ->
            val f = repoDir.resolve(rel)
            f.parent.createDirectories()
            f.writeText(content)
        }

        runGit("add", ".", repoDir = repoDir)
        runGit("commit", "--quiet", "-m", "release $tag", repoDir = repoDir)
        runGit("tag", tag, repoDir = repoDir)
    }

    return repoDir
}

/**
 * Adds a new tag to an existing SwiftPM repo (commit + tag).
 */
internal fun addSwiftPmGitTag(
    repoDir: Path,
    tag: String,
    files: Map<String, String>? = null,
) {
    files?.forEach { (filePath, fileContent) ->
        val f = repoDir.resolve(filePath)
        f.parent.createDirectories()
        f.writeText(fileContent)
    }

    runGit("add", ".", repoDir = repoDir)
    runGit("commit", "--quiet", "-m", "release $tag", repoDir = repoDir)
    runGit("tag", tag, repoDir = repoDir)
}

internal fun TestProject.initDefaultKmp(extra: KotlinMultiplatformExtension.() -> Unit = {}) {
    plugins {
        kotlin("multiplatform")
    }
    buildScriptInjection {
        project.applyMultiplatform {
            listOf(
                iosArm64(),
                iosSimulatorArm64()
            ).forEach {
                it.binaries.framework {
                    baseName = "Shared"
                    isStatic = true
                }
            }
            extra()
        }
    }
}

internal class LockFileTestFixture(
    val project: TestProject,
    val cacheDirFile: File,
    val reposRoot: Path,
    val daemon: GitDaemon,
    val packageResolvedSynchronization: PackageResolvedSynchronization,
    val persistedPackageResolvedSyncPath: Path =
        project.selectedPersistedPackageResolvedPath(packageResolvedSynchronization),
)

internal data class RepoRef(
    val name: String,
    val url: String,
) : java.io.Serializable

internal fun TestProject.withLockFileFixture(
    packageResolvedSynchronization: PackageResolvedSynchronization = PackageResolvedSynchronization.Identifier("default"),
    block: LockFileTestFixture.() -> Unit,
) {
    val cacheDirFile = projectPath.resolve("customXcodePackageCache").toFile()
    val reposRoot = projectPath.resolve("spmRepos").apply { createDirectories() }
    val daemon = GitDaemon(
        reposRoot,
        logFile = projectPath.resolve("gitDaemonLogs.log").apply { createFile() }.toFile(),
    )

    daemon.useWithFailure {
        start()
        LockFileTestFixture(
            project = this@withLockFileFixture,
            cacheDirFile = cacheDirFile,
            reposRoot = reposRoot,
            daemon = this,
            packageResolvedSynchronization,
        ).block()
    }
}

internal fun TestProject.selectedPersistedPackageResolvedPath(
    sync: PackageResolvedSynchronization,
): Path =
    when (sync) {
        is PackageResolvedSynchronization.Identifier ->
            projectPath.resolve(".swiftpm-locks/${sync.identifier}/swiftImport/Package.resolved")

        PackageResolvedSynchronization.None ->
            projectPath.resolve("Package.resolved")
    }

internal fun TestProject.initSwiftPmProject(
    cacheDirFile: File,
    extra: KotlinMultiplatformExtension.() -> Unit,
) {
    initDefaultKmp {
        project.tasks
            .withType(FetchSyntheticImportProjectPackages::class.java)
            .configureEach { task ->
                task.additionalXcodeArgs.set(
                    listOf(
                        "-packageFingerprintPolicy", "warn",
                        "-packageCachePath", cacheDirFile.path,
                    )
                )
                task.additionalSwiftPackageResolveArgs.set(
                    listOf(
                        "--resolver-fingerprint-checking", "warn",
                    )
                )
            }

        project.tasks
            .withType(ConvertSyntheticSwiftPMImportProjectIntoDefFile::class.java)
            .configureEach { task ->
                task.additionalXcodeArgs.set(
                    listOf(
                        "-packageFingerprintPolicy", "warn",
                        "-packageCachePath", cacheDirFile.path,
                    )
                )
            }

        extra()
    }
}

internal fun LockFileTestFixture.createRepo(
    name: String,
    tags: List<String>,
): Path {
    return createSwiftPmGitRepoWithTags(
        reposRoot = reposRoot,
        packageName = name,
        tags = tags,
    )
}

internal fun LockFileTestFixture.repoRef(name: String): RepoRef =
    RepoRef(name = name, url = daemon.urlFor(name))

internal fun LockFileTestFixture.repoDir(name: String): Path =
    reposRoot.resolve(name)

internal fun LockFileTestFixture.releaseTag(
    repoName: String,
    tag: String,
) {
    val repoDir = repoDir(repoName)

    addSwiftPmGitTag(
        repoDir = repoDir,
        tag = tag,
        files = mapOf(
            "Sources/$repoName/${repoName}_${tag.replace('.', '_')}.swift" to
                    "public struct ${repoName}_${tag.replace('.', '_')} { public static let v = \"$tag\" }\n"
        )
    )
}

internal fun BuildResult.assertResolvedVersions(
    persistedPackageResolved: Path,
    checkoutRepoDir: Path? = null,
    expectedPins: List<Pair<RepoRef, String>>,
) {
    assertFileExists(persistedPackageResolved, "Project directory Package.resolved should be generated")

    val actual = parsePackageResolved(persistedPackageResolved.readText())

    val expected = SwiftPmPackageResolved(
        pins = expectedPins.map { (repoRef, version) ->
            checkoutRepoDir?.let { checkoutRepoDir ->
                assertCheckoutVersion(checkoutRepoDir, repoRef, version)
            }
            SwiftPmPin(
                identity = repoRef.name.lowercase(),
                kind = "remoteSourceControl",
                location = repoRef.url,
                state = SwiftPmPinState(
                    revision = "<ignored>",
                    version = version,
                )
            )
        },
        version = 2,
    )

    assertEquals(expected.sortedPins(), actual.sortedPins().ignoreRevisions())
}

private fun SwiftPmPackageResolved.sortedPins(): SwiftPmPackageResolved =
    copy(pins = pins.sortedBy { it.identity })

private fun assertCheckoutVersion(checkoutRepoDir: Path, repoRef: RepoRef, version: String) {
    val gitCheckoutTag = runGit(
        "tag", "--points-at", "HEAD", repoDir = checkoutRepoDir.resolve(repoRef.name)
    ).trim()

    assertEquals(
        gitCheckoutTag, version, "Project directory Package.resolved should still have the same version as the tag"
    )
}


// Package.resolved DTO

/**
 * Example Package.resolved
 * {
 *   "pins" : [
 *     {
 *       "identity" : "aws-sdk-ios-spm",
 *       "kind" : "remoteSourceControl",
 *       "location" : "https://github.com/aws-amplify/aws-sdk-ios-spm.git",
 *       "state" : {
 *         "revision" : "f2e9fbed08dd14a962f8d62d7e302d73b9ecb429",
 *         "version" : "2.41.0"
 *       }
 *     },
 *     {
 *       "identity" : "swift-protobuf",
 *       "kind" : "remoteSourceControl",
 *       "location" : "https://github.com/apple/swift-protobuf.git",
 *       "state" : {
 *         "revision" : "c6fe6442e6a64250495669325044052e113e990c",
 *         "version" : "1.32.0"
 *       }
 *     }
 *   ],
 *   "version" : 2
 * }
 */
@Serializable
internal data class SwiftPmPackageResolved(
    val pins: List<SwiftPmPin>,
    val version: Int,
)

@Serializable
internal data class SwiftPmPin(
    val identity: String,
    val kind: String,
    val location: String,
    val state: SwiftPmPinState,
)

@Serializable
internal data class SwiftPmPinState(
    val revision: String,
    val version: String? = null,
    val branch: String? = null,
)

private val swiftPmJson = Json {
    ignoreUnknownKeys = true
}


internal fun SwiftPmPackageResolved.ignoreRevisions(): SwiftPmPackageResolved =
    copy(pins = pins.map { it.copy(state = it.state.copy(revision = "<ignored>")) })

internal fun SwiftPmPackageResolved.ignoreTopLevelVersion(): SwiftPmPackageResolved =
    copy(version = -1)

internal fun parsePackageResolved(jsonString: String): SwiftPmPackageResolved = swiftPmJson.decodeFromString(jsonString)

// Package.resolved DTO

// region swift package description DTOs


@Serializable
data class SwiftPackageDescription(
    val dependencies: List<SwiftPackageDependency> = emptyList(),
    @SerialName("manifest_display_name") val manifestDisplayName: String,
    val name: String,
    val path: String,
    val platforms: List<SwiftPackagePlatform> = emptyList(),
    val products: List<SwiftPackageProduct> = emptyList(),
    val targets: List<SwiftPackageTarget> = emptyList(),
    @SerialName("tools_version") val toolsVersion: String,
)

@Serializable
data class SwiftPackageDependency(
    val identity: String,
    val requirement: SwiftPackageDependencyRequirement? = null,
    val type: String,
    val url: String? = null,
)

@Serializable
data class SwiftPackageDependencyRequirement(
    val range: List<SwiftPackageVersionRange>? = null,
)

@Serializable
data class SwiftPackageVersionRange(
    @SerialName("lower_bound") val lowerBound: String,
    @SerialName("upper_bound") val upperBound: String,
)

@Serializable
data class SwiftPackagePlatform(
    val name: String,
    val version: String,
)

@Serializable
data class SwiftPackageProduct(
    val name: String,
    val targets: List<String> = emptyList(),
    val type: SwiftPackageProductType,
)

@Serializable
data class SwiftPackageProductType(
    val library: List<SwiftPackageLibraryType>? = null,
    val executable: Boolean? = null,
)

@Serializable
enum class SwiftPackageLibraryType {
    @SerialName("dynamic")
    DYNAMIC,

    @SerialName("static")
    STATIC,

    @SerialName("automatic") // but in Package.swift it's '.none'
    AUTOMATIC,
}

@Serializable
data class SwiftPackageTarget(
    @SerialName("module_type") val moduleType: String,
    val name: String,
    val path: String,
    @SerialName("product_dependencies") val productDependencies: List<String> = emptyList(),
    @SerialName("product_memberships") val productMemberships: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val type: String,
)

// endregion

private val appleToolJson = Json {
    ignoreUnknownKeys = true
}

private inline fun <reified T> runAppleToolCommand(
    workingDir: Path,
    command: List<String>,
    outputFile: File? = null,
): T {
    val result = runProcess(
        cmd = command,
        workingDir = workingDir.toFile(),
    )
    require(result.isSuccessful) {
        "Failed to run command ${command.joinToString(" ")} at $workingDir: ${result.output}"
    }
    val jsonContent = outputFile?.readText() ?: result.output
    return appleToolJson.decodeFromString<T>(jsonContent)
}

fun describeSwiftPackage(packagePath: Path): SwiftPackageDescription {
    return runAppleToolCommand(packagePath, listOf("/usr/bin/swift", "package", "describe", "--type", "json"))
}

/**
 * Local git-daemon helper for tests:
 * - serves repos under [baseDir]
 * - repositories are accessible as git://[host]:[port]/
 */

internal class GitDaemon(
    private val baseDir: Path,
    private val logFile: File,
    private val host: String = "127.0.0.1",
    private var port: Int = 0,
) : Closeable {

    private var process: Process? = null
    private var logDumpThread: Thread? = null

    fun start(): GitDaemon {
        if (port == 0) {
            port = findFreePort()
        }

        val pb = ProcessBuilder(
            "git",
            "daemon",
            "--verbose",
            "--reuseaddr",
            "--export-all",
            "--base-path=${baseDir.toAbsolutePath()}",
            "--listen=$host",
            "--port=$port",
            baseDir.toAbsolutePath().toString(),
        ).redirectErrorStream(true)

        val process = pb.start()
        this.process = process
        waitUntilReady(process)

        return this
    }

    fun urlFor(repoName: String): String = "git://$host:$port/$repoName"

    fun dumpDaemonLogs() {
        val logs = dumpDaemonLogsToString()
        if (logs.isNotBlank()) {
            println("=== git daemon logs ===")
            println(logs)
            println("=== end git daemon logs ===")
        }
    }

    private fun waitUntilReady(process: Process) {
        if (!process.isAlive) error("Git daemon process is not alive")

        val sema = Semaphore(0)
        logDumpThread = thread {
            logFile.writer().use { fileWriteStream ->
                process.inputStream.reader().useLines {
                    it.forEach { line ->
                        if (line.contains("Ready to rumble")) {
                            sema.release()
                        }
                        fileWriteStream.appendLine(line)
                        fileWriteStream.flush()
                    }
                }
            }
        }
        sema.acquire()

        return
    }

    fun log(message: String) {
        logFile.appendText(message + System.lineSeparator())
    }

    fun dumpDaemonLogsToString(): String =
        if (logFile.exists()) logFile.readText() else "<log file missing>"

    override fun close() {
        process?.destroy()
        logDumpThread?.join(10_000)
        if (logDumpThread?.isAlive == true) error("Failed to stop git daemon log dump thread")
        process = null
    }

    private fun findFreePort(): Int =
        java.net.ServerSocket(0).use { it.localPort }
}

internal fun <R> GitDaemon.useWithFailure(
    onFailure: GitDaemon.(Throwable) -> Unit = { dumpDaemonLogs() },
    block: GitDaemon.() -> R,
): R {
    try {
        return use {
            block()
        }
    } catch (t: Throwable) {
        onFailure(t)
        throw t
    }
}


internal fun runGit(vararg args: String, repoDir: Path): String {
    val out = runProcess(
        cmd = listOf("git") + args.toList(),
        workingDir = repoDir.toFile(),
    )
    if (out.exitCode != 0) error("git ${args.joinToString(" ")} failed:\n${out.output}")
    else return out.output
}


// region swift package dump DTOs

@Serializable
data class SwiftPackageDump(
    val name: String,
    val targets: List<SwiftPackageDumpTarget> = emptyList(),
)


@Serializable
data class SwiftPackageDumpTarget(
    val name: String,
    val type: String,
    val dependencies: List<SwiftPackageDumpTargetDependency> = emptyList(),
    val settings: List<SwiftPackageDumpTargetSetting> = emptyList(),
)

@Serializable
data class SwiftPackageDumpTargetDependency(
    // product is a heterogeneous array: [productName: String, packageName: String, moduleAliases: Any?, condition: Any?]
    val product: List<kotlinx.serialization.json.JsonElement>? = null,
)

@Serializable
data class SwiftPackageDumpTargetSetting(
    val tool: String,
    val kind: SwiftPackageDumpTargetSettingKind,
)

@Serializable
data class SwiftPackageDumpTargetSettingKind(
    val unsafeFlags: SwiftPackageDumpUnsafeFlags? = null,
)

@Serializable
data class SwiftPackageDumpUnsafeFlags(
    @SerialName("_0") val flags: List<String> = emptyList(),
)

// endregion

fun dumpSwiftPackage(packagePath: Path): SwiftPackageDump {
    return runAppleToolCommand(packagePath, listOf("swift", "package", "dump-package"))
}

// region xcodebuild PIF dump DTOs

@Serializable
data class XcodebuildPIFEntry(
    val signature: String,
    val type: String,
    val contents: XcodebuildPIFContents,
)

@Serializable
data class XcodebuildPIFContents(
    val guid: String,
    val name: String? = null,
    val path: String? = null,
    val projectDirectory: String? = null,
    val projectIsPackage: String? = null,
    val projectName: String? = null,
    val targets: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val dependencies: List<XcodebuildPIFDependency> = emptyList(),
    val buildConfigurations: List<XcodebuildPIFBuildConfiguration> = emptyList(),
    val buildPhases: List<XcodebuildPIFBuildPhase> = emptyList(),
    val productReference: XcodebuildPIFProductReference? = null,
    val productTypeIdentifier: String? = null,
    val type: String? = null,
    val frameworksBuildPhase: XcodebuildPIFFrameworksBuildPhase? = null,
)

@Serializable
data class XcodebuildPIFDependency(
    val guid: String,
    val name: String? = null,
    val platformFilters: List<String> = emptyList(),
)

@Serializable
data class XcodebuildPIFBuildConfiguration(
    val guid: String,
    val name: String,
    val buildSettings: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

@Serializable
data class XcodebuildPIFBuildPhase(
    val guid: String,
    val type: String,
    val buildFiles: List<XcodebuildPIFBuildFile> = emptyList(),
)

@Serializable
data class XcodebuildPIFBuildFile(
    val guid: String,
    val fileReference: String? = null,
    val targetReference: String? = null,
    val platformFilters: List<String> = emptyList(),
)

@Serializable
data class XcodebuildPIFProductReference(
    val guid: String,
    val name: String,
    val type: String,
)

@Serializable
data class XcodebuildPIFFrameworksBuildPhase(
    val guid: String,
    val type: String,
    val buildFiles: List<XcodebuildPIFBuildFile> = emptyList(),
)

// endregion

fun dumpXcodebuildPIF(appPath: Path): List<XcodebuildPIFEntry> {
    val outputFile = File.createTempFile("xcodebuild-pif", ".json")
    return runAppleToolCommand(appPath, listOf("xcodebuild", "-dumpPIF", outputFile.absolutePath), outputFile)
}

data class ApplicationRun(
    val stdout: File,
    val stderr: File,
    val exitCode: Int,
)

fun runApplication(projectPath: Path, appPath: Path): ApplicationRun {
    val stdout = projectPath.resolve("stdout").toFile()
    val stderr = projectPath.resolve("stderr").toFile()

    val exitCode = XCTestHelpers().use {
        val simulator = it.createSimulator().apply {
            boot()
        }
        simulator.install(appPath.toFile())
        simulator.launch("org.example.project.emptyxcode", stdout, stderr)
    }

    return ApplicationRun(
        stdout = stdout,
        stderr = stderr,
        exitCode = exitCode,
    )
}

fun TestProject.assertApplicationRunsAndObjCRuntimeDoesntEmitInStderr(
    appPath: Path,
) {
    val result = runApplication(projectPath = projectPath, appPath = appPath)
    try {
        assertEquals(
            result.exitCode,
            0,
        )
        assertEquals(
            "",
            result.stderr.readLines().filter {
                /**
                 * Google Maps and some other libraries litter in stderr with logs we don't care about. We only need the objc message about
                 * class duplication.
                 * @see `org.jetbrains.kotlin.gradle.apple.SwiftPMImportDynamicLinkageTests.dynamic linkage with SwiftPM import - duplicates static binary during application linkage`
                 */
                "is implemented in both" in it
            }.joinToString("\n"),
        )
    } catch (e: Throwable) {
        throw AssertionError(
            listOf(
                "exitCode: ${result.exitCode}",
                "stdout: ${result.stdout.readText()}",
                "stderr: ${result.stderr.readText()}",
            ).joinToString("\n"),
            e,
        )
    }
}

fun PublishedProject.assertSwiftPMMetadataVariantExistsInRootComponent() {
    val gradleMetadata = rootComponent.gradleMetadata.readText().let {
        swiftPmJson.decodeFromString<GradleMetadata>(it)
    }

    assertEquals(
        Variant(
            name = "swiftPMDependenciesMetadataElements",
            attributes = mapOf(
                "org.gradle.category" to "library",
                "org.gradle.usage" to "swiftPMDependenciesMetadata"
            ),
            availableAt = null,
            files = listOf(
                VariantFile(
                    name = "swiftPMDependenciesMetadata",
                    url = "${name}-${version}-swiftpm-metadata.json",
                )
            ),
        ).prettyPrinted,
        gradleMetadata.variants.single { it.name == "swiftPMDependenciesMetadataElements" }.prettyPrinted
    )
}

fun TestProject.commonizeAndDumpCinteropSignatures(
    commonizerBasePath: Path = projectPath,
    commonizeTask: String = "commonizeCInterop",
): String {
    build(commonizeTask)

    val commonizerResult = commonizerBasePath.resolve("build/classes/kotlin/commonizer/swiftPMImport")
        .listDirectoryEntries()
        .single { it.isDirectory() }
        .listDirectoryEntries()
        .single { it.isDirectory() }
        .listDirectoryEntries()
        .single { it.isDirectory() }

    return dumpKlibMetadataSignatures(commonizerResult.toFile())
}

private val CINTEROP_NOISE_SIGNATURE_LINES = setOf(
    "swiftPMImport.emptyxcode/SWIFT_TYPEDEFS.<get-SWIFT_TYPEDEFS>|<get-SWIFT_TYPEDEFS>(){}[0]",
    "swiftPMImport.emptyxcode/SWIFT_TYPEDEFS|{}SWIFT_TYPEDEFS[0]",
    "swiftPMImport.emptyxcode/char16_tVar|null[0]",
    "swiftPMImport.emptyxcode/char16_t|null[0]",
    "swiftPMImport.emptyxcode/char32_tVar|null[0]",
    "swiftPMImport.emptyxcode/char32_t|null[0]",
    "swiftPMImport.emptyxcode/char8_tVar|null[0]",
    "swiftPMImport.emptyxcode/char8_t|null[0]",
    "swiftPMImport.emptyxcode/swift_double2Var|null[0]",
    "swiftPMImport.emptyxcode/swift_double2|null[0]",
    "swiftPMImport.emptyxcode/swift_float3Var|null[0]",
    "swiftPMImport.emptyxcode/swift_float3|null[0]",
    "swiftPMImport.emptyxcode/swift_float4Var|null[0]",
    "swiftPMImport.emptyxcode/swift_float4|null[0]",
    "swiftPMImport.emptyxcode/swift_int3Var|null[0]",
    "swiftPMImport.emptyxcode/swift_int3|null[0]",
    "swiftPMImport.emptyxcode/swift_int4Var|null[0]",
    "swiftPMImport.emptyxcode/swift_int4|null[0]",
    "swiftPMImport.emptyxcode/swift_uint3Var|null[0]",
    "swiftPMImport.emptyxcode/swift_uint3|null[0]",
    "swiftPMImport.emptyxcode/swift_uint4Var|null[0]",
    "swiftPMImport.emptyxcode/swift_uint4|null[0]",
)

fun String.filterOutNoiseSignatures() =
    lines().filter { it !in CINTEROP_NOISE_SIGNATURE_LINES }.joinToString("\n").trim()
