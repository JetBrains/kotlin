/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * The C-ABI spellings of the C export. It is shared by:
 *  - the type translator(s), which map exported parameter/return types to C (primitive/unsigned C types and the
 *    opaque "kref" reference naming)
 *  - the API renderer ([CAdapterApiExporter]), which emits the fixed runtime-support surface (the box/unbox
 *    service functions for the predefined types).
 */
internal object CAdapterCAbi {
    // C type of a Kotlin primitive, e.g. `Byte` -> `<prefix>_KByte`, native pointer -> `void*`.
    fun primitiveCType(prefix: String, type: KonanPrimitiveType): String = when (type) {
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

    // C type of a Kotlin unsigned type, e.g. `UByte` -> `<prefix>_KUByte`.
    fun unsignedCType(prefix: String, type: UnsignedType): String = when (type) {
        UnsignedType.UBYTE -> "${prefix}_KUByte"
        UnsignedType.USHORT -> "${prefix}_KUShort"
        UnsignedType.UINT -> "${prefix}_KUInt"
        UnsignedType.ULONG -> "${prefix}_KULong"
    }

    // Unsigned C types keyed by [ClassId], for looking up by an inlined class' id.
    fun unsignedCTypesByClassId(prefix: String): Map<ClassId, String> =
            UnsignedType.entries.associate { it.classId to unsignedCType(prefix, it) }

    // The opaque reference ("kref") C type for a Kotlin class fq-name, e.g. `<prefix>_kref_kotlin_String`.
    fun krefTypeName(prefix: String, fqName: String): String =
            "${prefix}_kref_${fqName.replace('.', '_')}"

    // A predefined (boxable) type exposed by the fixed runtime-support surface.
    class PredefinedType(
            // e.g. `"Byte"`, `"UByte"`, `"Unit"`; feeds `Kotlin_box<name>`, `createNullable<name>`, etc.
            val shortName: String,
            // The non-null C type; `"void"` for `Unit`.
            val cType: String,
            // The boxed/opaque C type, e.g. `<prefix>_kref_kotlin_Byte`.
            val nullableCType: String,
            val isUnit: Boolean,
    ) {
        // Service-function name that boxes a C value into the nullable Kotlin reference.
        val createNullableName: String get() = "createNullable$shortName"

        // Service-function name that unboxes the nullable Kotlin reference back to a C value.
        val getNonNullValueOfName: String get() = "getNonNullValueOf$shortName"
    }

    /**
     * The fixed set of predefined types (8 primitives + `Unit` + 4 unsigned), in the order the header has always
     * emitted them. These back the `createNullable*` / `getNonNullValueOf*` service functions and their typedefs.
     */
    fun predefinedTypes(prefix: String): List<PredefinedType> {
        // shortName (e.g. "Byte") and the kref fq-name (e.g. "kotlin.Byte") both come from the type's classId.
        fun predefined(classId: ClassId, cType: String, isUnit: Boolean) =
                PredefinedType(
                        shortName = classId.shortClassName.asString(),
                        cType = cType,
                        nullableCType = krefTypeName(prefix, classId.asSingleFqName().asString()),
                        isUnit = isUnit,
                )
        fun primitive(type: KonanPrimitiveType) = predefined(type.classId, primitiveCType(prefix, type), isUnit = false)
        fun unsigned(type: UnsignedType) = predefined(type.classId, unsignedCType(prefix, type), isUnit = false)
        return listOf(
                primitive(KonanPrimitiveType.BYTE),
                primitive(KonanPrimitiveType.SHORT),
                primitive(KonanPrimitiveType.INT),
                primitive(KonanPrimitiveType.LONG),
                primitive(KonanPrimitiveType.FLOAT),
                primitive(KonanPrimitiveType.DOUBLE),
                primitive(KonanPrimitiveType.CHAR),
                primitive(KonanPrimitiveType.BOOLEAN),
                predefined(StandardClassIds.Unit, "void", isUnit = true),
                unsigned(UnsignedType.UBYTE),
                unsigned(UnsignedType.USHORT),
                unsigned(UnsignedType.UINT),
                unsigned(UnsignedType.ULONG),
        )
    }
}
