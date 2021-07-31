/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMModuleRef
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.file

internal val moduleToImports = mutableMapOf<LLVMModuleRef, LlvmImports>()
internal val moduleToSpecification = mutableMapOf<LLVMModuleRef, FileLlvmModuleSpecification>()
internal val moduleToStaticData = mutableMapOf<LLVMModuleRef, StaticData>()
internal val moduleToLlvm = mutableMapOf<LLVMModuleRef, Llvm>()
internal val moduleToDebugInfo = mutableMapOf<LLVMModuleRef, DebugInfo>()
internal val moduleToLlvmDeclarations = mutableMapOf<LLVMModuleRef, LlvmDeclarations>()

internal val irFileToModule = mutableMapOf<IrFile, LLVMModuleRef>()
internal val irFileToCodegenVisitor = mutableMapOf<IrFile, CodeGeneratorVisitor>()

internal class FileLlvmModuleSpecification(
        private val irFile: IrFile,
) {
    fun containsDeclaration(declaration: IrDeclaration): Boolean =
            declaration.file == irFile
}