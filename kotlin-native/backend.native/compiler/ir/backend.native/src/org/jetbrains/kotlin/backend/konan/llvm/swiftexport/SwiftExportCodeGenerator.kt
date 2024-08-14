/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.swiftexport

import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.ContextUtils
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import java.io.File

internal class SwiftExportCodeGenerator(
        override val generationState: NativeGenerationState,
        typeMappingsFiles: List<String>
) : ContextUtils {

    val typeMappings: SwiftExportTypeMappings

    init {
        try {
            typeMappings = parseSwiftExportTypeMappingsFromFiles(typeMappingsFiles.map { File(it) })
        } catch (e: Exception) {
            context.reportCompilationError("Failed to parse type mappings: ${e.message}")
        }
    }

    fun generate(codegen: CodeGenerator) {
        TODO()
    }
}