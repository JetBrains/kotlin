/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.isInteropLibrary
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.utils.atMostOne

private fun IrClass.isClassTypeWithSignature(signature: IdSignature.CommonSignature): Boolean {
    return signature == symbol.signature
}

fun IrClass.isUnit() = this.isClassTypeWithSignature(IdSignatureValues.unit)

fun IrClass.isKotlinArray() = this.isClassTypeWithSignature(IdSignatureValues.array)

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.isClassTypeWithSignature(IdSignatureValues.any)

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

internal fun IrExpression.isBoxOrUnboxCall() =
        (this is IrCall && symbol.owner.origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION)

internal val IrFunctionAccessExpression.actualCallee: IrFunction
    get() {
        val callee = symbol.owner
        return ((this as? IrCall)?.superQualifierSymbol?.owner?.getOverridingOf(callee) ?: callee).target
    }

internal val IrFunctionAccessExpression.isVirtualCall: Boolean
    get() = this is IrCall && this.superQualifierSymbol == null && this.symbol.owner.isOverridable

private fun IrClass.getOverridingOf(function: IrFunction) = (function as? IrSimpleFunction)?.let {
    it.allOverriddenFunctions.atMostOne { it.parent == this }
}

val ModuleDescriptor.konanLibrary get() = (this.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrPackageFragment.konanLibrary: KotlinLibrary?
    get() {
        if (this is IrFile) {
            val fileMetadata = metadata as? DescriptorMetadataSource.File
            val moduleDescriptor = fileMetadata?.descriptors?.singleOrNull() as? ModuleDescriptor
            moduleDescriptor?.konanLibrary?.let { return it }
        }
        return this.moduleDescriptor.konanLibrary
    }
// Any changes made to konanLibrary here should be ported to the containsDeclaration
// function in LlvmModuleSpecificationBase in LlvmModuleSpecificationImpl.kt
val IrDeclaration.konanLibrary: KotlinLibrary?
    get() {
        ((this as? IrMetadataSourceOwner)?.metadata as? KonanMetadata)?.let { return it.konanLibrary }
        return when (val parent = parent) {
            is IrPackageFragment -> parent.konanLibrary
            is IrDeclaration -> parent.konanLibrary
            else -> TODO("Unexpected declaration parent: $parent")
        }
    }

fun IrDeclaration.isFromInteropLibrary() = konanLibrary?.isInteropLibrary() == true
fun IrPackageFragment.isFromInteropLibrary() = konanLibrary?.isInteropLibrary() == true
