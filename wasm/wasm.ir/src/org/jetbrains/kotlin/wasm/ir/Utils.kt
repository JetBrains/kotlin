/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

/**
 * Calculate declaration IDs of linked wasm module
 */
fun WasmModule.calculateIds() {
    fun List<WasmNamedModuleField>.calculateIds(startIndex: Int = 0) {
        for ((index, field) in this.withIndex()) {
            field.id = index + startIndex
        }
    }

    functionTypes.calculateIds()
    structs.calculateIds(startIndex = functionTypes.size)
    importedFunctions.calculateIds()
    importedMemories.calculateIds()
    importedTables.calculateIds()
    importedGlobals.calculateIds()
    elements.calculateIds()

    definedFunctions.calculateIds(startIndex = importedFunctions.size)
    globals.calculateIds(startIndex = importedGlobals.size)
    memories.calculateIds(startIndex = importedMemories.size)
    tables.calculateIds(startIndex = importedTables.size)
}
