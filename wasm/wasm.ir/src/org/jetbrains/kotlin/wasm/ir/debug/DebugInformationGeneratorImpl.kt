/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.debug

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class DebugInformationGeneratorImpl private constructor(
    private val sourceMapGenerator: DebugInformationGenerator?,
    private val dwarfGenerator: DebugInformationGenerator?,
) : DebugInformationGenerator {

    companion object {
        fun createIfNeeded(
            sourceMapGenerator: DebugInformationGenerator?,
            dwarfGenerator: DebugInformationGenerator?
        ): DebugInformationGenerator? =
            if (sourceMapGenerator != null || dwarfGenerator != null)
                DebugInformationGeneratorImpl(sourceMapGenerator, dwarfGenerator)
            else null
    }

    override fun addSourceLocation(location: SourceLocationMapping) {
        sourceMapGenerator?.addSourceLocation(location)
        dwarfGenerator?.addSourceLocation(location)
    }

    override fun generateDebugInformation(): DebugInformation {
        val sourceMapDebugInformation = sourceMapGenerator?.generateDebugInformation() ?: emptyList()
        val dwarfDebugInformation = dwarfGenerator?.generateDebugInformation() ?: emptyList()

        return sourceMapDebugInformation + dwarfDebugInformation
    }

    override fun startFunction(location: SourceLocationMapping, name: String) {
        sourceMapGenerator?.startFunction(location, name)
        dwarfGenerator?.startFunction(location, name)
    }

    override fun endFunction(location: SourceLocationMapping) {
        sourceMapGenerator?.endFunction(location)
        dwarfGenerator?.endFunction(location)
    }
}