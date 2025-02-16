/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import java.nio.file.Path

internal data class KlibExportSettings(
    val path: Path,
    val swiftModuleName: String,
    val rootPackage: String? = null,
)

internal fun KlibExportSettings.createConfig(outputPath: Path): SwiftExportConfig {
    return SwiftExportConfig(
        distribution = Distribution(KonanHome.konanHomePath),
        outputPath = outputPath
    )
}

internal fun KlibExportSettings.createInputModule(config: SwiftModuleConfig) =
    InputModule(swiftModuleName, path, config)