/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.common.report
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCTypeAdapter.Companion.ObjCTypeAdapterForBindClassToObjCName
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCTypeAdapter.Companion.ObjCTypeAdapterForBindClassToObjCNameWithReverseBridges
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfoOverrideError
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.bindObjCExportTypeAdapterTo
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName

internal fun CodeGenerator.processBindClassToObjCNameAnnotations(file: IrFile) {
    val reverseBridgesByClass = collectReverseBridgeAdapters(file)

    file.allBindClassToObjCName.forEach {
        val layoutBuilder = generationState.context.getLayoutBuilder(it.kotlinClass)
        val isInterface = it.kotlinClass.isInterface
        val vtableSize = if (isInterface) -1 else layoutBuilder.vtableEntries.size
        // For interfaces, pass the interface's own vtable size as the itable size — the runtime
        // uses this when adding the interface's itable slot for Swift subclasses (see
        // ObjCExport.mm `addITable(typeInfo->classId_, typeAdapter->kotlinItableSize)`). Mismatch
        // against the reverse adapter's itableSize triggers a RuntimeAssert, so these must agree.
        val itableSize = if (isInterface) layoutBuilder.interfaceVTableEntries.size else 0
        val reverseAdapters = reverseBridgesByClass[it.kotlinClass] ?: emptyList()
        val adapter = if (reverseAdapters.isEmpty()) {
            ObjCTypeAdapterForBindClassToObjCName(it.kotlinClass, it.objCName, vtableSize, itableSize)
        } else {
            ObjCTypeAdapterForBindClassToObjCNameWithReverseBridges(
                it.kotlinClass,
                it.objCName,
                reverseAdapters,
                vtableSize,
                itableSize,
            )
        }
        val typeAdapter = staticData.placeGlobal("", adapter).pointer
        // Register the adapter pointer so it ends up in Kotlin_ObjCExport_sortedClassAdapters /
        // sortedProtocolAdapters when ObjC-export finalizes the module. Without this, the runtime's
        // findProtocolAdapter / findClassAdapter cannot map a Swift class's protocol list (or objc
        // class name) back to the Kotlin TypeInfo, which is required for interface-conformance
        // discovery on Swift subclasses.
        val adaptersMap = if (it.kotlinClass.isInterface) {
            generationState.bindClassToObjCNameInterfaceAdapters
        } else {
            generationState.bindClassToObjCNameClassAdapters
        }
        adaptersMap[it.objCName] = typeAdapter
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