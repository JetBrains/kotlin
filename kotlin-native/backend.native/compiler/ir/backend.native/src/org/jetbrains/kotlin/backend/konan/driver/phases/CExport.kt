/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.cexport.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterApiExporter
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterGenerator
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterTypeTranslator
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import java.io.File

internal val BuildCExports = createSimpleNamedCompilerPhase<PsiToIrContext, FrontendPhaseOutput.Full, CAdapterExportedElements>(
        "BuildCExports", "Build C exports",
        outputIfNotEnabled = { _, _, _, _ -> error("") }
) { context, input ->
    val prefix = context.config.fullExportedNamePrefix.replace("-|\\.".toRegex(), "_")
    val typeTranslator = CAdapterTypeTranslator(prefix, context.builtIns)
    CAdapterGenerator(context, typeTranslator).buildExports(input.moduleDescriptor)
}

internal data class CExportApiPhaseInput(
        val elements: CAdapterExportedElements,
        val cAdapterHeader: File,
        val cAdapterDef: File? = null,
        val cAdapterCpp: File,
        val cAdapterBitcode: File
)

// TODO: Improve naming
internal val CExportApiPhase = createSimpleNamedCompilerPhase<PhaseContext, CExportApiPhaseInput, List<File>>(
        name = "CExportApi",
        description = "Create C API",
        outputIfNotEnabled = { _, _, _, _ -> emptyList() },
        op = { context, input ->
            CAdapterApiExporter(
                    context.config.target,
                    input.elements,
                    cAdapterHeader = input.cAdapterHeader,
                    cAdapterDef = input.cAdapterDef,
                    cAdapterCpp = input.cAdapterCpp,
            ).makeGlobalStruct()
            produceCAdapterBitcode(context.config.clang, input.cAdapterCpp.absolutePath, input.cAdapterBitcode.absolutePath)
            listOf(input.cAdapterBitcode)
        }
)