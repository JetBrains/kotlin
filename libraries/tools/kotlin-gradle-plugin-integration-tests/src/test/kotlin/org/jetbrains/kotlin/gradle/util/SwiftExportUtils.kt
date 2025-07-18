/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariables
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

@OptIn(EnvironmentalVariablesOverride::class)
internal fun GradleProject.swiftExportEmbedAndSignEnvVariables(
    testBuildDir: Path,
    archs: List<String> = listOf("arm64"),
    sdk: String = "iphoneos",
) = EnvironmentalVariables(
    "CONFIGURATION" to "Debug",
    "SDK_NAME" to sdk,
    "ARCHS" to archs.joinToString(" "),
    "ONLY_ACTIVE_ARCH" to "YES",
    "TARGET_BUILD_DIR" to testBuildDir.absolutePathString(),
    "FRAMEWORKS_FOLDER_PATH" to "build/xcode-derived",
    "PLATFORM_NAME" to sdk,
    "BUILT_PRODUCTS_DIR" to projectPath.resolve("build/builtProductsDir").absolutePathString(),
)

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