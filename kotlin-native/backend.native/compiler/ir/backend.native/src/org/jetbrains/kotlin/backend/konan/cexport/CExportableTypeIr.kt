/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.computeBinaryType
import org.jetbrains.kotlin.backend.konan.unwrapToPrimitiveOrReference
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.ClassId

internal fun IrType.asCExportableType(): CExportableType = IrCExportableType(this)

private class IrCExportableType(private val type: IrType) : CExportableType {
    override fun isUnit(): Boolean = type.isUnit()

    override fun isNothing(): Boolean = type.isNothing()

    override fun binaryTypeIsReference(): Boolean = type.binaryTypeIsReference()

    override fun referenceClassOrNull(): CExportableClass? =
            (type.computeBinaryType() as? BinaryType.Reference)?.types?.first()?.let(::IrCExportableClass)

    override fun <R : Any> unwrapToPrimitiveOrReference(
            eachInlinedClass: (CExportableClass) -> R?,
            ifPrimitive: (KonanPrimitiveType) -> R,
            ifReference: (CExportableClass) -> R,
    ): R = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ -> eachInlinedClass(IrCExportableClass(inlinedClass))?.let { return it } },
            ifPrimitive = { primitiveType, _ -> ifPrimitive(primitiveType) },
            ifReference = { referenceType ->
                ifReference(IrCExportableClass((referenceType.computeBinaryType() as BinaryType.Reference).types.first()))
            },
    )
}

private class IrCExportableClass(private val irClass: IrClass) : CExportableClass {
    override val classId: ClassId? get() = irClass.classId
    override fun isString(): Boolean = irClass.symbol.signature == IdSignatureValues.string
    override val fqName: String get() = irClass.fqNameWhenAvailable!!.asString()
}

/**
 * An exported reference ("kref") type identified directly by its Kotlin fq-name rather than by a backing [IrType].
 * Used for enum entries: in K1 each entry is a distinct class with its own default type, so the descriptor mode
 * emits a per-entry `kref` typedef. Nothing in the generated C API actually references these typedefs (entries are
 * exposed through the enum class's type, not their own) — they are dead declarations that exist solely to keep the
 * IR mode's output byte-identical to K1's.
 * TODO: Consider removing this in KT-90281
 */
internal class CExportedReferenceTypeByFqName(prefix: String, fqName: String) : CExportedType() {
    private val krefName = CAdapterCAbi.krefTypeName(prefix, fqName)
    override fun translateType(): String = krefName
    override fun translateTypeBridge(): String = "KObjHeader*"
    override fun isMappedToVoid(): Boolean = false
    override fun isMappedToString(): Boolean = false
    override fun isMappedToReference(): Boolean = true
}
