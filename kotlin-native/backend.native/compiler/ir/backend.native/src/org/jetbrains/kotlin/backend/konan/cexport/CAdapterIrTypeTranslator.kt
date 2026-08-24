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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * IR counterpart of [CAdapterTypeTranslator]: maps an [IrType] to its C export spellings. It mirrors the K1
 * translator method-for-method, but classifies types via the IR inline-classes support instead of descriptors.
 * All C spellings come from the shared [CAdapterCAbi], so the IR and K1 modes never drift apart.
 */
internal class CAdapterIrTypeTranslator(val prefix: String) {
    private val primitiveTypeMapping = KonanPrimitiveType.entries.associateWith {
        CAdapterCAbi.primitiveCType(prefix, it)
    }
    private val unsignedTypeMapping = CAdapterCAbi.unsignedCTypesByClassId(prefix)

    fun isMappedToVoid(type: IrType): Boolean = type.isUnit() || type.isNothing()

    fun isMappedToString(type: IrType): Boolean {
        if (isMappedToVoid(type)) return false
        val binaryType = type.computeBinaryType()
        return binaryType is BinaryType.Reference && binaryType.types.first().isStringClass()
    }

    fun isMappedToReference(type: IrType): Boolean =
            !isMappedToVoid(type) && !isMappedToString(type) && type.binaryTypeIsReference()

    fun translateType(type: IrType): String = translateTypeFull(type).first

    fun translateTypeBridge(type: IrType): String = translateTypeFull(type).second

    fun exportedType(type: IrType): CExportedType = CExportedTypeIr(type, this)

    private fun translateTypeFull(type: IrType): Pair<String, String> =
            if (isMappedToVoid(type)) {
                "void" to "void"
            } else {
                translateNonVoidTypeFull(type)
            }

    private fun translateNonVoidTypeFull(type: IrType): Pair<String, String> = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ ->
                inlinedClass.classId?.let { unsignedTypeMapping[it] }?.let {
                    return it to it
                }
            },
            ifPrimitive = { primitiveType, _ ->
                primitiveTypeMapping.getValue(primitiveType).let { it to it }
            },
            ifReference = {
                val clazz = (it.computeBinaryType() as BinaryType.Reference).types.first()
                if (clazz.isStringClass()) {
                    "const char*" to "KObjHeader*"
                } else {
                    CAdapterCAbi.krefTypeName(prefix, clazz.fqNameWhenAvailable!!.asString()) to "KObjHeader*"
                }
            }
    )

    private fun IrClass.isStringClass(): Boolean = classId == StandardClassIds.String
}

internal class CExportedTypeIr(
        private val type: IrType,
        private val typeTranslator: CAdapterIrTypeTranslator,
) : CExportedType {
    override fun translateType(): String = typeTranslator.translateType(type)
    override fun translateTypeBridge(): String = typeTranslator.translateTypeBridge(type)
    override fun isMappedToVoid(): Boolean = typeTranslator.isMappedToVoid(type)
    override fun isMappedToString(): Boolean = typeTranslator.isMappedToString(type)
    override fun isMappedToReference(): Boolean = typeTranslator.isMappedToReference(type)

    // Two exported types are the same C type iff they render the same. Used to de-duplicate the typedef section.
    override fun equals(other: Any?): Boolean =
            other is CExportedType && other.translateType() == translateType()

    override fun hashCode(): Int = translateType().hashCode()
}

/**
 * An exported reference ("kref") type identified directly by its Kotlin fq-name rather than by a backing [IrType].
 * Used for enum entries, whose own synthetic type has no dedicated [IrType] but is still exposed as a `kref` typedef
 * (matching the K1 mode, where each entry is a distinct class with its own default type).
 */
internal class CExportedReferenceTypeByFqName(prefix: String, fqName: String) : CExportedType {
    private val krefName = CAdapterCAbi.krefTypeName(prefix, fqName)
    override fun translateType(): String = krefName
    override fun translateTypeBridge(): String = "KObjHeader*"
    override fun isMappedToVoid(): Boolean = false
    override fun isMappedToString(): Boolean = false
    override fun isMappedToReference(): Boolean = true
    override fun equals(other: Any?): Boolean =
            other is CExportedType && other.translateType() == translateType()

    override fun hashCode(): Int = translateType().hashCode()
}
