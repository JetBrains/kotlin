/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.lower.getRichPropertyReferenceForOptimizableDelegatedProperty
import org.jetbrains.kotlin.backend.jvm.lower.getSingletonOrConstantForOptimizableDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.superTypes

fun IrPluginContext.generateBodyForDefaultConstructor(declaration: IrConstructor): IrBody? {
    val type = declaration.returnType as? IrSimpleType ?: return null

    val delegatingAnyCall = IrDelegatingConstructorCallImpl(
        -1,
        -1,
        irBuiltIns.anyType,
        irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return null,
        typeArgumentsCount = 0,
    )

    val initializerCall = IrInstanceInitializerCallImpl(
        -1,
        -1,
        (declaration.parent as? IrClass)?.symbol ?: return null,
        irBuiltIns.unitType,
    )

    return irFactory.createBlockBody(-1, -1, listOf(delegatingAnyCall, initializerCall))
}

fun IrClass.addDefaultConstructorBodyIfAbsent(ctx: IrPluginContext) {
    val declaration = primaryConstructor ?: return
    if (declaration.body == null) declaration.body = ctx.generateBodyForDefaultConstructor(declaration)
}

/**
 * Returns the type path from [childType] to [parentType], inclusive,
 * or an empty list if [childType] is not a subtype of [parentType].
 */
fun findSupertypePath(childType: IrSimpleType, parentType: IrSimpleType): List<IrSimpleType> {
    return findPathInternal(childType, parentType)
}

private fun findPathInternal(
    type: IrSimpleType,
    targetParentType: IrSimpleType,
    prev: List<IrSimpleType> = emptyList(),
): List<IrSimpleType> {
    val current = prev + type
    if (FqNameEqualityChecker.areEqual(targetParentType.classifier, type.classifier)) return current

    type.superTypes().asSequence().filterIsInstance<IrSimpleType>().forEach {
        val result = findPathInternal(it, targetParentType, current)
        if (result.isNotEmpty()) return result
    }

    return emptyList()
}

/**
 * Tries to map the type parameter at [indexInChild] from the child type
 * ([supertypePath].first()) to the parent type ([supertypePath].last()).
 *
 * Returns its index in the parent type, or `null` if it is not propagated through the path.
 *
 * If [supertypePath] is empty then `null` is returned.
 *
 * Example:
 * ```
 *   class B<X, Y>: A<Int, String, X>()
 *   class A<X, Y, Z>
 * ```
 * Path is [`B`, `A`]
 *  - for [indexInChild]` = 0` function will return `2`
 *  - for [indexInChild]` = 1` function will return `null` - the type parameter is not used in `A`
 *  - for [indexInChild]` = 2` function will return `null` - `B` has no type parameter at this index
 */
fun findIndexInParent(indexInChild: Int, supertypePath: List<IrSimpleType>): Int? {
    if (supertypePath.isEmpty()) return null

    var expectedType: IrSimpleType = supertypePath[0]
    var expectedIndex = indexInChild

    supertypePath.forEach { type ->
        type.arguments.forEachIndexed { i, arg ->
            val typeParameter = ((type.arguments[i] as? IrSimpleType)?.classifier as? IrTypeParameterSymbol)?.owner ?: return@forEachIndexed
            if (typeParameter.index == expectedIndex && typeParameter.belongsClass(expectedType)) {
                expectedType = type
                expectedIndex = i
                return@forEach
            }
        }
        // no type parameter found
        return null
    }

    return expectedIndex
}

/**
 * Checks if type parameter is defined in [typeOfClass] class definition.
 */
private fun IrTypeParameter.belongsClass(typeOfClass: IrSimpleType): Boolean {
    val classInParameter = parent as? IrClass ?: return false
    val classOfType = typeOfClass.getClass() ?: return false

    val classId = classOfType.classId
    return classId != null && classId == classInParameter.classId
}

/** Returns true if a delegate is optimizable on the JVM, omitting a `$delegate` auxiliary property */
internal fun IrProperty.isJvmOptimizableDelegate(): Boolean =
    isDelegated && !isFakeOverride && backingField != null && // fast path
            (getRichPropertyReferenceForOptimizableDelegatedProperty() != null || getSingletonOrConstantForOptimizableDelegatedProperty() != null)


internal val IrProperty.isNonStaticWithField get() = backingField != null && getter?.dispatchReceiverParameter != null
