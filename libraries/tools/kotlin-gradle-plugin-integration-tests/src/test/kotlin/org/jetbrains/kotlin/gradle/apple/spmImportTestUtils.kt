/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertContains

@Suppress("INVISIBLE_REFERENCE")
const val SYNTHETIC_IMPORT_TARGET_MAGIC_NAME =
    GenerateSyntheticLinkageImportProject.Companion.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME

fun createLocalSwiftPackage(
    localPackageDir: Path,
    packageName: String = "LocalSwiftPackage",
    productName: String = packageName,
    targetName: String = productName,
) {
    localPackageDir.resolve("Sources/$targetName").createDirectories()
    createPackageManifestFile(localPackageDir, packageName, productName, targetName)

    createSwiftFile(localPackageDir, targetName, packageName)
}

private fun createSwiftFile(packageDir: Path, targetName: String, packageName: String) {
    packageDir.resolve("Sources/$targetName/$targetName.swift").writeText(
        """
                import Foundation

                @objc public class LocalHelper: NSObject {
                    @objc public static func greeting() -> String {
                        return "Hello from $packageName"
                    }
                }
            """.trimIndent()
    )
}

private fun createPackageManifestFile(
    localPackageDir: Path,
    packageName: String,
    productName: String,
    targetName: String,
) {
    localPackageDir.resolve("Package.swift").writeText(
        """
                // swift-tools-version: 5.9
                import PackageDescription

                let package = Package(
                    name: "$packageName",
                    platforms: [.iOS(.v15)],
                    products: [
                        .library(name: "$productName", targets: ["$targetName"]),
                    ],
                    targets: [
                        .target(name: "$targetName"),
                    ]
                )
            """.trimIndent()
    )
}


// Small test utility to reduce duplication in assertions across tests
internal fun assertPackageResolvedContains(
    resolvedText: String,
    packageName: String?,
    repoUrl: String,
    version: String
) {
    packageName?.let { assertContains(resolvedText, it) }
    assertContains(resolvedText, "\"location\" : \"$repoUrl\"")
    assertContains(resolvedText, "\"version\" : \"$version\"")
}

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

    println("Creating SwiftPM repo at $repoDir")

    runGit("init", repoDir = repoDir)
    runGit("config", "user.email", "spm@test",  repoDir = repoDir)
    runGit("config", "user.name", "spm-test", repoDir = repoDir)
    val commandResult = runProcess(
        listOf("touch", "git-daemon-export-ok"),
        workingDir = repoDir.toFile(),
    )

    if(!commandResult.isSuccessful) throw IllegalStateException("Failed to create git-daemon-export-ok file: ${commandResult.output}")



    createPackageManifestFile(repoDir, packageName, packageName, packageName)

    // Seed sources dir
    repoDir.resolve("Sources/$packageName").createDirectories()
    repoDir.resolve("Sources/$packageName/$packageName.swift").writeText(
        "public struct $packageName { public static let v = \"seed\" }\n"
    )

    runGit("add", ".",  repoDir = repoDir)
    runGit("commit", "-m", "init",  repoDir = repoDir)

    tags.forEach { tag ->
        // If caller provided per-tag content, apply it; otherwise just update a value.
        val files = fileByTag[tag]
            ?: mapOf("Sources/$packageName/$packageName.swift" to "public struct $packageName { public static let v = \"$tag\" }\n")

        files.forEach { (rel, content) ->
            val f = repoDir.resolve(rel)
            f.parent.createDirectories()
            f.writeText(content)
        }

        runGit("add", ".",  repoDir = repoDir)
        runGit("commit", "-m", "release $tag",  repoDir = repoDir)
        runGit("tag", tag,  repoDir = repoDir)
    }

    return repoDir
}

/**
 * Adds a new tag to an existing SwiftPM repo (commit + tag).
 */
internal fun addSwiftPmGitTag(
    repoDir: Path,
    packageName: String,
    tag: String,
    files: Map<String, String>? = null,
) {
    files?.forEach { (rel, content) ->
        val f = repoDir.resolve(rel)
        f.parent.createDirectories()
        f.writeText(content)
    }

    if (!repoDir.resolve("Package.swift").exists()) {
        repoDir.resolve("Package.swift").writeText(
            """
            // swift-tools-version: 5.9
            import PackageDescription

            let package = Package(
                name: "$packageName",
                products: [
                    .library(name: "$packageName", targets: ["$packageName"])
                ],
                targets: [
                    .target(name: "$packageName")
                ]
            )
            """.trimIndent() + "\n"
        )
        repoDir.resolve("Sources/$packageName").createDirectories()
    }

    runGit("add", ".",  repoDir = repoDir)
    runGit("commit", "-m", "release $tag",  repoDir = repoDir)
    runGit("tag", tag,  repoDir = repoDir)
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
    return runAppleToolCommand(packagePath, listOf("swift", "package", "describe", "--type", "json"))
}

/**
 * Local git-daemon helper for tests:
 * - serves repos under [baseDir]
 * - repositories are accessible as git://127.0.0.1:9418/<repoName>
 */

internal class GitDaemon(
    private val baseDir: Path,
    private val host: String = "127.0.0.1",
    private var port: Int = 0,
) : Closeable {

    private var process: Process? = null

    fun start(): GitDaemon {
        if (port == 0) {
            port = findFreePort()
        }

        val pb = ProcessBuilder(
            "git",
            "daemon",
            "--reuseaddr",
            "--export-all",
            "--base-path=${baseDir.toAbsolutePath()}",
            "--listen=$host",
            "--port=$port",
            baseDir.toAbsolutePath().toString(),
        ).redirectErrorStream(true)

        process = pb.start()

        return this
    }

    fun urlFor(repoName: String): String = "git://$host:$port/$repoName"

    override fun close() {
        process?.destroy()
        process?.waitFor()
        process = null
    }

    private fun findFreePort(): Int =
        java.net.ServerSocket(0).use { it.localPort }
}

internal fun runGit(vararg args: String, repoDir: Path) : String {
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
