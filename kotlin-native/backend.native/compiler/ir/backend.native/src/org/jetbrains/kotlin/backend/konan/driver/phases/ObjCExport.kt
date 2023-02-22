/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.createObjCFramework
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

internal data class ProduceObjCExportInterfaceInput(
        val globalConfig: ObjCExportGlobalConfig,
        val headerInfos: List<ObjCExportHeaderInfo>,
)

/**
 * Create internal representation of Objective-C wrapper.
 */
internal val ProduceObjCExportInterfacePhase = createSimpleNamedCompilerPhase<PhaseContext, ProduceObjCExportInterfaceInput, ObjCExportedInterface>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    require(input.headerInfos.size == 1)
    val stdlibNamer = ObjCExportStdlibNamer.create(input.globalConfig.stdlibPrefix)
    produceObjCExportInterface(context, input.globalConfig, input.headerInfos.first(), stdlibNamer)
}

internal val ProduceObjCExportMultipleInterfacesPhase = createSimpleNamedCompilerPhase<PhaseContext, ProduceObjCExportInterfaceInput, List<ObjCExportedInterface>>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    val stdlibNamer = ObjCExportStdlibNamer.create(input.globalConfig.stdlibPrefix)
    val objcInterfaces = input.headerInfos.map { headerInfo ->
        produceObjCExportInterface(context, input.globalConfig, headerInfo, stdlibNamer)
    }
    objcInterfaces
}

internal data class CreateObjCFrameworkInput(
        val moduleDescriptor: ModuleDescriptor,
        val exportedInterface: ObjCExportedInterface,
        val frameworkDirectory: File,
)

/**
 * Create Objective-C framework in the given directory without binary.
 */
internal val CreateObjCFrameworkPhase = createSimpleNamedCompilerPhase<PhaseContext, CreateObjCFrameworkInput>(
        "CreateObjCFramework",
        "Create Objective-C framework"
) { context, input ->
    val config = context.config
    createObjCFramework(config, input.moduleDescriptor, input.exportedInterface, input.frameworkDirectory)
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