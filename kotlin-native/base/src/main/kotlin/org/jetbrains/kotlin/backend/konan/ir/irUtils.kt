/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.InternalKonanApi
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.utils.atMostOne

private fun IrClass.isClassTypeWithSignature(signature: IdSignature.CommonSignature): Boolean {
    return signature == symbol.signature
}

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }

fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.isClassTypeWithSignature(IdSignatureValues.any)

fun IrClass.isUnit() = this.isClassTypeWithSignature(IdSignatureValues.unit)

fun IrClass.isKotlinArray() = this.isClassTypeWithSignature(IdSignatureValues.array)

fun IrClass.isNothing() = this.isClassTypeWithSignature(IdSignatureValues.nothing)

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

fun IrValueParameter.isInlineParameter(): Boolean =
        !this.isNoinline && (this.type.isFunction() || this.type.isSuspendFunction()) && !this.type.isMarkedNullable()


fun buildSimpleAnnotation(irBuiltIns: IrBuiltIns, startOffset: Int, endOffset: Int,
                          annotationClass: IrClass, vararg args: String): IrConstructorCall {
    val constructor = annotationClass.constructors.let {
        it.singleOrNull() ?: it.single { ctor -> ctor.valueParameters.size == args.size }
    }
    return IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, constructor.returnType, constructor.symbol).apply {
        args.forEachIndexed { index, arg ->
            assert(constructor.valueParameters[index].type == irBuiltIns.stringType) {
                "String type expected but was ${constructor.valueParameters[index].type}"
            }
            putValueArgument(index, IrConstImpl.string(startOffset, endOffset, irBuiltIns.stringType, arg))
        }
    }
}

@InternalKonanApi
val IrFunctionAccessExpression.isVirtualCall: Boolean
    get() = this is IrCall && this.superQualifierSymbol == null && this.symbol.owner.isOverridable



val ModuleDescriptor.konanLibrary get() = (this.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library
