/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCTypeAdapter.Companion.ObjCTypeAdapterForBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfoOverrideError
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.bindObjCExportTypeAdapterTo
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.kotlinFqName

internal fun CodeGenerator.processBindClassToObjCNameAnnotations(file: IrFile) {
    file.allBindClassToObjCName.forEach {
        val adapter = ObjCTypeAdapterForBindClassToObjCName(it.kotlinClass, it.objCName)
        val typeAdapter = staticData.placeGlobal("", adapter).pointer
        try {
            bindObjCExportTypeAdapterTo(it.kotlinClass, typeAdapter)
        } catch (e: WritableTypeInfoOverrideError) {
            val reason = when (e.reason) {
                WritableTypeInfoOverrideError.Reason.NON_OVERRIDABLE -> "class cannot have ObjC class attachments"
                WritableTypeInfoOverrideError.Reason.ALREADY_OVERRIDDEN -> "another ObjC class is already bound"
            }
            context.report(
                    CompilerMessageSeverity.ERROR,
                    element = it.annotationElement,
                    irFile = file,
                    message = "Cannot bind ObjC class `${it.objCName}` to ${it.kotlinClass.kotlinFqName}: $reason",
            )
        }
    }
}