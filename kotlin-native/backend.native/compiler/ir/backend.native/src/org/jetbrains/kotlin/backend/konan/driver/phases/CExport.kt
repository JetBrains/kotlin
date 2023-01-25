/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterApiExporter
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterGenerator
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterTypeTranslator

internal val BuildCExports = createSimpleNamedCompilerPhase<PsiToIrContext, FrontendPhaseOutput.Full, CAdapterExportedElements>(
        "BuildCExports", "Build C exports",
        outputIfNotEnabled = { _, _, _, _ -> error("") }
) { context, input ->
    val prefix = context.config.fullExportedNamePrefix.replace("-|\\.".toRegex(), "_")
    val typeTranslator = CAdapterTypeTranslator(prefix, context.builtIns)
    CAdapterGenerator(context, typeTranslator).buildExports(input.moduleDescriptor)
}

internal val CExportGenerateApiPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CExportGenerateApi",
        description = "Create C header for the exported API",
) { context, _ ->
    CAdapterApiExporter(context, context.context.cAdapterExportedElements!!).makeGlobalStruct()
}