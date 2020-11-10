/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.backend.konan.serialization.KonanFileMetadataSource
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleFragmentImpl
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.isPublicApi
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private fun IrClass.isClassTypeWithSignature(signature: IdSignature.PublicSignature): Boolean {
    if (!symbol.isPublicApi) return false
    return signature == symbol.signature
}

fun IrClass.isUnit() = this.isClassTypeWithSignature(IdSignatureValues.unit)

fun IrClass.isKotlinArray() = this.isClassTypeWithSignature(IdSignatureValues.array)

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.isClassTypeWithSignature(IdSignatureValues.any)

fun IrClass.isNothing() = this.isClassTypeWithSignature(IdSignatureValues.nothing)

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

// Note: psi2ir doesn't set `origin = FAKE_OVERRIDE` for fields and properties yet.
val IrProperty.isReal: Boolean get() = this.descriptor.kind.isReal
val IrField.isReal: Boolean get() = this.descriptor.kind.isReal

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != DescriptorVisibilities.PRIVATE
            && modality != Modality.FINAL
            && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrFunction.isOverridableOrOverrides
    get() = this is IrSimpleFunction && (this.isOverridable || this.overriddenSymbols.isNotEmpty())

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

inline fun <reified T> IrDeclaration.getAnnotationArgumentValue(fqName: FqName, argumentName: String): T? {
    val annotation = this.annotations.findAnnotation(fqName) ?: return null
    for (index in 0 until annotation.valueArgumentsCount) {
        val parameter = annotation.symbol.owner.valueParameters[index]
        if (parameter.name == Name.identifier(argumentName)) {
            val actual = annotation.getValueArgument(index).safeAs<IrConst<T>>()
            return actual?.value
        }
    }
    return null
}

fun IrValueParameter.isInlineParameter(): Boolean =
    !this.isNoinline && (this.type.isFunction() || this.type.isSuspendFunction()) && !this.type.isMarkedNullable()

val IrDeclaration.parentDeclarationsWithSelf: Sequence<IrDeclaration>
    get() = generateSequence(this, { it.parent as? IrDeclaration })

fun IrClass.companionObject() = this.declarations.filterIsInstance<IrClass>().atMostOne { it.isCompanion }

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

val ModuleDescriptor.konanLibrary get() = (this.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library
val IrModuleFragment.konanLibrary get() =
    (this as? KonanIrModuleFragmentImpl)?.konanLibrary ?: descriptor.konanLibrary
val IrFile.konanLibrary get() =
    (metadata as? KonanFileMetadataSource)?.module?.konanLibrary ?: packageFragmentDescriptor.containingDeclaration.konanLibrary
val IrDeclaration.konanLibrary: KotlinLibrary? get() {
    ((this as? IrMetadataSourceOwner)?.metadata as? KonanMetadata)?.let { return it.konanLibrary }
    val result = when (val parent = parent) {
        is IrFile -> parent.konanLibrary
        is IrPackageFragment -> parent.packageFragmentDescriptor.containingDeclaration.konanLibrary
        is IrDeclaration -> parent.konanLibrary
        else -> TODO("Unexpected declaration parent: $parent")
    }
    if (this is IrMetadataSourceOwner && this !is IrLazyDeclarationBase)
        metadata = KonanMetadata(metadata?.name, result)
    return result
}

fun IrDeclaration.isFromInteropLibrary() = konanLibrary?.isInteropLibrary() == true
