/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.LinkKlibsContext
import org.jetbrains.kotlin.backend.konan.LinkKlibsOutput
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.cexport.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterApiExporter
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterGenerator
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterIrGenerator
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterTypeTranslator
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import java.io.File

internal val BuildCExports = createSimpleNamedCompilerPhase<LinkKlibsContext, FrontendPhaseOutput.Full, CAdapterExportedElements>(
        "BuildCExports",
        outputIfNotEnabled = { _, _, _, _ -> error("") }
) { context, input ->
    val prefix = context.config.cExportPrefix

    @OptIn(K1Deprecation::class)
    val typeTranslator = CAdapterTypeTranslator(prefix, context.builtIns)
    CAdapterGenerator(context, typeTranslator).buildExports(input.moduleDescriptor)
}

/**
 * IR variant of [BuildCExports]: builds the C export model (phase 1) from the linked IR of the exported modules,
 * without touching K1 descriptors. Runs post-linkage (unlike [BuildCExports]), where the IR symbols are bound.
 */
internal fun buildCExportsFromIr(
        config: NativeSecondStageCompilationConfig,
        linkKlibsOutput: LinkKlibsOutput,
): CAdapterExportedElements {
    val prefix = config.cExportPrefix
    val exportedFragments = (config.loadedKlibs.included + config.loadedKlibs.exported)
            .mapNotNull { linkKlibsOutput.irModules[it.path] }
            .distinct()
    return CAdapterIrGenerator(prefix, linkKlibsOutput.irBuiltIns).buildExports(exportedFragments)
}

private val NativeSecondStageCompilationConfig.cExportPrefix: String
    get() = fullExportedNamePrefix.replace("-|\\.".toRegex(), "_")

internal data class CExportGenerateApiInput(
        val elements: CAdapterExportedElements,
        val headerFile: File,
        val defFile: File?,
        val cppAdapterFile: File,
)

internal val CExportGenerateApiPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, CExportGenerateApiInput>(
        name = "CExportGenerateApi",
) { context, input ->
    CAdapterApiExporter(
            elements = input.elements,
            headerFile = input.headerFile,
            defFile = input.defFile,
            cppAdapterFile = input.cppAdapterFile,
            target = context.config.target,
    ).makeGlobalStruct()
}

internal class CExportCompileAdapterInput(
        val cppAdapterFile: File,
        val bitcodeAdapterFile: File,
)

internal val CExportCompileAdapterPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, CExportCompileAdapterInput>(
        name = "CExportCompileAdapter",
) { context, input ->
    produceCAdapterBitcode(context.config.clang, input.cppAdapterFile, input.bitcodeAdapterFile)
}
