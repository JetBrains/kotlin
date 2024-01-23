/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.runner

import org.jetbrains.kotlin.sir.runner.builders.buildFunctionBridges
import org.jetbrains.kotlin.sir.runner.builders.buildSwiftModule
import org.jetbrains.kotlin.sir.runner.transformation.transformToSwift
import org.jetbrains.kotlin.sir.runner.writer.dumpResultToFiles
import java.nio.file.Path

public data class SwiftExportConfig(
    val settings: Map<String, String>
)

public data class SwiftExportInput(
    val sourceRoot: Path, // todo: we do not support multi-modules currently. see KT-65220
    val libraries: List<Path> = emptyList(), // todo: not supported currently. see KT-65221
)

public data class SwiftExportOutput(
    val swiftApi: Path,
    val kotlinBridges: Path,
    val cHeaderBridges: Path,
)

/**
 * A root function for running Swift Export from build tool
 */
public fun runSwiftExport(
    input: SwiftExportInput,
    config: SwiftExportConfig = SwiftExportConfig(emptyMap()),
    output: SwiftExportOutput,
) {
    val module = buildSwiftModule(input)
        .transformToSwift()
    val bridgeRequests = module.buildFunctionBridges()
    module.dumpResultToFiles(bridgeRequests, output)
}
