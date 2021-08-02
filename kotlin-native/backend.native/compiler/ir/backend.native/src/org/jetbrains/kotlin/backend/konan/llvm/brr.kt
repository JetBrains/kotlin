/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKStringFromUtf8
import llvm.LLVMGetModuleIdentifier
import llvm.LLVMModuleRef
import llvm.size_tVar
import org.jetbrains.kotlin.backend.konan.Context
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

// TODO: Class, that accepts rule.
sealed class Spec {
    abstract fun containsDeclaration(declaration: IrDeclaration): Boolean
}

// TODO: Rule: NOT in IrFiles
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

fun stableModuleName(llvmModule: LLVMModuleRef): String = memScoped {
    val sizeVar = alloc<size_tVar>()
    LLVMGetModuleIdentifier(llvmModule, sizeVar.ptr)?.toKStringFromUtf8()!!
}

internal class SeparateCompilation(val context: Context) {
    fun shouldRecompile(llvmModule: LLVMModuleRef): Boolean {
        val moduleName = stableModuleName(llvmModule)
        val cachedModule = context.config.tempFiles.lookup("$moduleName.bc")
                ?: return false
        return true
    }
}