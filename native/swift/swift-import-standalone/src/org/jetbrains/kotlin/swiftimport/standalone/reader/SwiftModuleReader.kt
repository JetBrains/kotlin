/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.reader

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftimport.standalone.SwiftInputModule
import org.jetbrains.kotlin.swiftimport.standalone.config.SwiftImportConfig

/**
 * Reads a [SwiftInputModule] into SIR.
 */
internal fun interface SwiftModuleReader {
    fun read(module: SwiftInputModule, config: SwiftImportConfig): SirModule
}
