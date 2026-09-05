/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.name.ClassId

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
        fun primitive(type: KonanPrimitiveType, shortName: String) =
                PredefinedType(shortName, primitiveCType(prefix, type), krefTypeName(prefix, "kotlin.$shortName"), isUnit = false)
        fun unsigned(type: UnsignedType, shortName: String) =
                PredefinedType(shortName, unsignedCType(prefix, type), krefTypeName(prefix, "kotlin.$shortName"), isUnit = false)
        return listOf(
                primitive(KonanPrimitiveType.BYTE, "Byte"),
                primitive(KonanPrimitiveType.SHORT, "Short"),
                primitive(KonanPrimitiveType.INT, "Int"),
                primitive(KonanPrimitiveType.LONG, "Long"),
                primitive(KonanPrimitiveType.FLOAT, "Float"),
                primitive(KonanPrimitiveType.DOUBLE, "Double"),
                primitive(KonanPrimitiveType.CHAR, "Char"),
                primitive(KonanPrimitiveType.BOOLEAN, "Boolean"),
                PredefinedType("Unit", "void", krefTypeName(prefix, "kotlin.Unit"), isUnit = true),
                unsigned(UnsignedType.UBYTE, "UByte"),
                unsigned(UnsignedType.USHORT, "UShort"),
                unsigned(UnsignedType.UINT, "UInt"),
                unsigned(UnsignedType.ULONG, "ULong"),
        )
    }
}
