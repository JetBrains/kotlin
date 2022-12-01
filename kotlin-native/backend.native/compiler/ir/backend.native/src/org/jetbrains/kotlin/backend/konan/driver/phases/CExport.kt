/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.CAdapterGenerator
import org.jetbrains.kotlin.backend.konan.CExportResults
import org.jetbrains.kotlin.backend.konan.CExportTypeTranslator

internal val BuildCExports = createSimpleNamedCompilerPhase<PsiToIrContext, FrontendPhaseOutput.Full, CExportResults>(
        "BuildCExports", "Build C exports",
        outputIfNotEnabled = { _, _, _, _ -> error("") }
) { context, input ->
    val prefix = context.config.fullExportedNamePrefix.replace("-|\\.".toRegex(), "_")
    val typeTranslator = CExportTypeTranslator(prefix, context.builtIns)
    CAdapterGenerator(context, input.moduleDescriptor, typeTranslator).buildExports(context.symbolTable!!)
}