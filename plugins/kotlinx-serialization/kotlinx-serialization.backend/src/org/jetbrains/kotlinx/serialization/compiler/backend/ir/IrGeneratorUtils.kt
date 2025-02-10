/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
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

// TODO KT-53096
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
 * Find the [parentType] heirs along the chain up to [childType], with order from [childType] to the [parentType],
 * or `null` of [childType] isn't inheritor of [parentType].
 */
fun findPath(childType: IrSimpleType, parentType: IrSimpleType): List<IrSimpleType>? {
    return findPathInternal(childType, parentType)
}

private fun findPathInternal(
    type: IrSimpleType,
    targetParentType: IrSimpleType,
    prev: List<IrSimpleType> = emptyList(),
): List<IrSimpleType>? {
    val current = prev + type
    if (FqNameEqualityChecker.areEqual(targetParentType.classifier, type.classifier)) return current

    type.superTypes().asSequence().filterIsInstance<IrSimpleType>().forEach {
        val result = findPathInternal(it, targetParentType, current)
        if (result != null) return result
    }

    return null
}

/**
 * Try to find the use of type parameter of the parent type ([path]`.last()`) in the child ([path]`.first()`)
 *
 * It checks whether a type parameter with the [indexInChild] index is inherited from the [path]`.last()` parent,
 * if so, the index of the parent's type parameter is returned, if not, `null`.
 *
 * If [path] is empty then `null` is returned.
 *
 * Example:
 * ```
 *   class B<X, Y>: A<Int, String, X>()
 *   class A<X, Y, Z>
 * ```
 * Path is [`B`, `A`]
 *  - for [indexInChild]` = 0` function will return `2`
 *  - for [indexInChild]` = 1` function will return `null` - index aren't reused in `A`
 *  - for [indexInChild]` = 2` function will return `null` - no type parameter with such index in `B`
 */
fun mapTypeParameterIndex(indexInChild: Int, path: List<IrSimpleType>): Int? {
    if (path.isEmpty()) return null

    var expectedType: IrSimpleType = path[0]
    var expectedIndex = indexInChild

    path.forEach { type ->
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

