/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.config

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.ErrorTypeStrategy
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportLogger
import org.jetbrains.kotlin.swiftexport.standalone.createDummyLogger
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Path

public data class SwiftExportConfig(
    val outputPath: Path,
    val stableDeclarationsOrder: Boolean = false,
    val renderDocComments: Boolean = false,
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath),
    val errorTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.SpecialType,
    val logger: SwiftExportLogger = createDummyLogger(),
) {
    val moduleForPackagesName: String = "ExportedKotlinPackages"
    val runtimeSupportModuleName: String = "KotlinRuntimeSupport"
    val runtimeModuleName: String = "KotlinRuntime"
}