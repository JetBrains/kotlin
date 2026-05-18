/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.superTypes
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

fun IrParcelerScope?.getCustomSerializer(irType: IrType): IrParcelSerializer? {
    val writeWithArgumentType = irType.getAnyAnnotation(WRITE_WITH_FQ_NAMES)?.let { writeWith ->
        (writeWith.type as IrSimpleType).arguments.single().typeOrNull
    }
    writeWithArgumentType?.getClass()?.let { parceler ->
        val customSerializer = IrCustomParcelSerializer(parceler)
        val parcelerSupertype = writeWithArgumentType.superTypes()
            .single { it.classOrNull?.owner?.classId in ParcelizeNames.PARCELER_CLASS_IDS } as IrSimpleType
        val parcelerTypeArgument = parcelerSupertype.arguments.single().typeOrFail
        return if (irType.isNullable() && !parcelerTypeArgument.isNullable()) IrNullAwareParcelSerializer(customSerializer)
        else customSerializer
    }

    this?.get(irType)?.let { return IrCustomParcelSerializer(it) }

    return if (irType.isNullable()) {
        this?.get(irType.makeNotNull())?.let { IrNullAwareParcelSerializer(IrCustomParcelSerializer(it)) }
    } else {
        null
    }
}

fun IrParcelerScope?.hasCustomSerializer(irType: IrType): Boolean {
    return getCustomSerializer(irType) != null
}

fun IrAnnotationContainer.getParcelerScope(parent: IrParcelerScope? = null): IrParcelerScope? {
    val typeParcelerAnnotations = annotations.filterTo(mutableListOf()) {
        it.classSymbol.owner.fqNameWhenAvailable in TYPE_PARCELER_FQ_NAMES
    }

    if (typeParcelerAnnotations.isEmpty())
        return parent

    val scope = IrParcelerScope(parent)

    for (annotation in typeParcelerAnnotations) {
        val [mappedType, parcelerType] = (annotation.type as IrSimpleType).arguments.map { it.typeOrNull!! }
        scope.add(mappedType, parcelerType.getClass()!!)
    }

    return scope
}
