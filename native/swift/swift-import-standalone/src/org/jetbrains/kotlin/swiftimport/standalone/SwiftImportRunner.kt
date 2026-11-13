/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone

import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.swiftimport.standalone.config.SwiftImportConfig
import org.jetbrains.kotlin.swiftimport.standalone.reader.SwiftExtractModuleReader
import org.jetbrains.kotlin.swiftimport.standalone.reader.SwiftModuleReader
import org.jetbrains.kotlin.swiftimport.standalone.translation.toKirModule

/**
 * Translates a single Swift module to KIR.
 * Only top-level functions are supported.
 *
 * @param module The Swift module to import.
 * @param config The configuration object specifying the behavior of Swift import.
 * @return A [Result] containing the translated [KirModule] upon success, or an exception in case of a failure.
 */
public fun runSwiftImport(
    module: SwiftInputModule,
    config: SwiftImportConfig = SwiftImportConfig(),
): Result<KirModule> =
    runSwiftImport(module, config, SwiftExtractModuleReader)

internal fun runSwiftImport(
    module: SwiftInputModule,
    config: SwiftImportConfig,
    reader: SwiftModuleReader,
): Result<KirModule> = runCatching {
    val sirModule = reader.read(module, config)
    sirModule.toKirModule()
}
