/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

// To access nested maps safely
@Suppress("UNCHECKED_CAST")
internal fun <T> Map<String, Any>.getNestedValue(key: String): T? {
    return this[key] as? T
}

internal fun parseJsonToMap(jsonFile: Path): Map<String, Any> {
    val jsonText = jsonFile.readText()
    val typeToken = object : TypeToken<Map<String, Any>>() {}
    return Gson().fromJson(jsonText, typeToken.type)
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
): ProcessRunResult {
    val sdkPathResult = runProcess(
        listOf("xcrun", "--sdk", sdk, "--show-sdk-path"),
        workingDir
    )
    require(sdkPathResult.isSuccessful) { "Failed to get SDK path: ${sdkPathResult.output}" }
    val sdkPath = sdkPathResult.output.trim()

    val outputDir = workingDir.resolve("symbolgraph-output")
    outputDir.mkdirs()

    val command = mutableListOf(
        "xcrun", "swift", "symbolgraph-extract",
        "-module-name", moduleName,
        "-target", target,
        "-output-dir", outputDir.absolutePath,
        "-sdk", sdkPath
    )
    searchPaths.forEach { path ->
        command.add("-I")
        command.add(path.absolutePath)
    }

    return runProcess(command, workingDir)
}

/**
 * Parses symbol graph JSON and extracts symbol names.
 */
private fun parseSymbolGraphNames(symbolGraphFile: File): Set<String> {
    val json = symbolGraphFile.readText()
    val typeToken = object : TypeToken<Map<String, Any>>() {}
    val graph = Gson().fromJson<Map<String, Any>>(json, typeToken.type)

    @Suppress("UNCHECKED_CAST")
    val symbols = graph["symbols"] as? List<Map<String, Any>> ?: return emptySet()

    return symbols.mapNotNull { symbol ->
        @Suppress("UNCHECKED_CAST")
        val names = symbol["names"] as? Map<String, Any>
        names?.get("title") as? String
    }.toSet()
}

/**
 * Asserts that a Swift module contains expected symbols and does not contain unexpected ones.
 */
internal fun assertSwiftModuleSymbols(
    workingDir: File,
    moduleName: String,
    target: String,
    sdk: String = "iphoneos",
    searchPaths: List<File>,
    expectedSymbols: Set<String> = emptySet(),
    unexpectedSymbols: Set<String> = emptySet(),
) {
    val result = swiftSymbolgraphExtract(workingDir, moduleName, target, sdk, searchPaths)
    assert(result.isSuccessful) {
        "symbolgraph-extract failed for module $moduleName: ${result.output}"
    }

    val outputDir = workingDir.resolve("symbolgraph-output")

    // Try to find all symbol graph files (including extension files like Module@Extension.symbols.json)
    val allSymbolGraphFiles = outputDir.listFiles()?.filter { it.name.endsWith(".symbols.json") } ?: emptyList()

    assert(allSymbolGraphFiles.isNotEmpty()) {
        "No symbol graph files found in: ${outputDir.absolutePath}. Search paths: $searchPaths"
    }

    // Collect symbols from all symbol graph files
    val actualSymbols = mutableSetOf<String>()
    allSymbolGraphFiles.forEach { file ->
        actualSymbols.addAll(parseSymbolGraphNames(file))
    }

    expectedSymbols.forEach { expected ->
        assert(expected in actualSymbols) {
            val allFileContents = allSymbolGraphFiles.joinToString("\n---\n") { file ->
                "${file.name}: ${file.readText().take(1000)}"
            }
            "Expected symbol '$expected' not found in module $moduleName.\n" +
                    "Found symbol names: $actualSymbols\n" +
                    "Symbol graph files found: ${allSymbolGraphFiles.map { it.name }}\n" +
                    "File contents (truncated): $allFileContents"
        }
    }

    unexpectedSymbols.forEach { unexpected ->
        assert(unexpected !in actualSymbols) {
            "Unexpected symbol '$unexpected' found in module $moduleName"
        }
    }
}