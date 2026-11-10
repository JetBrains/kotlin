/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.konan.ir.annotations.BindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.ir.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.ir.bridgeDirectionsTo
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.LlvmCallable
import org.jetbrains.kotlin.backend.konan.serialization.SerializedObjCReverseBridge
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
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.utils.addToStdlib.takeIfNotEmpty

/**
 * A resolved `@BindReverseBridgeToMethod` annotation: the place of the overridden method in the class layout,
 * plus the bridge function to dispatch to.
 */
internal class ReverseBridgeAdapter(
        val selector: String,
        val itablePlace: ClassLayoutBuilder.InterfaceTablePlace,
        val vtableIndex: Int,
        val kotlinImpl: LlvmCallable,
)

internal fun CodeGenerator.KotlinToObjCMethodAdapter(adapter: ReverseBridgeAdapter) = KotlinToObjCMethodAdapter(
        selector = adapter.selector,
        itablePlace = adapter.itablePlace,
        vtableIndex = adapter.vtableIndex,
        kotlinImpl = adapter.kotlinImpl.toConstPointer(),
)

/**
 * Collects `@BindReverseBridgeToMethod` annotations from the given file,
 * grouped by target class, and resolves each to the [ReverseBridgeAdapter]s
 * containing the vtable index or itable place and bridge function pointer.
 *
 * The result is used by [processBindClassToObjCNameAnnotations] to create
 * combined type adapters with both class binding and reverse bridges.
 */
internal fun CodeGenerator.collectReverseBridgeAdapters(file: IrFile): Map<IrClass, List<ReverseBridgeAdapter>> {
    val bridgesByClass = file.allBindReverseBridgeToMethod.groupBy { it.targetClass }
    return bridgesByClass.mapValues { [irClass, bridges] ->
        val layoutBuilder = generationState.context.getLayoutBuilder(irClass)
        bridges.flatMap { bridge ->
            resolveReverseBridgeAdapters(irClass, layoutBuilder, bridge) ?: error(
                    "Cannot bind ${bridge.bridgeFunction.render()} to '${bridge.targetMethod}'"
            )
        }
    }.filterValues { it.isNotEmpty() }
}

private fun CodeGenerator.resolveReverseBridgeAdapters(
        irClass: IrClass,
        layoutBuilder: ClassLayoutBuilder,
        bridge: BindReverseBridgeToMethod,
): List<ReverseBridgeAdapter>? {
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

    val kotlinImpl = getLlvmFunctionFrom(bridge.bridgeFunction)
    val adapter = ReverseBridgeAdapter(
            selector = bridge.targetMethod,
            itablePlace = itablePlace,
            vtableIndex = vtableIndex,
            kotlinImpl = kotlinImpl,
    )
    if (isInterfaceMethod) return listOf(adapter)
    return listOf(adapter) + implementedInterfaceSlotAdapters(targetFunction, bridge.targetMethod, kotlinImpl)
}

private fun CodeGenerator.implementedInterfaceSlotAdapters(
        targetFunction: IrSimpleFunction,
        selector: String,
        kotlinImpl: LlvmCallable,
): List<ReverseBridgeAdapter> {
    val bridgesPolicy = generationState.config.bridgesPolicy
    val implementation = targetFunction.target
    if (!implementation.bridgeDirectionsTo(targetFunction, bridgesPolicy).allNotNeeded()) return emptyList()
    return targetFunction.allOverriddenFunctions
            .filter { it.parentAsClass.isInterface && it.findOverriddenMethodOfAny() == null }
            .filter { implementation.bridgeDirectionsTo(it, bridgesPolicy).allNotNeeded() }
            .map { generationState.context.getLayoutBuilder(it.parentAsClass).itablePlace(it) }
            .distinct()
            .map { itablePlace ->
                ReverseBridgeAdapter(selector = selector, itablePlace = itablePlace, vtableIndex = -1, kotlinImpl = kotlinImpl)
            }
}

private fun IrSimpleFunction.hasMatchingSignatureTo(bridgeFunction: IrSimpleFunction): Boolean {
    val valueParameters = parameters.filter { it.kind != IrParameterKind.DispatchReceiver }

    val bridgeValueParameters = bridgeFunction.parameters.filter { it.kind == IrParameterKind.Regular }.drop(1)

    return valueParameters.size == bridgeValueParameters.size &&
            valueParameters.zip(bridgeValueParameters).all { [parameter, bridgeParameter] ->
                parameter.type.erasedUpperBound == bridgeParameter.type.erasedUpperBound &&
                        parameter.type.isNullable() == bridgeParameter.type.isNullable()
            }
}
