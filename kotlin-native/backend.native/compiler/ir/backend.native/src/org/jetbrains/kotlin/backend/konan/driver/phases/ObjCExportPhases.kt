/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File

internal val ProduceObjCInterfacePhase = object : SimpleNamedCompilerPhase<PhaseContext, FrontendPhaseResult.Full, ObjCExportedInterface>(
        "ObjCInterface", "Generate Objective-C interface"
) {
    override fun outputIfNotEnabled(context: PhaseContext, input: FrontendPhaseResult.Full): ObjCExportedInterface {
        error("")
    }

    override fun phaseBody(context: PhaseContext, input: FrontendPhaseResult.Full): ObjCExportedInterface {
        return produceObjCExportInterface(context, input)
    }
}

internal data class ObjCCodeSpecInput(val symbolTable: SymbolTable, val objCExportedInterface: ObjCExportedInterface)

internal val ProduceObjCCodeSpecPhase = object : SimpleNamedCompilerPhase<PhaseContext, ObjCCodeSpecInput, ObjCExportCodeSpec>(
        "ObjCCodeSpec", "Generate Objective-C codespec"
) {
    override fun outputIfNotEnabled(context: PhaseContext, input: ObjCCodeSpecInput): ObjCExportCodeSpec {
        return ObjCExportCodeSpec(emptyList(), emptyList())
    }

    override fun phaseBody(context: PhaseContext, input: ObjCCodeSpecInput): ObjCExportCodeSpec {
        return input.objCExportedInterface.createCodeSpec(input.symbolTable)
    }
}

internal data class WriteObjCFrameworkInput(
        val objCInterface: ObjCExportedInterface,
        val moduleDescriptor: ModuleDescriptor,
        val frameworkFile: File,
)

internal val WriteObjCFramework = object : SimpleNamedCompilerPhase<PhaseContext, WriteObjCFrameworkInput, Unit>(
        "WriteObjCFramework", "Write Objective-C framework interface"
) {
    override fun outputIfNotEnabled(context: PhaseContext, input: WriteObjCFrameworkInput) {

    }

    override fun phaseBody(context: PhaseContext, input: WriteObjCFrameworkInput) {
        val objCExportFrameworkWriter = ObjCExportFrameworkWriter(context, input.moduleDescriptor)
        objCExportFrameworkWriter.produceFrameworkSpecific(input.objCInterface.headerLines, input.frameworkFile)
    }
}