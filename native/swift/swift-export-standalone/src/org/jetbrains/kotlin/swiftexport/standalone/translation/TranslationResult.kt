/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.translation

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources

internal class TranslationResult(
    val sirModule: SirModule,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val config: SwiftExportConfig,
    val moduleConfig: SwiftModuleConfig,
    val bridgesModuleName: String,
)