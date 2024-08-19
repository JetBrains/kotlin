/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.swiftexport

import llvm.LLVMLinkage
import org.jetbrains.kotlin.backend.konan.ir.annotations.ToRetainedSwift
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.symbolName
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionProto
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.setObjCExportTypeInfo
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal fun CodeGenerator.processToRetainedSwift(function: IrFunction) {
    if(!context.config.swiftExport)
        return

    ToRetainedSwift.findInFunction(function)?.let { annotation ->
        val llvmFunction = if (function.isExternal) {
            val proto = LlvmFunctionProto(function, function.symbolName, this, LLVMLinkage.LLVMExternalLinkage)
            llvm.externalFunction(proto)
        } else function.llvmFunction
        setObjCExportTypeInfo(annotation.targetClass, convertToRetained = llvmFunction.toConstPointer())
    }
}