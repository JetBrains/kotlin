/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.ir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName

fun <T> IrConstructorCall.getConstValueArgument(idx: Int, kind: IrConstKind<T>): T {
    val arg = (getValueArgument(idx) as? IrConst<*>) ?: throw IllegalStateException("Argument of ${this.symbol.owner.name} is not const expr")
    if (arg.kind != kind) {
        error("Argument of ${this.symbol.owner.name} is not a $kind")
    }
    return kind.valueOf(arg)
}

fun getConstructorTypeArgument(call: IrConstructorCall, idx: Int): IrType? {
    return (call.type as? IrSimpleType)?.arguments?.get(idx)?.typeOrNull
}

fun IrPluginContext.makeDeclBuilder(symbol: IrSymbol): DeclarationIrBuilder {
    return DeclarationIrBuilder(IrGeneratorContextBase(irBuiltIns), symbol, -1, -1)
}

fun IrType.isClass(): Boolean {
    return classOrNull?.owner?.kind == ClassKind.CLASS
}

fun IrClass.getSuperTypesClassSequence(): Sequence<IrClass> {
    return sequence {
        var parent = superTypes.find { it.isClass() }?.getClass()
        while (parent != null) {
            yield(parent)
            parent = parent.superTypes.find { it.isClass() }?.getClass()
        }
    }
}

fun IrSimpleFunction.getMainOverriddenSequence(): Sequence<IrSimpleFunction> {
    return sequence {
        var next = overriddenSymbols.find { it.owner.parentAsClass.kind == ClassKind.CLASS }?.owner
        while (next != null) {
            yield(next)
            next = next.overriddenSymbols.find { it.owner.parentAsClass.kind == ClassKind.CLASS }?.owner
        }
    }
}

fun IrSimpleFunction.getTopmostBaseFunction(): IrSimpleFunction {
    return getMainOverriddenSequence().lastOrNull() ?: this
}

fun IrDeclaration.isGeneratedBy(key: GeneratedDeclarationKey): Boolean {
    val origin = origin
    return origin is IrDeclarationOrigin.GeneratedByPlugin && origin.pluginKey == key
}

fun IrSimpleFunction.fqName(): FqName {
    return parent.kotlinFqName.child(name)
}