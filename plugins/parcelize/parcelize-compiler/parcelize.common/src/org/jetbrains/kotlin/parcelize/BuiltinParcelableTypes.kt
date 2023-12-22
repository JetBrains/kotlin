/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

/*
 * List of builtin Parcelable types.
 *
 * The Parcelize plugin has direct support for serializing all types in `PARCELABLE_BASE_TYPE_FQNAMES`,
 * as well as the container types in `PARCELABLE_CONTAINER_FQNAMES` whenever all element types can be serialized.
 *
 * Apart from that, Parcelize can serialize all types with supertypes in `PARCELABLE_SUPERTYPE_FQNAMES` through
 * reflection, as well as all objects, enums, and function types (since they are implicitly serializable).
 */
object BuiltinParcelableTypes {
    val IMMUTABLE_LIST_FQNAMES = setOf(
        kotlinxImmutable("PersistentList"),
        kotlinxImmutable("ImmutableList"),
    )

    val IMMUTABLE_SET_FQNAMES = setOf(
        kotlinxImmutable("PersistentSet"),
        kotlinxImmutable("ImmutableSet"),
    )

    val IMMUTABLE_MAP_FQNAMES = setOf(
        kotlinxImmutable("PersistentMap"),
        kotlinxImmutable("ImmutableMap"),
    )

    val IMMUTABLE_COLLECTIONS_FQNAMES = setOf(
        *IMMUTABLE_LIST_FQNAMES.toTypedArray(),
        *IMMUTABLE_SET_FQNAMES.toTypedArray(),
        *IMMUTABLE_MAP_FQNAMES.toTypedArray()
    )

    val PARCELABLE_SUPERTYPE_FQNAMES = setOf(
        "android.os.Parcelable",
        "android.os.IBinder",
        "java.io.Serializable"
    )

    val PARCELABLE_BASE_TYPE_FQNAMES = setOf(
        "android.os.Bundle",
        "android.os.PersistableBundle",
        "android.util.Size",
        "android.util.SizeF",
        "android.util.SparseBooleanArray",
        "android.util.SparseIntArray",
        "android.util.SparseLongArray",
        "java.io.FileDescriptor",
        "java.lang.Boolean",
        "java.lang.Byte",
        "java.lang.CharSequence",
        "java.lang.Character",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Short",
        "java.lang.String",
        "kotlin.Boolean",
        "kotlin.BooleanArray",
        "kotlin.Byte",
        "kotlin.ByteArray",
        "kotlin.Char",
        "kotlin.CharArray",
        "kotlin.CharSequence",
        "kotlin.Double",
        "kotlin.DoubleArray",
        "kotlin.Float",
        "kotlin.FloatArray",
        "kotlin.Int",
        "kotlin.IntArray",
        "kotlin.Long",
        "kotlin.LongArray",
        "kotlin.Short",
        "kotlin.String",
        "kotlin.UByte",
        "kotlin.UByteArray",
        "kotlin.UInt",
        "kotlin.UIntArray",
        "kotlin.ULong",
        "kotlin.ULongArray",
        "kotlin.UShort",
        "kotlin.UShortArray",
        "kotlin.time.Duration",
    ) + PARCELABLE_SUPERTYPE_FQNAMES

    val PARCELABLE_CONTAINER_FQNAMES = setOf(
        "android.util.SparseArray",
        "java.util.ArrayDeque",
        "java.util.ArrayList",
        "java.util.HashMap",
        "java.util.HashSet",
        "java.util.LinkedHashMap",
        "java.util.LinkedHashSet",
        "java.util.List",
        "java.util.Map",
        "java.util.NavigableMap",
        "java.util.NavigableSet",
        "java.util.Set",
        "java.util.SortedMap",
        "java.util.SortedSet",
        "java.util.TreeMap",
        "java.util.concurrent.ConcurrentHashMap",
        "kotlin.Array",
        "kotlin.collections.ArrayDeque",
        "kotlin.collections.ArrayList",
        "kotlin.collections.HashMap",
        "kotlin.collections.HashSet",
        "kotlin.collections.LinkedHashMap",
        "kotlin.collections.LinkedHashSet",
        "kotlin.collections.List",
        "kotlin.collections.Map",
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableMap",
        "kotlin.collections.MutableSet",
        "kotlin.collections.Set",
        *IMMUTABLE_COLLECTIONS_FQNAMES.toTypedArray(),
    )
}