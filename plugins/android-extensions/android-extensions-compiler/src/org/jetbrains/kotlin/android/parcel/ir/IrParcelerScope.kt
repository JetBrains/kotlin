/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel.ir

import org.jetbrains.kotlin.android.parcel.ParcelableAnnotationChecker
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation

// Keep track of all custom parcelers which are currently in scope.
// Note that custom parcelers are resolved in *reverse* lexical order.
class IrParcelerScope(val parent: IrParcelerScope? = null) {
    private val typeParcelers = mutableMapOf<IrType, IrClass>()

    fun add(type: IrType, parceler: IrClass) {
        typeParcelers.putIfAbsent(type, parceler)
    }

    fun get(type: IrType): IrClass? =
        parent?.get(type) ?: typeParcelers[type]
}

fun IrParcelerScope?.getCustomSerializer(irType: IrType): IrClass? =
    irType.getAnnotation(ParcelableAnnotationChecker.WRITE_WITH_FQNAME)?.let { writeWith ->
        (writeWith.type as IrSimpleType).arguments.single().typeOrNull!!.getClass()!!
    } ?: this?.get(irType)

fun IrParcelerScope?.hasCustomSerializer(irType: IrType): Boolean =
    getCustomSerializer(irType) != null

fun IrAnnotationContainer.getParcelerScope(parent: IrParcelerScope? = null): IrParcelerScope? {
    val typeParcelerAnnotations = annotations.filterTo(mutableListOf()) {
        it.symbol.owner.constructedClass.fqNameWhenAvailable == ParcelableAnnotationChecker.TYPE_PARCELER_FQNAME
    }

    if (typeParcelerAnnotations.isEmpty())
        return parent

    val scope = IrParcelerScope(parent)

    for (annotation in typeParcelerAnnotations) {
        val (mappedType, parcelerType) = (annotation.type as IrSimpleType).arguments.map { it.typeOrNull!! }
        scope.add(mappedType, parcelerType.getClass()!!)
    }

    return scope
}
