/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariables
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(EnvironmentalVariablesOverride::class)
internal fun GradleProject.swiftExportEmbedAndSignEnvVariables(
    testBuildDir: Path,
    archs: List<String> = listOf("arm64"),
    sdk: String = "iphoneos",
    iphoneOsDeploymentTarget: String = "14.1",
) = EnvironmentalVariables(
    "CONFIGURATION" to "Debug",
    "SDK_NAME" to sdk,
    "ARCHS" to archs.joinToString(" "),
    "ONLY_ACTIVE_ARCH" to "YES",
    "TARGET_BUILD_DIR" to testBuildDir.absolutePathString(),
    "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
    "PLATFORM_NAME" to sdk,
    "DEPLOYMENT_TARGET_SETTING_NAME" to "IPHONEOS_DEPLOYMENT_TARGET",
    "IPHONEOS_DEPLOYMENT_TARGET" to iphoneOsDeploymentTarget,
    "BUILT_PRODUCTS_DIR" to projectPath.resolve("build/builtProductsDir").absolutePathString(),
)

internal fun KGPBaseTest.publishMultiplatformLibrary(
    gradleVersion: GradleVersion,
    projectName: String = "multiplatformLibrary",
    configure: KotlinMultiplatformExtension.() -> Unit = {
        iosArm64()
        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
    },
): PublishedProject = project("empty", gradleVersion) {
    plugins {
        kotlin("multiplatform")
    }
    settingsBuildScriptInjection {
        settings.rootProject.name = projectName
    }
    buildScriptInjection {
        project.applyMultiplatform(configure)
    }
}.publish(publisherConfiguration = PublisherConfiguration())

internal fun swiftCompile(workingDir: File, libDir: File, source: File, target: String) = runProcess(
    listOf(
        "xcrun", "--sdk", "iphonesimulator", "swiftc", "${source.relativeTo(workingDir).path}",
        "-I", libDir.absolutePath, "-target", target,
        "-Xlinker", "-L", "-Xlinker", libDir.absolutePath, "-Xlinker", "-lShared",
        "-framework", "Foundation", "-framework", "UIKit"
    ),
    workingDir
)

internal val swiftConsumerSource
    get() = """
    import Shared

    #if arch(arm64)
    iosSimulatorArm64Bar()
    #elseif arch(x86_64)
    iosX64Bar()
    #else
    #error("Not supposed to happen")
    #endif
""".trimIndent()

internal fun JsonObject.getNestedList(key: String): List<JsonObject>? {
    return this[key]?.jsonArray?.map { it.jsonObject }
}

internal fun parseJsonToMap(jsonFile: Path): JsonObject {
    val jsonText = jsonFile.readText()
    return Json.parseToJsonElement(jsonText).jsonObject
}

/**
 * Extracts symbol graph from a Swift module using `swift symbolgraph-extract`.
 */
private fun swiftSymbolgraphExtract(
    workingDir: File,
    moduleName: String,
    target: String,
    sdk: String = "iphoneos",
    searchPaths: List<File> = emptyList(),
    outputDir: File = symbolgraphOutputDir(workingDir, moduleName),
): ProcessRunResult {
    val sdkPathResult = runProcess(
        listOf("xcrun", "--sdk", sdk, "--show-sdk-path"),
        workingDir
    )
    sdkPathResult.assertProcessRunResult { assertTrue(isSuccessful, "Failed to get SDK path") }
    val sdkPath = sdkPathResult.output.trim()

    if (outputDir.exists()) {
        outputDir.deleteRecursively()
    }
    outputDir.mkdirs()

    val command = mutableListOf(
        "xcrun", "swift", "symbolgraph-extract",
        "-module-name", moduleName,
        "-target", target,
        "-output-dir", outputDir.absolutePath,
        "-sdk", sdkPath,
        "-skip-synthesized-members"
    )
    searchPaths.forEach { path ->
        command.add("-I")
        command.add(path.absolutePath)
    }

    return runProcess(command, workingDir)
}

private fun symbolgraphOutputDir(workingDir: File, moduleName: String): File {
    return workingDir.resolve("symbolgraph-output").resolve(moduleName)
}

/**
 * Parses symbol graph JSON and extracts symbol identifiers.
 */
private object SymbolGraphParsing {
    @Serializable
    data class SymbolGraph(
        val symbols: List<Symbol> = emptyList(),
    )

    @Serializable
    data class Symbol(
        val identifier: SymbolIdentifier? = null,
        val names: SymbolNames? = null,
        val pathComponents: List<String>? = null,
    )

    @Serializable
    data class SymbolIdentifier(
        val precise: String,
        val interfaceLanguage: String? = null,
    )

    @Serializable
    data class SymbolNames(
        val title: String? = null,
    )

    val json = Json { ignoreUnknownKeys = true }
}

/**
 * Swift symbol for symbolgraph-extract assertions.
 *
 * [demangledId] stores the demangled USR (for example, "Shared.foo() -> ()").
 * [pathComponents] are for readable diagnostics and do not participate in equality.
 */
internal data class SwiftSymbol(
    val demangledId: String,
    val pathComponents: List<String>,
) {
    init {
        require(demangledId.isNotBlank()) { "demangledId must not be blank" }
    }

    val displayName: String
        get() = pathComponents.joinToString(".")

    override fun equals(other: Any?): Boolean =
        other is SwiftSymbol && other.demangledId == demangledId

    override fun hashCode(): Int = demangledId.hashCode()

    override fun toString(): String = demangledId
}

/**
 * Demangles a raw USR using `xcrun swift-demangle`.
 *
 * Compound USRs with `::SYNTHESIZED::` represent protocol extension members synthesized for
 * a concrete type. Each part is demangled separately and reassembled for readability.
 */
private fun demangleUsr(rawUsr: String, workingDir: File): String {
    if ("::SYNTHESIZED::" in rawUsr) {
        val parts = rawUsr.split("::SYNTHESIZED::", limit = 2)
        require(parts.size == 2) {
            "Malformed SYNTHESIZED USR (expected 2 parts, got ${parts.size}): $rawUsr"
        }
        val (left, right) = parts
        return "${demangleSingleUsr(left, workingDir)} [SYNTHESIZED for ${demangleSingleUsr(right, workingDir)}]"
    }
    return demangleSingleUsr(rawUsr, workingDir)
}

/**
 * Demangles a single USR by converting the `s:` prefix to `$s` (Swift mangled symbol format).
 * USRs without the `s:` prefix are passed through unchanged.
 */
private fun demangleSingleUsr(rawUsr: String, workingDir: File): String {
    val mangled = if (rawUsr.startsWith("s:")) rawUsr.replaceFirst("s:", "\$s") else rawUsr
    val result = runProcess(listOf("xcrun", "swift-demangle", "--compact", mangled), workingDir)
    val output = result.output.trim()
    assert(result.isSuccessful) {
        "swift-demangle failed for USR '$rawUsr' (mangled: '$mangled'): ${result.output}"
    }
    assert(output.isNotEmpty()) {
        "swift-demangle returned empty output for USR '$rawUsr' (mangled: '$mangled')"
    }
    return output
}

private fun extractModuleSymbols(
    workingDir: File,
    moduleName: String,
    target: String,
    sdk: String,
    searchPaths: List<File>,
): Set<SwiftSymbol> {
    val outputDir = symbolgraphOutputDir(workingDir, moduleName)
    val result = swiftSymbolgraphExtract(workingDir, moduleName, target, sdk, searchPaths, outputDir)
    assert(result.isSuccessful) {
        "symbolgraph-extract failed for module $moduleName: ${result.output}"
    }

    val allSymbolGraphFiles = outputDir.listFiles()?.filter { it.name.endsWith(".symbols.json") }.orEmpty()

    return allSymbolGraphFiles.flatMap { symbolGraphFile ->
        val graph = SymbolGraphParsing.json.decodeFromString<SymbolGraphParsing.SymbolGraph>(symbolGraphFile.readText())
        graph.symbols.mapNotNull { symbol ->
            val precise = symbol.identifier?.precise ?: return@mapNotNull null
            val pathComponents = symbol.pathComponents ?: return@mapNotNull null
            SwiftSymbol(
                demangledId = demangleUsr(precise, workingDir),
                pathComponents = pathComponents
            )
        }
    }.toSet()
}

/**
 * Asserts that a Swift module contains exactly the expected symbols.
 */
internal fun assertSwiftModuleSymbols(
    workingDir: File,
    moduleName: String,
    target: String,
    sdk: String = "iphoneos",
    searchPaths: List<File>,
    expectedSymbols: Set<SwiftSymbol>,
) {
    val actualSymbols = extractModuleSymbols(workingDir, moduleName, target, sdk, searchPaths)
    assertEquals(
        expectedSymbols,
        actualSymbols,
        "Symbol mismatch in module $moduleName"
    )
}

/**
 * Asserts that Swift modules contain exactly the expected symbols.
 *
 * This function discovers all `.swiftmodule` directories in the built products directory,
 * extracts their symbols using `swift symbolgraph-extract`, and verifies that:
 * 1. All expected modules are present
 * 2. No unexpected modules are present
 * 3. Each module contains exactly the expected symbols (no more, no less)
 *
 * @param workingDir The working directory where symbolgraph-extract output will be written
 * @param builtProductsDir The directory containing `.swiftmodule` directories
 * @param target The target triple (e.g., "arm64-apple-ios14.1")
 * @param sdk The SDK name (e.g., "iphoneos")
 * @param expectedSymbolsByModule Map of module name to expected symbols. Modules not in this map
 *        will cause the assertion to fail if discovered.
 */
internal fun assertAllSwiftModuleSymbols(
    workingDir: File,
    builtProductsDir: File,
    target: String,
    sdk: String = "iphoneos",
    expectedSymbolsByModule: Map<String, Set<SwiftSymbol>>,
) {
    val allDirEntries = builtProductsDir.listFiles() ?: emptyArray()
    val swiftModuleDirs = allDirEntries.filter { it.isDirectory && it.name.endsWith(".swiftmodule") }

    assert(swiftModuleDirs.isNotEmpty()) {
        "No .swiftmodule directories found in: ${builtProductsDir.absolutePath}\n" +
                "Directory contents: ${allDirEntries.map { it.name }}"
    }

    val actualSymbolsByModule = swiftModuleDirs.associate { moduleDir ->
        val moduleName = moduleDir.name.removeSuffix(".swiftmodule")
        moduleName to extractModuleSymbols(
            workingDir = workingDir,
            moduleName = moduleName,
            target = target,
            sdk = sdk,
            searchPaths = listOf(builtProductsDir)
        )
    }

    assertEquals(expectedSymbolsByModule, actualSymbolsByModule)
}
