/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.K1Deprecation

internal class CAdapterTypeTranslator(
        val prefix: String,
        @OptIn(K1Deprecation::class)
        val builtIns: KonanBuiltIns,
) {
    private fun translateTypeFull(type: KotlinType): Pair<String, String> =
            if (isMappedToVoid(type)) {
                "void" to "void"
            } else {
                translateNonVoidTypeFull(type)
            }

    internal fun isMappedToReference(type: KotlinType) =
            !isMappedToVoid(type) && !isMappedToString(type) &&
                    type.binaryTypeIsReference()

    fun isMappedToString(binaryType: BinaryType<ClassDescriptor>): Boolean =
            @OptIn(K1Deprecation::class)
            when (binaryType) {
                is BinaryType.Primitive -> false
                is BinaryType.Reference -> binaryType.types.first() == builtIns.string
            }

    fun isMappedToString(type: KotlinType): Boolean =
            isMappedToString(type.computeBinaryType())

    internal fun isMappedToVoid(type: KotlinType): Boolean {
        return type.isUnit() || type.isNothing()
    }

    fun translateType(type: KotlinType): String
            = translateTypeFull(type).first

    fun translateTypeBridge(type: KotlinType): String = translateTypeFull(type).second

    fun exportedType(type: KotlinType): CExportedType = CExportedTypeK1(type, this)

    private fun translateNonVoidTypeFull(type: KotlinType): Pair<String, String> = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ ->
                unsignedTypeMapping[inlinedClass.classId]?.let {
                    return it to it
                }
            },
            ifPrimitive = { primitiveType, _ ->
                primitiveTypeMapping[primitiveType]!!.let { it to it }
            },
            ifReference = {
                val clazz = (it.computeBinaryType() as BinaryType.Reference).types.first()
                @OptIn(K1Deprecation::class)
                if (clazz == builtIns.string) {
                    "const char*" to "KObjHeader*"
                } else {
                    CAdapterCAbi.krefTypeName(prefix, clazz.fqNameSafe.asString()) to "KObjHeader*"
                }
            }
    )

    private val primitiveTypeMapping = KonanPrimitiveType.entries.associate {
        it to CAdapterCAbi.primitiveCType(prefix, it)
    }

    private val unsignedTypeMapping = CAdapterCAbi.unsignedCTypesByClassId(prefix)
}

internal class CExportedTypeK1(
        private val type: KotlinType,
        private val typeTranslator: CAdapterTypeTranslator,
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