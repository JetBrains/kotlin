/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class CAdapterTypeTranslator(
        val prefix: String,
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
            when (binaryType) {
                is BinaryType.Primitive -> false
                is BinaryType.Reference -> binaryType.types.first() == builtIns.string
            }

    fun isMappedToString(type: KotlinType): Boolean =
            isMappedToString(type.computeBinaryType())

    internal fun isMappedToVoid(type: KotlinType): Boolean {
        return type.isUnit() || type.isNothing()
    }

    fun translateType(element: SignatureElement): String =
            translateTypeFull(element.type).first

    fun translateType(type: KotlinType): String
            = translateTypeFull(type).first

    fun translateTypeBridge(type: KotlinType): String = translateTypeFull(type).second

    fun translateTypeFqName(name: String): String {
        return name.replace('.', '_')
    }

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
                if (clazz == builtIns.string) {
                    "const char*" to "KObjHeader*"
                } else {
                    "${prefix}_kref_${translateTypeFqName(clazz.fqNameSafe.asString())}" to "KObjHeader*"
                }
            }
    )

    private val primitiveTypeMapping = KonanPrimitiveType.values().associate {
        it to when (it) {
            KonanPrimitiveType.BOOLEAN -> "${prefix}_KBoolean"
            KonanPrimitiveType.CHAR -> "${prefix}_KChar"
            KonanPrimitiveType.BYTE -> "${prefix}_KByte"
            KonanPrimitiveType.SHORT -> "${prefix}_KShort"
            KonanPrimitiveType.INT -> "${prefix}_KInt"
            KonanPrimitiveType.LONG -> "${prefix}_KLong"
            KonanPrimitiveType.FLOAT -> "${prefix}_KFloat"
            KonanPrimitiveType.DOUBLE -> "${prefix}_KDouble"
            KonanPrimitiveType.NON_NULL_NATIVE_PTR -> "void*"
            KonanPrimitiveType.VECTOR128 -> "${prefix}_KVector128"
        }
    }

    private val unsignedTypeMapping = UnsignedType.values().associate {
        it.classId to when (it) {
            UnsignedType.UBYTE -> "${prefix}_KUByte"
            UnsignedType.USHORT -> "${prefix}_KUShort"
            UnsignedType.UINT -> "${prefix}_KUInt"
            UnsignedType.ULONG -> "${prefix}_KULong"
        }
    }
}