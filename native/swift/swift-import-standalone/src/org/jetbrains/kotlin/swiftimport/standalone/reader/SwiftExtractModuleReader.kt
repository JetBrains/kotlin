/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.reader

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftimport.standalone.SwiftInputModule
import org.jetbrains.kotlin.swiftimport.standalone.config.SwiftImportConfig
import org.jetbrains.kotlin.swiftimport.standalone.translation.toSirModule
import org.swift.swiftkit.core.SwiftArena
import org.swift.swiftkit.swiftextract.SwiftAnalyzer
import kotlin.io.path.readText

/**
 * Analyzes the sources of a [SwiftInputModule] with swiftextract.
 */
internal object SwiftExtractModuleReader : SwiftModuleReader {
    override fun read(module: SwiftInputModule, config: SwiftImportConfig): SirModule =
        SwiftArena.ofConfined().use { arena ->
            SwiftAnalyzer.analyzeSourcesForJExtract(
                module.name,
                config.publicOnly,
                module.sources.map { it.toString() }.toTypedArray(),
                module.sources.map { it.readText() }.toTypedArray(),
                arena,
            ).toSirModule(module.name, arena)
        }
}
