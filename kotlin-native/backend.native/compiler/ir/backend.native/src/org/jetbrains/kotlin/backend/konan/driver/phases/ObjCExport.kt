/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.LinkKlibsContext
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.createObjCFramework
import org.jetbrains.kotlin.backend.konan.objcexport.dumpSelectorToSignatureMapping
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable

/**
 * Create internal representation of Objective-C wrapper.
 */
internal val ProduceObjCExportInterfacePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, FrontendPhaseOutput.Full, ObjCExportedInterface>(
        "ObjCExportInterface",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    produceObjCExportInterface(context, input.moduleDescriptor, input.frontendServices).also {
        if (context.config.omitFrameworkBinary) {
            // Dump selector -> signature mapping before IR linking if omitFrameworkBinary is true.
            context.config.dumpObjcSelectorToSignatureMapping?.let { path ->
                // Use a temporary SymbolTable here to generate the signatures because the main SymbolTable
                // is not available at this point (it's initialized in the LinkKlibs phase).
                val isolatedSymbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
                val objCCodeSpec = it.createCodeSpec(isolatedSymbolTable)
                objCCodeSpec.dumpSelectorToSignatureMapping(path, isolatedSymbolTable.signaturer!!, it.mapper)
            }
        }
    }
}

internal data class CreateObjCFrameworkInput(
    val moduleDescriptor: ModuleDescriptor,
    val exportedInterface: ObjCExportedInterface,
)

/**
 * Create Objective-C framework in the given directory without binary.
 */
internal val CreateObjCFrameworkPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, CreateObjCFrameworkInput>(
        "CreateObjCFramework",
) { context, input ->
    val config = context.config
    // TODO: Share this instance between multiple contexts (including NativeGenerationState)?
    val outputFiles = OutputFiles(config.outputPath, config.target, config.produce)
    createObjCFramework(config, input.moduleDescriptor, input.exportedInterface, outputFiles.mainFile)
}

/**
 * Create specification for bridges between exported Objective-C interfaces and their Kotlin origins.
 */
internal val CreateObjCExportCodeSpecPhase = createSimpleNamedCompilerPhase<LinkKlibsContext, ObjCExportedInterface, ObjCExportCodeSpec>(
        "ObjCExportCodeCodeSpec",
        outputIfNotEnabled = { _, _, _, _, -> ObjCExportCodeSpec(emptyList(), emptyList()) }
) { context, input ->
    input.createCodeSpec(context.symbolTable!!).also {
        // Dump selector -> signature mapping using the SymbolTable from the linking phase.
        context.config.dumpObjcSelectorToSignatureMapping?.let { path ->
            check(!context.config.omitFrameworkBinary)
            it.dumpSelectorToSignatureMapping(path, context.symbolTable!!.signaturer!!, input.mapper)
        }
    }
}
