/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.konan.ir.annotations.BindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.KotlinToObjCMethodAdapter
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.KotlinToObjCMethodAdapter.Companion.KotlinToObjCMethodAdapter
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.findOverriddenMethodOfAny
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.utils.addToStdlib.takeIfNotEmpty

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
    return bridgesByClass.mapValues { [irClass, bridges] ->
        val layoutBuilder = generationState.context.getLayoutBuilder(irClass)
        bridges.mapNotNull { bridge ->
            resolveReverseBridgeAdapter(irClass, layoutBuilder, bridge) ?: error(
                    "Cannot bind ${bridge.bridgeFunction.render()} to '${bridge.targetMethod}'"
            )
        }
    }.filterValues { it.isNotEmpty() }
}

private fun CodeGenerator.resolveReverseBridgeAdapter(
        irClass: IrClass,
        layoutBuilder: ClassLayoutBuilder,
        bridge: BindReverseBridgeToMethod,
): KotlinToObjCMethodAdapter? {
    val candidates = irClass.simpleFunctions()
            .filter { it.name.asString() == bridge.targetMethod }
            .map { with(layoutBuilder) { it.getLoweredVersion() } }
            .filter { it.bridgeTarget == null }
            .takeIfNotEmpty()
            ?: return null

    val targetFunction = candidates.singleOrNull()
            ?: candidates.singleOrNull { it.hasMatchingSignatureTo(bridge.bridgeFunction) }
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
            kotlinImpl = getLlvmFunctionFrom(bridge.bridgeFunction).toConstPointer(),
    )
}

private fun IrSimpleFunction.hasMatchingSignatureTo(bridgeFunction: IrSimpleFunction): Boolean {
    val valueParameters = parameters.filter { it.kind == IrParameterKind.Regular }
    val bridgeValueParameters = bridgeFunction.parameters.filter { it.kind == IrParameterKind.Regular }.drop(1)

    return valueParameters.size == bridgeValueParameters.size &&
            valueParameters.zip(bridgeValueParameters).all { [parameter, bridgeParameter] ->
                parameter.type.erasedUpperBound == bridgeParameter.type.erasedUpperBound &&
                        parameter.type.isNullable() == bridgeParameter.type.isNullable()
            }
}
