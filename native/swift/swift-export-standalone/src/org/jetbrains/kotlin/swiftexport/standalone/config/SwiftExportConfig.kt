/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.config

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.swiftexport.standalone.ErrorTypeStrategy
import org.jetbrains.kotlin.swiftexport.standalone.InputModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportLogger
import org.jetbrains.kotlin.swiftexport.standalone.createDummyLogger
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

public data class SwiftExportConfig(
    val outputPath: Path,
    val stableDeclarationsOrder: Boolean = false,
    val renderDocComments: Boolean = false,
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath),
    val konanTarget: KonanTarget,
    val errorTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.SpecialType,
    val logger: SwiftExportLogger = createDummyLogger(),
) {
    val moduleForPackagesName: String = "ExportedKotlinPackages"
    val runtimeSupportModuleName: String = "KotlinRuntimeSupport"
    val runtimeModuleName: String = "KotlinRuntime"

    val stdlibInputModule: InputModule by lazy { createInputModuleForStdlib(distribution) }
    val platformLibsInputModule: Set<InputModule> by lazy { createInputModuleForPlatformLibs(Path(distribution.platformLibs(konanTarget)).toFile()) }

    val targetPlatform: TargetPlatform by lazy { NativePlatforms.nativePlatformBySingleTarget(konanTarget) }

    private fun createInputModuleForStdlib(distribution: Distribution) =
        InputModule("stdlib", Path(distribution.stdlib), SwiftModuleConfig())

    private fun createInputModuleForPlatformLibs(platformLibsRootFile: File) = platformLibsRootFile.list()!!
        .map {
            InputModule(
                name = it.split(".").last(),
                platformLibsRootFile.resolve(it).toPath(),
                SwiftModuleConfig()
            )
        }.toSet()
}
