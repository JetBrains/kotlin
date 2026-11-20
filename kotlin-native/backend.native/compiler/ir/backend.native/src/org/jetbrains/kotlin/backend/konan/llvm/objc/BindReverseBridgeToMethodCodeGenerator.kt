/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objc

import org.jetbrains.kotlin.backend.konan.ir.annotations.BindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.annotations.allBindReverseBridgeToMethod
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.findOverriddenMethodOfAny
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.simpleFunctions
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
 * grouped by target class, and resolves each to a [ReverseBridgeAdapter]
 * containing the vtable index and bridge function pointer.
 *
 * The result is used by [processBindClassToObjCNameAnnotations] to create
 * combined type adapters with both class binding and reverse bridges.
 */
internal fun CodeGenerator.collectReverseBridgeAdapters(file: IrFile): Map<IrClass, List<ReverseBridgeAdapter>> {
    val bridgesByClass = file.allBindReverseBridgeToMethod.groupBy { it.targetClass }
    return bridgesByClass.mapValues { [irClass, bridges] ->
        val layoutBuilder = generationState.context.getLayoutBuilder(irClass)
        bridges.map { bridge -> resolveReverseBridgeAdapter(irClass, layoutBuilder, bridge) }
    }.filterValues { it.isNotEmpty() }
}

private fun CodeGenerator.resolveReverseBridgeAdapter(
        irClass: IrClass,
        layoutBuilder: ClassLayoutBuilder,
        bridge: BindReverseBridgeToMethod,
): ReverseBridgeAdapter {
    val targetFunction = findTargetFunction(irClass, layoutBuilder, bridge)

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

    return ReverseBridgeAdapter(
            selector = bridge.targetMethod,
            itablePlace = itablePlace,
            vtableIndex = vtableIndex,
            kotlinImpl = getLlvmFunctionFrom(bridge.bridgeFunction),
    )
}

private fun findTargetFunction(
        irClass: IrClass,
        layoutBuilder: ClassLayoutBuilder,
        bridge: BindReverseBridgeToMethod,
): IrSimpleFunction {
    val bridgeFunction = bridge.bridgeFunction
    fun cannotBind(reason: String): Nothing =
            error("Cannot bind ${bridgeFunction.render()} to '${bridge.targetMethod}' of ${irClass.fqNameWhenAvailable}: $reason")

    val candidates = irClass.simpleFunctions()
            .filter { it.name.asString() == bridge.targetMethod }
            .map { with(layoutBuilder) { it.getLoweredVersion() } }
            .filter { it.bridgeTarget == null }
            .takeIfNotEmpty()
            ?: cannotBind("no such method")

    candidates.singleOrNull()?.let { return it }

    val erasedMatches = candidates.filter {
        it.hasMatchingSignatureTo(bridgeFunction) { l, r -> l.erasedUpperBound == r.erasedUpperBound && l.isNullable() == r.isNullable() }
    }

    return erasedMatches.singleOrNull()
            ?: erasedMatches.singleOrNull { it.hasMatchingSignatureTo(bridgeFunction) { l, r -> l == r } }
            ?: cannotBind("parameter types do not single out one overload; matching overloads:\n${erasedMatches.joinToString("\n") { "    ${it.render()}" }}")
}

private fun IrSimpleFunction.hasMatchingSignatureTo(
        bridgeFunction: IrSimpleFunction,
        typesMatch: (type: IrType, bridgeType: IrType) -> Boolean,
): Boolean {
    val valueParameters = parameters.filter { it.kind != IrParameterKind.DispatchReceiver }

    val bridgeValueParameters = bridgeFunction.parameters.filter { it.kind == IrParameterKind.Regular }.drop(1)

    return valueParameters.size == bridgeValueParameters.size &&
            valueParameters.zip(bridgeValueParameters).all { [parameter, bridgeParameter] ->
                typesMatch(parameter.type, bridgeParameter.type)
            }
}
