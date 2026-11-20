/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType

/**
 * Maps a [CExportableType] to its C export spellings. A single translator serves both the K1 and IR modes — the
 * only representation-specific logic lives behind [CExportableType], not here.
 */
internal class CAdapterTypeTranslator(val prefix: String) {
    private val primitiveTypeMapping = KonanPrimitiveType.entries.associateWith { CAdapterCAbi.primitiveCType(prefix, it) }
    private val unsignedTypeMapping = CAdapterCAbi.unsignedCTypesByClassId(prefix)

    fun isMappedToVoid(type: CExportableType): Boolean = type.isUnit() || type.isNothing()

    fun isMappedToString(type: CExportableType): Boolean =
            !isMappedToVoid(type) && type.referenceClassOrNull()?.isString() == true

    fun isMappedToReference(type: CExportableType): Boolean =
            !isMappedToVoid(type) && !isMappedToString(type) && type.binaryTypeIsReference()

    fun translateType(type: CExportableType): String = translateTypeFull(type).first

    fun translateTypeBridge(type: CExportableType): String = translateTypeFull(type).second

    fun exportedType(type: CExportableType): CExportedType = TranslatedType(type)

    private fun translateTypeFull(type: CExportableType): Pair<String, String> =
            if (isMappedToVoid(type)) "void" to "void" else translateNonVoidTypeFull(type)

    private fun translateNonVoidTypeFull(type: CExportableType): Pair<String, String> = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { clazz -> clazz.classId?.let { unsignedTypeMapping[it] }?.let { it to it } },
            ifPrimitive = { primitiveTypeMapping.getValue(it).let { cType -> cType to cType } },
            ifReference = { clazz ->
                if (clazz.isString()) "const char*" to "KObjHeader*"
                else CAdapterCAbi.krefTypeName(prefix, clazz.fqName) to "KObjHeader*"
            },
    )

    private inner class TranslatedType(private val type: CExportableType) : CExportedType() {
        override fun translateType(): String = this@CAdapterTypeTranslator.translateType(type)
        override fun translateTypeBridge(): String = this@CAdapterTypeTranslator.translateTypeBridge(type)
        override fun isMappedToVoid(): Boolean = this@CAdapterTypeTranslator.isMappedToVoid(type)
        override fun isMappedToString(): Boolean = this@CAdapterTypeTranslator.isMappedToString(type)
        override fun isMappedToReference(): Boolean = this@CAdapterTypeTranslator.isMappedToReference(type)
    }
}
