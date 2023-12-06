/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.createObjCFramework
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

/**
 * Create internal representation of Objective-C wrapper.
 */
internal val ProduceObjCExportInterfacePhase = createSimpleNamedCompilerPhase<PhaseContext, FrontendPhaseOutput.Full, ObjCExportedInterface>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    produceObjCExportInterface(context, input.moduleDescriptor, input.frontendServices)
}

internal data class CreateObjCFrameworkInput(
    val moduleDescriptor: ModuleDescriptor,
    val exportedInterface: ObjCExportedInterface,
)

/**
 * Create Objective-C framework in the given directory without binary.
 */
internal val CreateObjCFrameworkPhase = createSimpleNamedCompilerPhase<PhaseContext, CreateObjCFrameworkInput>(
        "CreateObjCFramework",
        "Create Objective-C framework"
) { context, input ->
    val config = context.config
    // TODO: Share this instance between multiple contexts (including NativeGenerationState)?
    val outputFiles = OutputFiles(config.outputPath, config.target, config.produce)
    createObjCFramework(config, input.moduleDescriptor, input.exportedInterface, outputFiles.mainFile)
}

/**
 * Create specification for bridges between exported Objective-C interfaces and their Kotlin origins.
 */
internal val CreateObjCExportCodeSpecPhase = createSimpleNamedCompilerPhase<PsiToIrContext, ObjCExportedInterface, ObjCExportCodeSpec>(
        "ObjCExportCodeCodeSpec",
        "Objective-C IR symbols",
        outputIfNotEnabled = { _, _, _, _, -> ObjCExportCodeSpec(emptyList(), emptyList()) }
) { context, input ->
    input.createCodeSpec(context.symbolTable!!)
}