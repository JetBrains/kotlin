/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

internal val moduleToImports = mutableMapOf<LLVMModuleRef, LlvmImports>()
internal val moduleToSpecification = mutableMapOf<LLVMModuleRef, Spec>()
internal val moduleToStaticData = mutableMapOf<LLVMModuleRef, StaticData>()
internal val moduleToLlvm = mutableMapOf<LLVMModuleRef, Llvm>()
internal val moduleToDebugInfo = mutableMapOf<LLVMModuleRef, DebugInfo>()
internal val moduleToLlvmDeclarations = mutableMapOf<LLVMModuleRef, LlvmDeclarations>()

internal val irFileToModule = mutableMapOf<IrFile, LLVMModuleRef>()
internal val irFileToCodegenVisitor = mutableMapOf<IrFile, CodeGeneratorVisitor>()

internal fun programBitcode(): List<LLVMModuleRef> = irFileToModule.values.toList()


sealed class Spec {
    abstract fun containsDeclaration(declaration: IrDeclaration): Boolean
}

internal class RootSpec : Spec() {
    override fun containsDeclaration(declaration: IrDeclaration): Boolean {
        return false
    }
}

internal class FileLlvmModuleSpecification(
        val irFile: IrFile,
) : Spec() {
    override fun containsDeclaration(declaration: IrDeclaration): Boolean =
            declaration.file == irFile
}