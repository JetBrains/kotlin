/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.GenerateSyntheticLinkageImportProject
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

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

    localPackageDir.resolve("Sources/$targetName/$targetName.swift").writeText(
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
