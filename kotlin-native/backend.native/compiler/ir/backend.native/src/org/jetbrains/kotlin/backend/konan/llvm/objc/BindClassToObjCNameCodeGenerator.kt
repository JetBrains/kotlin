/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCTypeAdapter.Companion.ObjCTypeAdapterForBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfoOverrideError
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.bindObjCExportTypeAdapterTo
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName

internal fun CodeGenerator.processBindClassToObjCNameAnnotations(file: IrFile) {
    val reverseBridgesByClass = collectReverseBridgeAdapters(file)

    file.allBindClassToObjCName.forEach {
        val vtableSize = if (it.kotlinClass.isInterface)
            -1
        else
            generationState.context.getLayoutBuilder(it.kotlinClass).vtableEntries.size
        val reverseAdapters = reverseBridgesByClass[it.kotlinClass] ?: emptyList()
        val adapter = ObjCTypeAdapterForBindClassToObjCName(it.kotlinClass, it.objCName, vtableSize, reverseAdapters)
        val typeAdapter = staticData.placeGlobal("", adapter).pointer
        try {
            bindObjCExportTypeAdapterTo(it.kotlinClass, typeAdapter)
        } catch (e: WritableTypeInfoOverrideError) {
            val reason = when (e.reason) {
                WritableTypeInfoOverrideError.Reason.NON_OVERRIDABLE -> "class cannot have ObjC class attachments"
                WritableTypeInfoOverrideError.Reason.ALREADY_OVERRIDDEN -> "another ObjC class is already bound"
            }
            context.diagnosticReporter.report(
                    NativeBackendDiagnostics.NATIVE_BACKEND_ERROR,
                    "Cannot bind ObjC class `${it.objCName}` to ${it.kotlinClass.kotlinFqName}: $reason",
                    it.annotationElement.getCompilerMessageLocation(file)
            )
        }
    }
}