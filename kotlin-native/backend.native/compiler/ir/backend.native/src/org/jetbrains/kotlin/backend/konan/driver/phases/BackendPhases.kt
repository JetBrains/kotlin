/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jebrains.kotlin.backend.native.PhaseContext
import org.jebrains.kotlin.backend.native.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.FrontendPhaseOutput
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.BackendPhaseContext
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.makeEntryPoint
import org.jetbrains.kotlin.backend.konan.objcexport.createTestBundle
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope


internal val CopyDefaultValuesToActualPhase = createSimpleNamedCompilerPhase<PhaseContext, Pair<IrModuleFragment, IrBuiltIns>>(
        name = "CopyDefaultValuesToActual",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { _, input ->
    ExpectToActualDefaultValueCopier(input.first, input.second).process()
}

internal val EntryPointPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "addEntryPoint",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, module ->
    val parent = context.context
    val entryPoint = parent.symbols.entryPoint!!.owner
    val file: IrFile = if (context.llvmModuleSpecification.containsDeclaration(entryPoint)) {
        entryPoint.file
    } else {
        // `main` function is compiled to other LLVM module.
        // For example, test running support uses `main` defined in stdlib.
        module.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.internal.abi"))
    }

    file.addChild(makeEntryPoint(context))
}

internal val CreateTestBundlePhase = createSimpleNamedCompilerPhase<BackendPhaseContext, FrontendPhaseOutput.Full>(
        "CreateTestBundlePhase",
) { context, input ->
    val config = context.config
    val output = OutputFiles(config.outputPath, config.target, config.produce).mainFile
    createTestBundle(config, input.moduleDescriptor, output)
}

private fun IrModuleFragment.addFile(fileEntry: IrFileEntry, packageFqName: FqName): IrFile {
    val packageFragmentDescriptor = object : PackageFragmentDescriptorImpl(this.descriptor, packageFqName) {
        override fun getMemberScope(): MemberScope = MemberScope.Empty
    }

    return IrFileImpl(fileEntry, packageFragmentDescriptor).also(this::addFile)
}
