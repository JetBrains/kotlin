/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * REPL-only reconstruction payload embedded into a compiled snippet wrapper class's
 * `.kotlin_metadata` via `ProtoBuf.CompilerPluginData` (keyed by [REPL_SIDECAR_PLUGIN_ID]): the
 * `isReplSnippetDeclaration` member refs and file-level imports that a stateless
 * `FirReplHistoryProvider` cannot recover from `.kotlin_metadata` alone.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

data class SnippetArtifactSidecar(
    val sidecarVersion: Int,
    val replSnippetDeclarations: List<MemberRef>,
    val imports: List<ImportEntry>,
) {
    /**
     * Reference to a top-level member of the snippet wrapper class that carried
     * `isReplSnippetDeclaration == true` at compile time.
     *
     * @property descriptor Despite the name, not a JVM descriptor but an overload-discriminating
     *   signature for [Kind.FUNCTION] (see [replMemberOverloadSignature]); `null` elsewhere, and a
     *   `null` falls back to name-only matching on read.
     */
    data class MemberRef(
        val kind: Kind,
        val name: String,
        val descriptor: String?,
        val visibility: Visibility = Visibility.UNKNOWN,
        val returnTypeSignature: String? = null,
    ) {
        enum class Kind { PROPERTY, FUNCTION, CLASS, TYPEALIAS }

        /** [UNKNOWN] is treated as PUBLIC by the consumer: safer to leak a declaration than to drop a real one. */
        enum class Visibility { PUBLIC, INTERNAL, PROTECTED, PRIVATE, UNKNOWN }
    }

    data class ImportEntry(
        val fqName: String,
        val isAllUnder: Boolean,
        val aliasName: String?,
    )

    companion object {
        /** Bumped on every structural change to the sidecar. */
        const val CURRENT_VERSION: Int = 5

        /** The oldest [sidecarVersion] this codec still decodes. */
        const val MIN_SUPPORTED_VERSION: Int = 1
    }
}

/**
 * Hand-rolled protobuf wire-format codec for [SnippetArtifactSidecar], kept dependency-free.
 *
 * Field numbers are append-only and never reused; unknown fields are skipped on read and absent
 * ones fall back to defaults, which keeps the format forward- and backward-compatible.
 */
object SnippetArtifactSidecarProtoCodec {

    private const val WIRETYPE_VARINT = 0
    private const val WIRETYPE_LEN = 2

    private fun memberRefKindId(kind: SnippetArtifactSidecar.MemberRef.Kind): Int = when (kind) {
        SnippetArtifactSidecar.MemberRef.Kind.PROPERTY -> 1
        SnippetArtifactSidecar.MemberRef.Kind.FUNCTION -> 2
        SnippetArtifactSidecar.MemberRef.Kind.CLASS -> 3
        SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS -> 4
    }

    private fun memberRefKind(id: Int): SnippetArtifactSidecar.MemberRef.Kind = when (id) {
        1 -> SnippetArtifactSidecar.MemberRef.Kind.PROPERTY
        2 -> SnippetArtifactSidecar.MemberRef.Kind.FUNCTION
        3 -> SnippetArtifactSidecar.MemberRef.Kind.CLASS
        4 -> SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS
        else -> error("SnippetArtifactSidecar: unknown MemberRef.Kind id=$id")
    }

    private fun visibilityId(v: SnippetArtifactSidecar.MemberRef.Visibility): Int = when (v) {
        SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN -> 0
        SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC -> 1
        SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL -> 2
        SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED -> 3
        SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE -> 4
    }

    private fun visibility(id: Int): SnippetArtifactSidecar.MemberRef.Visibility = when (id) {
        0 -> SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN
        1 -> SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC
        2 -> SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL
        3 -> SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED
        4 -> SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE
        else -> SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN
    }

    fun encode(sidecar: SnippetArtifactSidecar): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeInt32Field(1, sidecar.sidecarVersion)
        for (m in sidecar.replSnippetDeclarations) out.writeBytesField(2, encodeMemberRef(m))
        for (i in sidecar.imports) out.writeBytesField(3, encodeImport(i))
        return out.toByteArray()
    }

    private fun encodeMemberRef(m: SnippetArtifactSidecar.MemberRef): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeInt32Field(1, memberRefKindId(m.kind))
        out.writeStringField(2, m.name)
        m.descriptor?.let { out.writeStringField(3, it) }
        out.writeInt32Field(4, visibilityId(m.visibility))
        m.returnTypeSignature?.let { out.writeStringField(5, it) }
        return out.toByteArray()
    }

    private fun encodeImport(i: SnippetArtifactSidecar.ImportEntry): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeStringField(1, i.fqName)
        out.writeBoolField(2, i.isAllUnder)
        i.aliasName?.let { out.writeStringField(3, it) }
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): SnippetArtifactSidecar {
        val r = ProtoReader(bytes)
        var version = -1
        val declarations = ArrayList<SnippetArtifactSidecar.MemberRef>()
        val imports = ArrayList<SnippetArtifactSidecar.ImportEntry>()
        while (r.hasMore) {
            val tag = r.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            when (field) {
                1 -> version = r.readVarint().toInt()
                2 -> declarations += decodeMemberRef(r.readBytes())
                3 -> imports += decodeImport(r.readBytes())
                else -> r.skipField(wireType)
            }
        }
        if (version < 0) {
            error("SnippetArtifactSidecar: missing sidecarVersion field")
        }
        if (version < SnippetArtifactSidecar.MIN_SUPPORTED_VERSION) {
            error(
                "SnippetArtifactSidecar: sidecarVersion=$version is older than the minimum supported " +
                        "version ${SnippetArtifactSidecar.MIN_SUPPORTED_VERSION}; rebuild the previous snippet " +
                        "with a current compiler."
            )
        }
        // A version newer than CURRENT_VERSION is still decoded best-effort: unknown fields are skipped.
        return SnippetArtifactSidecar(
            sidecarVersion = version,
            replSnippetDeclarations = declarations,
            imports = imports,
        )
    }

    private fun decodeMemberRef(bytes: ByteArray): SnippetArtifactSidecar.MemberRef {
        val r = ProtoReader(bytes)
        var kindId = 0
        var name = ""
        var descriptor: String? = null
        var visibilityId = 0
        var returnTypeSignature: String? = null
        while (r.hasMore) {
            val tag = r.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            when (field) {
                1 -> kindId = r.readVarint().toInt()
                2 -> name = r.readString()
                3 -> descriptor = r.readString()
                4 -> visibilityId = r.readVarint().toInt()
                5 -> returnTypeSignature = r.readString()
                else -> r.skipField(wireType)
            }
        }
        return SnippetArtifactSidecar.MemberRef(
            kind = memberRefKind(kindId),
            name = name,
            descriptor = descriptor,
            visibility = visibility(visibilityId),
            returnTypeSignature = returnTypeSignature,
        )
    }

    private fun decodeImport(bytes: ByteArray): SnippetArtifactSidecar.ImportEntry {
        val r = ProtoReader(bytes)
        var fqName = ""
        var isAllUnder = false
        var aliasName: String? = null
        while (r.hasMore) {
            val tag = r.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            when (field) {
                1 -> fqName = r.readString()
                2 -> isAllUnder = r.readVarint() != 0L
                3 -> aliasName = r.readString()
                else -> r.skipField(wireType)
            }
        }
        return SnippetArtifactSidecar.ImportEntry(fqName = fqName, isAllUnder = isAllUnder, aliasName = aliasName)
    }

    private fun ByteArrayOutputStream.writeVarint(valueArg: Long) {
        var value = valueArg
        while (true) {
            val b = (value and 0x7F).toInt()
            value = value ushr 7
            if (value != 0L) write(b or 0x80) else { write(b); return }
        }
    }

    private fun ByteArrayOutputStream.writeTag(field: Int, wireType: Int) = writeVarint(((field shl 3) or wireType).toLong())

    private fun ByteArrayOutputStream.writeInt32Field(field: Int, value: Int) {
        writeTag(field, WIRETYPE_VARINT); writeVarint(value.toLong() and 0xFFFFFFFFL)
    }

    private fun ByteArrayOutputStream.writeBoolField(field: Int, value: Boolean) {
        writeTag(field, WIRETYPE_VARINT); writeVarint(if (value) 1L else 0L)
    }

    private fun ByteArrayOutputStream.writeStringField(field: Int, value: String) {
        writeBytesField(field, value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun ByteArrayOutputStream.writeBytesField(field: Int, value: ByteArray) {
        writeTag(field, WIRETYPE_LEN); writeVarint(value.size.toLong()); write(value)
    }

    private class ProtoReader(private val buf: ByteArray) {
        private var pos = 0
        val hasMore: Boolean get() = pos < buf.size

        fun readVarint(): Long {
            var shift = 0
            var result = 0L
            while (true) {
                if (pos >= buf.size) error("SnippetArtifactSidecar: truncated varint")
                val b = buf[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
        }

        fun readString(): String {
            val len = readVarint().toInt()
            val s = String(buf, pos, len, StandardCharsets.UTF_8)
            pos += len
            return s
        }

        fun readBytes(): ByteArray {
            val len = readVarint().toInt()
            val b = buf.copyOfRange(pos, pos + len)
            pos += len
            return b
        }

        fun skipField(wireType: Int) {
            when (wireType) {
                WIRETYPE_VARINT -> readVarint()
                WIRETYPE_LEN -> { val len = readVarint().toInt(); pos += len }
                1 -> pos += 8 // 64-bit
                5 -> pos += 4 // 32-bit
                else -> error("SnippetArtifactSidecar: unsupported wire type $wireType")
            }
        }
    }
}

/** Plugin id under which [SnippetArtifactSidecar] is embedded into the snippet's `.kotlin_metadata`. */
const val REPL_SIDECAR_PLUGIN_ID: String = "org.jetbrains.kotlin.scripting.repl.stateless"


