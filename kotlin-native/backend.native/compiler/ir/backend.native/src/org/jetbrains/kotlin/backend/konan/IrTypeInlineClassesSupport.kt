/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne

fun IrType.getInlinedClassNative(): IrClass? = IrTypeInlineClassesSupport.getInlinedClass(this)

fun IrType.isInlinedNative(): Boolean = IrTypeInlineClassesSupport.isInlined(this)

fun IrClass.isInlined(): Boolean = IrTypeInlineClassesSupport.isInlined(this)

fun IrClass.isNativePrimitiveType() = IrTypeInlineClassesSupport.isTopLevelClass(this) &&
        KonanPrimitiveType.byFqNameParts[packageFqName]?.get(name) != null

fun IrType.computePrimitiveBinaryTypeOrNull(): PrimitiveBinaryType? =
        this.computeBinaryType().primitiveBinaryTypeOrNull()

fun IrType.computeBinaryType(): BinaryType<IrClass> = IrTypeInlineClassesSupport.computeBinaryType(this)

fun IrClass.inlinedClassIsNullable(): Boolean = this.defaultType.makeNullable().getInlinedClassNative() == this // TODO: optimize

fun IrClass.isUsedAsBoxClass(): Boolean = IrTypeInlineClassesSupport.isUsedAsBoxClass(this)

fun IrType.binaryTypeIsReference(): Boolean = this.computePrimitiveBinaryTypeOrNull() == null

internal inline fun <R> IrType.unwrapToPrimitiveOrReference(
        eachInlinedClass: (inlinedClass: IrClass, nullable: Boolean) -> Unit,
        ifPrimitive: (primitiveType: KonanPrimitiveType, nullable: Boolean) -> R,
        ifReference: (type: IrType) -> R
): R = IrTypeInlineClassesSupport.unwrapToPrimitiveOrReference(this, eachInlinedClass, ifPrimitive, ifReference)

internal object IrTypeInlineClassesSupport : InlineClassesSupport<IrClass, IrType>() {

    override fun isNullable(type: IrType): Boolean = type.isNullable()

    override fun makeNullable(type: IrType): IrType = type.makeNullable()

    override tailrec fun erase(type: IrType): IrClass {
        val classifier = type.classifierOrFail
        return when (classifier) {
            is IrClassSymbol -> classifier.owner
            is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
            else -> error(classifier)
        }
    }

    override fun computeFullErasure(type: IrType): Sequence<IrClass> = when (val classifier = type.classifierOrFail) {
        is IrClassSymbol -> sequenceOf(classifier.owner)
        is IrTypeParameterSymbol -> classifier.owner.superTypes.asSequence().flatMap { computeFullErasure(it) }
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }

    override fun hasInlineModifier(clazz: IrClass): Boolean = clazz.isSingleFieldValueClass

    override fun getNativePointedSuperclass(clazz: IrClass): IrClass? {
        var superClass: IrClass? = clazz
        while (superClass != null && InteropFqNames.nativePointed.toSafe() != superClass.fqNameWhenAvailable)
            superClass = superClass.getSuperClassNotAny()
        return superClass
    }

    override fun getInlinedClassUnderlyingType(clazz: IrClass): IrType =
            clazz.constructors.firstOrNull { it.isPrimary }?.valueParameters?.single()?.type
                    ?: clazz.declarations.filterIsInstance<IrProperty>().atMostOne { it.backingField?.takeUnless { it.isStatic } != null }?.backingField?.type
                    ?: clazz.inlineClassRepresentation!!.underlyingType

    override fun getPackageFqName(clazz: IrClass) =
            clazz.packageFqName

    override fun getName(clazz: IrClass): Name? =
            clazz.name

    override fun isTopLevelClass(clazz: IrClass): Boolean = clazz.isTopLevel
}