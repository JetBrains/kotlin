/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface


internal val ProduceObjCExportInterfacePhase = createSimpleNamedCompilerPhase<PhaseContext, FrontendPhaseOutput.Full, ObjCExportedInterface>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("TODO") }
) { context, input ->
    produceObjCExportInterface(context, input.moduleDescriptor, input.frontendServices)
}

internal val CreateObjCExportCodeSpecPhase = createSimpleNamedCompilerPhase<PsiToIrContext, ObjCExportedInterface, ObjCExportCodeSpec>(
        "ObjCExportCodeCodeSpec",
        "Objective-C IR symbols",
        outputIfNotEnabled = { _, _, _, _, -> error("TODO") }
) { context, input ->
    input.createCodeSpec(context.symbolTable!!)
}