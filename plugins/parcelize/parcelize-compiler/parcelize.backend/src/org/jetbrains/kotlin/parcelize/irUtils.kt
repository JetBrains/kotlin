/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATE_FROM_PARCEL_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.NEW_ARRAY_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELIZE_CLASS_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_TO_PARCEL_NAME
import org.jetbrains.kotlin.parcelize.serializers.ParcelizeExtensionBase
import org.jetbrains.kotlin.types.Variance

// true if the class should be processed by the parcelize plugin
val IrClass.isParcelize: Boolean
    get() = kind in ParcelizeExtensionBase.ALLOWED_CLASS_KINDS &&
            (hasAnyAnnotation(PARCELIZE_CLASS_FQ_NAMES) || superTypes.any { superType ->
                superType.classOrNull?.owner?.let {
                    it.modality == Modality.SEALED && it.hasAnyAnnotation(PARCELIZE_CLASS_FQ_NAMES)
                } == true
            })

// Finds the getter for a pre-existing CREATOR field on the class companion, which is used for manual Parcelable implementations in Kotlin.
val IrClass.creatorGetter: IrSimpleFunctionSymbol?
    get() = companionObject()?.getPropertyGetter(CREATOR_NAME.asString())?.takeIf {
        it.owner.correspondingPropertySymbol?.owner?.backingField?.hasAnnotation(FqName("kotlin.jvm.JvmField")) == true
    }

// true if the class has a static CREATOR field
val IrClass.hasCreatorField: Boolean
    get() = fields.any { field -> field.name == CREATOR_NAME } || creatorGetter != null

// object P : Parceler<T> { fun T.write(parcel: Parcel, flags: Int) ...}
fun IrBuilderWithScope.parcelerWrite(
    parceler: IrClass,
    parcel: IrValueDeclaration,
    flags: IrValueDeclaration,
    value: IrExpression,
) = irCall(parceler.parcelerSymbolByName("write")!!).apply {
    dispatchReceiver = irGetObject(parceler.symbol)
    extensionReceiver = value
    putValueArgument(0, irGet(parcel))
    putValueArgument(1, irGet(flags))
}

// object P : Parceler<T> { fun create(parcel: Parcel): T }
fun IrBuilderWithScope.parcelerCreate(parceler: IrClass, parcel: IrValueDeclaration): IrExpression =
    irCall(parceler.parcelerSymbolByName("create")!!).apply {
        dispatchReceiver = irGetObject(parceler.symbol)
        putValueArgument(0, irGet(parcel))
    }

// object P: Parceler<T> { fun newArray(size: Int): Array<T> }
fun IrBuilderWithScope.parcelerNewArray(parceler: IrClass?, size: IrValueDeclaration): IrExpression? =
    parceler?.parcelerSymbolByName(NEW_ARRAY_NAME.identifier)?.takeIf {
        // The `newArray` method in `kotlinx.parcelize.Parceler` is stubbed out and we
        // have to produce a new implementation, unless the user overrides it.
        !it.owner.isFakeOverride || it.owner.resolveFakeOverride()?.parentClassOrNull?.fqNameWhenAvailable != PARCELER_FQN
    }?.let { newArraySymbol ->
        irCall(newArraySymbol).apply {
            dispatchReceiver = irGetObject(parceler.symbol)
            putValueArgument(0, irGet(size))
        }
    }

// class Parcelable { fun writeToParcel(parcel: Parcel, flags: Int) ...}
fun IrBuilderWithScope.parcelableWriteToParcel(
    parcelableClass: IrClass,
    parcelable: IrExpression,
    parcel: IrExpression,
    flags: IrExpression
): IrExpression {
    val writeToParcel = parcelableClass.functions.first { function ->
        function.name == WRITE_TO_PARCEL_NAME && function.overridesFunctionIn(PARCELABLE_FQN)
    }

    return irCall(writeToParcel).apply {
        dispatchReceiver = parcelable
        putValueArgument(0, parcel)
        putValueArgument(1, flags)
    }
}

// class C : Parcelable.Creator<T> { fun createFromParcel(parcel: Parcel): T ...}
fun IrBuilderWithScope.parcelableCreatorCreateFromParcel(creator: IrExpression, parcel: IrExpression): IrExpression {
    val createFromParcel = creator.type.getClass()!!.functions.first { function ->
        function.name == CREATE_FROM_PARCEL_NAME && function.overridesFunctionIn(CREATOR_FQN)
    }

    return irCall(createFromParcel).apply {
        dispatchReceiver = creator
        putValueArgument(0, parcel)
    }
}

// Construct an expression to access the parcelable creator field in the given class.
fun AndroidIrBuilder.getParcelableCreator(irClass: IrClass): IrExpression {
    // For classes annotated with `Parcelize` in the same module we can
    // use the creator field directly, since we add it ourselves.
    irClass.fields.find { it.name == CREATOR_NAME }?.let { creatorField ->
        return irGetField(null, creatorField)
    }

    // For parcelable classes which do not use `Parcelize`, the creator field
    // will be present as a `JvmField` on the companion object.
    irClass.creatorGetter?.let { getter ->
        return irCall(getter).apply {
            dispatchReceiver = irGetObject(irClass.companionObject()!!.symbol)
        }
    }

    // For classes in different modules, the creator field may not exist,
    // since static fields are not present in the Kotlin metadata.
    // As a workaround we create a synthetic field here together with a
    // field access.
    val creatorType = androidSymbols.androidOsParcelableCreator.typeWith(irClass.symbol.starProjectedType)
    val creatorField = irClass.factory.createField(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        IrFieldSymbolImpl(), CREATOR_NAME, creatorType, DescriptorVisibilities.PUBLIC,
        isFinal = true, isExternal = false, isStatic = true,
    ).also { it.parent = irClass }

    return irGetField(null, creatorField)
}

// Find a named function declaration which overrides the corresponding function in [Parceler].
// This is more reliable than trying to match the functions signature ourselves, since the frontend
// has already done the work.
private fun IrClass.parcelerSymbolByName(name: String): IrSimpleFunctionSymbol? =
    functions.firstOrNull { function ->
        function.name.asString() == name && function.overridesFunctionIn(PARCELER_FQN)
    }?.symbol

fun IrSimpleFunction.overridesFunctionIn(fqName: FqName): Boolean =
    parentClassOrNull?.fqNameWhenAvailable == fqName || allOverridden().any { it.parentClassOrNull?.fqNameWhenAvailable == fqName }

private fun IrBuilderWithScope.kClassReference(classType: IrType): IrClassReferenceImpl =
    IrClassReferenceImpl(
        startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, classType
    )

private fun AndroidIrBuilder.kClassToJavaClass(kClassReference: IrExpression): IrCall =
    irGet(androidSymbols.javaLangClass.starProjectedType, null, androidSymbols.kotlinKClassJava.owner.getter!!.symbol).apply {
        extensionReceiver = kClassReference
    }

// Produce a static reference to the java class of the given type.
fun AndroidIrBuilder.javaClassReference(classType: IrType): IrCall = kClassToJavaClass(kClassReference(classType))

fun IrClass.isSubclassOfFqName(fqName: String): Boolean =
    fqNameWhenAvailable?.asString() == fqName || superTypes.any { it.erasedUpperBound.isSubclassOfFqName(fqName) }

inline fun IrBlockBuilder.forUntil(upperBound: IrExpression, loopBody: IrBlockBuilder.(IrValueDeclaration) -> Unit) {
    val indexTemporary = irTemporary(irInt(0), isMutable = true)
    +irWhile().apply {
        condition = irNotEquals(irGet(indexTemporary), upperBound)
        body = irBlock {
            loopBody(indexTemporary)
            val inc = context.irBuiltIns.intClass.getSimpleFunction("inc")!!
            +irSet(indexTemporary.symbol, irCall(inc).apply {
                dispatchReceiver = irGet(indexTemporary)
            })
        }
    }
}

fun IrTypeArgument.upperBound(builtIns: IrBuiltIns): IrType =
    when (this) {
        is IrStarProjection -> builtIns.anyNType
        is IrTypeProjection -> {
            if (variance == Variance.OUT_VARIANCE || variance == Variance.INVARIANT)
                type
            else
                builtIns.anyNType
        }
    }

private fun IrClass.getSimpleFunction(name: String): IrSimpleFunctionSymbol? =
    findDeclaration<IrSimpleFunction> { it.name.asString() == name }?.symbol

// This is a version of getPropertyGetter which does not throw when applied to broken lazy classes, such as java.util.HashMap,
// which contains two "size" properties with different visibilities.
fun IrClass.getPropertyGetter(name: String): IrSimpleFunctionSymbol? =
    declarations.filterIsInstance<IrProperty>().firstOrNull { it.name.asString() == name && it.getter != null }?.getter?.symbol
        ?: getSimpleFunction("<get-$name>")

fun IrClass.getMethodWithoutArguments(name: String): IrSimpleFunction =
    functions.first { function ->
        function.name.asString() == name && function.dispatchReceiverParameter != null
                && function.extensionReceiverParameter == null && function.valueParameters.isEmpty()
    }

internal fun IrAnnotationContainer.hasAnyAnnotation(fqNames: List<FqName>): Boolean {
    for (fqName in fqNames) {
        if (hasAnnotation(fqName)) {
            return true
        }
    }

    return false
}

internal fun IrAnnotationContainer.getAnyAnnotation(fqNames: List<FqName>): IrConstructorCall? {
    for (fqName in fqNames) {
        val annotation = getAnnotation(fqName)
        if (annotation != null) {
            return annotation
        }
    }

    return null
}
