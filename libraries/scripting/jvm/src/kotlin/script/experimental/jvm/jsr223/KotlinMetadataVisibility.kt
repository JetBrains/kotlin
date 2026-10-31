/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

/*
 * A minimal reader of the visibility of Kotlin classes from their `kotlin.Metadata` annotation. This module can't depend
 * on the compiler's metadata deserialization (`core/metadata.jvm`) nor on `kotlin-metadata-jvm`, without adding a
 * dependency to every user of the published artifact (and to the jars embedding it, e.g. `kotlin-main-kts`), so the
 * small part needed here is duplicated.
 */

private const val KOTLIN_METADATA_CLASS_KIND = 1

// `ProtoBuf.Visibility.PUBLIC`
private const val KOTLIN_METADATA_PUBLIC_VISIBILITY = 3

// `ProtoBuf.Class.flags` default: public final class
private const val KOTLIN_METADATA_DEFAULT_CLASS_FLAGS = 6

/**
 * Returns the visibility (a `ProtoBuf.Visibility` number) of this class as recorded in its Kotlin metadata, or null
 * if it is not a Kotlin class or its metadata can't be read. Only the `flags` of the `ProtoBuf.Class` message are decoded.
 */
private fun Class<*>.kotlinClassVisibilityOrNull(): Int? {
    val metadata = getAnnotation(Metadata::class.java) ?: return null
    if (metadata.kind != KOTLIN_METADATA_CLASS_KIND) return null
    return try {
        val bytes = decodeKotlinMetadataBytes(metadata.data1)
        var position = 0
        fun readVarint(): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = bytes[position++].toInt()
                result = result or ((b and 0x7f) shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
        }
        // `d1` starts with the length-delimited `JvmProtoBuf.StringTableTypes` message, followed by the `ProtoBuf.Class` one.
        val stringTableTypesLength = readVarint()
        position += stringTableTypesLength
        // `flags` is field 1 of `ProtoBuf.Class`, hence serialized first if present.
        val flags = if (position < bytes.size && readVarint() == (1 shl 3)) readVarint() else KOTLIN_METADATA_DEFAULT_CLASS_FLAGS
        // `Flags.VISIBILITY`: 3 bits after `Flags.HAS_ANNOTATIONS`
        (flags shr 1) and 7
    } catch (_: Exception) {
        null
    }
}

/**
 * Returns true if this is a Kotlin class whose Kotlin visibility is not public, e.g. an `internal` class, which is public
 * in the bytecode but can't be referenced from other modules, snippets included.
 */
internal fun Class<*>.hasNonPublicKotlinVisibility(): Boolean =
    kotlinClassVisibilityOrNull().let { it != null && it != KOTLIN_METADATA_PUBLIC_VISIBILITY }

/** Duplicates `BitEncoding.decodeBytes` from `core/metadata.jvm`. */
private fun decodeKotlinMetadataBytes(data: Array<String>): ByteArray {
    var strings = data
    if (strings.isNotEmpty() && strings[0].isNotEmpty()) {
        when (strings[0][0]) {
            '\u0000' -> {
                strings = strings.copyOf().also { it[0] = it[0].substring(1) }
                val joined = strings.joinToString("")
                return ByteArray(joined.length) { joined[it].code.toByte() }
            }
            '\uffff' -> strings = strings.copyOf().also { it[0] = it[0].substring(1) }
        }
    }
    val joined = strings.joinToString("")
    // Adding 0x7f modulo 256 is the inverse of the encoding step
    val encoded = ByteArray(joined.length) { (joined[it].code + 0x7f).toByte() }
    val result = ByteArray(7 * encoded.size / 8)
    var byteIndex = 0
    var bit = 0
    for (i in result.indices) {
        val firstPart = (encoded[byteIndex].toInt() and 0xff) ushr bit
        byteIndex++
        val secondPart = (encoded[byteIndex].toInt() and ((1 shl (bit + 1)) - 1)) shl (7 - bit)
        result[i] = (firstPart + secondPart).toByte()
        if (bit == 6) {
            byteIndex++
            bit = 0
        } else {
            bit++
        }
    }
    return result
}
