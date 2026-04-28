/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.konan.ir.annotations.BindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.KotlinToObjCMethodAdapter
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.KotlinToObjCMethodAdapter.Companion.KotlinToObjCMethodAdapter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.findOverriddenMethodOfAny
import org.jetbrains.kotlin.ir.util.isInterface

/**
 * Collects `@BindReverseBridgeToMethod` annotations from the given file,
 * grouped by target class, and resolves each to a [KotlinToObjCMethodAdapter]
 * containing the vtable index and bridge function pointer.
 *
 * The result is used by [processBindClassToObjCNameAnnotations] to create
 * combined type adapters with both class binding and reverse bridges.
 */
internal fun CodeGenerator.collectReverseBridgeAdapters(file: IrFile): Map<IrClass, List<KotlinToObjCMethodAdapter>> {
    val bridgesByClass = file.allBindReverseBridgeToMethod.groupBy { it.targetClass }
    return bridgesByClass.mapValues { (irClass, bridges) ->
        val layoutBuilder = generationState.context.getLayoutBuilder(irClass)
        bridges.mapNotNull { bridge ->
            resolveReverseBridgeAdapter(irClass, layoutBuilder, bridge)
        }
    }.filterValues { it.isNotEmpty() }
}

private fun CodeGenerator.resolveReverseBridgeAdapter(
    irClass: IrClass,
    layoutBuilder: ClassLayoutBuilder,
    bridge: BindReverseBridgeToMethod,
): KotlinToObjCMethodAdapter? {
    val targetFunction = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == bridge.targetMethod }
            ?: return null

    val isInterfaceMethod = irClass.isInterface
    val vtableIndex = if (isInterfaceMethod) {
        -1
    } else {
        layoutBuilder.vtableIndex(targetFunction)
    }

    val itablePlace = if (isInterfaceMethod && targetFunction.findOverriddenMethodOfAny() == null) {
        layoutBuilder.itablePlace(targetFunction)
    } else {
        ClassLayoutBuilder.InterfaceTablePlace.INVALID
    }

    return KotlinToObjCMethodAdapter(
        selector = bridge.targetMethod,
        itablePlace = itablePlace,
        vtableIndex = vtableIndex,
        kotlinImpl = llvmFunction(bridge.bridgeFunction).toConstPointer(),
    )
}
