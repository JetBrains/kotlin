/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.parcelize.ParcelizeNames.TYPE_PARCELER_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_WITH_FQ_NAMES

// Keep track of all custom parcelers which are currently in scope.
// Note that custom parcelers are resolved in *reverse* lexical order.
class IrParcelerScope(private val parent: IrParcelerScope? = null) {
    private val typeParcelers = mutableMapOf<IrType, IrClass>()

    fun add(type: IrType, parceler: IrClass) {
        typeParcelers.putIfAbsent(type, parceler)
    }

    fun get(type: IrType): IrClass? =
        parent?.get(type) ?: typeParcelers[type]
}

fun IrParcelerScope?.getCustomSerializer(irType: IrType): IrClass? {
    return irType.getAnyAnnotation(WRITE_WITH_FQ_NAMES)?.let { writeWith ->
        (writeWith.type as IrSimpleType).arguments.single().typeOrNull!!.getClass()!!
    } ?: this?.get(irType)
}

fun IrParcelerScope?.hasCustomSerializer(irType: IrType): Boolean {
    return getCustomSerializer(irType) != null
}

fun IrAnnotationContainer.getParcelerScope(parent: IrParcelerScope? = null): IrParcelerScope? {
    val typeParcelerAnnotations = annotations.filterTo(mutableListOf()) {
        it.symbol.owner.constructedClass.fqNameWhenAvailable in TYPE_PARCELER_FQ_NAMES
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
