/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * The REPL-only reconstruction payload of a compiled snippet — the information that is *not*
 * recoverable from stock `.kotlin_metadata` alone and which a stateless `FirReplHistoryProvider`
 * (`ClasspathBackedFirReplHistoryProvider`) needs to reconstruct a `FirReplSnippetSymbol` view of a
 * prior snippet during the next compile: the `isReplSnippetDeclaration` member refs (with their
 * source-level visibilities) and the file-level imports.
 *
 * This payload is carried **only** embedded inside the snippet wrapper class's own
 * `.kotlin_metadata` (via the generic `ProtoBuf.CompilerPluginData` channel, keyed by
 * [REPL_SIDECAR_PLUGIN_ID]). The read path (`ClasspathBackedFirReplHistoryProvider`) finds the
 * wrapper class via its `ClassId`, then reads this sidecar from the located class's metadata.
 *
 * The wire format is **forward- and backward-compatible**: protobuf field *numbers* are append-only
 * and never reused, unknown fields are skipped on read, and fields absent from an older payload fall
 * back to their defaults. Bumping [sidecarVersion] is still mandatory on any structural change (so a
 * payload can be identified), but a version mismatch is no longer fatal within the supported range
 * (see [SnippetArtifactSidecar.MIN_SUPPORTED_VERSION]). This makes a sidecar safe to persist and
 * read back with a differing compiler version.
 *
 * Everything in this file is `internal` to `scripting-compiler`. It is **not** part of any public
 * API surface (`libraries/scripting/common`).
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
     * Reference to a top-level member of the snippet wrapper class that originated as a
     * REPL-snippet declaration (i.e. carried `isReplSnippetDeclaration == true` at compile time).
     *
     * @property kind one of [Kind].
     * @property name the source-level name (`Name.identifier`).
     * @property descriptor an overload-discriminating signature for [Kind.FUNCTION] declarations
     *   (see `replMemberOverloadSignature`) -- functions are the only declaration kind that can
     *   share a name within one snippet. `null` for every other kind (whose name is already unique),
     *   for a function whose signature cannot be derived, and for sidecars produced before v5 (which
     *   never populated this field). The read path (`ClasspathBackedFirReplHistoryProvider`) uses it
     *   to pair each deserialised overload with the correct [MemberRef]; a `null` descriptor falls
     *   back to name-only matching. Despite the historical name, this is **not** a JVM descriptor.
     * @property visibility source-level visibility of the declaration as seen at compile time.
     *   Defaults to [Visibility.UNKNOWN] for compatibility with sidecars produced before v3.
     *   Consumers (e.g. `ClasspathBackedFirReplHistoryProvider`) use this to decide whether to
     *   re-tag the deserialised declaration as `isReplSnippetDeclaration` — private/protected
     *   members must not be exposed to subsequent snippets via REPL-history scoping (else the
     *   `property_visibility` diagnostic for cross-snippet private access is suppressed). See
     *   `iterations/2026-05-27_stateless-repl-sidecar-v3.md`.
     * @property returnTypeSignature for [Kind.PROPERTY] / [Kind.FUNCTION]: a *renderable* string
     *   representation of the declared return type (FQ name + nullability + projected arguments,
     *   as produced by `ConeKotlinType.toString()`). `null` for [Kind.CLASS] / [Kind.TYPEALIAS]
     *   (the type *is* the declaration), or when the return type cannot be derived (e.g. error
     *   type at compile time). Not consumed by `materialize()` today — the deserialised
     *   `.kotlin_metadata` already carries the real type — but recorded so that downstream
     *   tooling (debugger / IDE inspections / cross-snippet anonymous-object signature checks)
     *   can reason about prior-snippet return types without re-loading the wrapper class. This
     *   is the field that closes the schema-shaped gap behind `function_returns_anonymous_object`.
     */
    data class MemberRef(
        val kind: Kind,
        val name: String,
        val descriptor: String?,
        val visibility: Visibility = Visibility.UNKNOWN,
        val returnTypeSignature: String? = null,
    ) {
        enum class Kind { PROPERTY, FUNCTION, CLASS, TYPEALIAS }

        /**
         * Source-level visibility tag carried on a [MemberRef].
         *
         * Mirrors the subset of [org.jetbrains.kotlin.descriptors.Visibilities] that REPL
         * declarations can plausibly use; everything else (e.g. `LocalVisibility`,
         * `InvisibleFake`) maps to [UNKNOWN] on the producer side and is treated as PUBLIC by
         * the consumer (safe default — extra leakage on unrecognised visibilities is preferable
         * to dropping real declarations).
         */
        enum class Visibility { PUBLIC, INTERNAL, PROTECTED, PRIVATE, UNKNOWN }
    }

    /** A file-level `FirImport` entry of the snippet's containing file. */
    data class ImportEntry(
        val fqName: String,
        val isAllUnder: Boolean,
        val aliasName: String?,
    )

    companion object {
        /**
         * Bumped on every structural change to the sidecar.
         *
         * | Version | Change |
         * |---------|--------|
         * | 1       | Initial prototype shape. |
         * | 2       | Added [isImplicit] for Q10b history-provider tagging. |
         * | 3       | Added [MemberRef.visibility] + [MemberRef.returnTypeSignature]. Fixes the |
         * |         | `property_visibility` (private members must not be re-tagged as REPL- |
         * |         | snippet declarations on the consumer side) and unlocks the |
         * |         | `function_returns_anonymous_object` diagnostic by carrying the function |
         * |         | return type's renderable signature so cross-snippet anonymous return |
         * |         | types can be reasoned about. |
         * | 4       | Trimmed to the embedded-only reconstruction payload. Dropped the |
         * |         | config-only fields (`snippetName`, `snippetClassInternalName`, |
         * |         | `packageFqName`, `historyIndex`, `stateObjectFqName`, `resultPropertyName`, |
         * |         | `isSynthetic`, `isImplicit`) that a caller already knows out-of-band |
         * |         | (e.g. via the snippet's own `ClassId`). |
         * | 5       | Began populating [MemberRef.descriptor] with an overload-discriminating |
         * |         | signature (previously always `null`); the read side pairs function |
         * |         | overloads by it instead of by simple name. Decode became forward- and |
         * |         | backward-compatible (see [MIN_SUPPORTED_VERSION]) rather than exact-match. |
         */
        const val CURRENT_VERSION: Int = 5

        /**
         * The oldest [sidecarVersion] this codec still decodes. The format is forward- and
         * backward-compatible within `[MIN_SUPPORTED_VERSION, ∞)`: field numbers are append-only
         * and never reused, unknown fields are skipped on read, and fields absent from an older
         * payload fall back to their defaults. A version below this bound is rejected (a field it
         * carried has since been repurposed or dropped); a version *above* [CURRENT_VERSION] is
         * accepted and decoded best-effort (any not-yet-known fields are simply skipped).
         */
        const val MIN_SUPPORTED_VERSION: Int = 1
    }
}

/**
 * Hand-rolled **protobuf wire-format** writer/reader for [SnippetArtifactSidecar].
 *
 * This is the "protobuf sidecar cut" of the prototype's paired-JSON format. The bytes produced here
 * are a plain protobuf message (varints + length-delimited fields), written directly per the
 * protobuf spec rather than via a generated `.proto` message, so the codec stays self-contained
 * (no `.proto`-generation build wiring, no dependency on the relocated protobuf runtime's API) and
 * is unit-testable in isolation.
 *
 * These same bytes are what the stateless REPL embeds into the snippet wrapper class's
 * `.kotlin_metadata` via the generic `ProtoBuf.CompilerPluginData` channel (keyed by
 * [REPL_SIDECAR_PLUGIN_ID]) — so promoting the standalone blob to in-metadata storage is a
 * transport change, not a format change.
 *
 * ### Field schema (stable field numbers)
 *
 * Top-level `Sidecar`: `1` version (int32), `2` replSnippetDeclarations (repeated `MemberRef`),
 * `3` imports (repeated `ImportEntry`).
 *
 * `MemberRef`: `1` kind (int32), `2` name, `3` descriptor (optional), `4` visibility (int32),
 * `5` returnTypeSignature (optional).
 *
 * `ImportEntry`: `1` fqName, `2` isAllUnder (bool), `3` aliasName (optional).
 *
 * Unknown fields are skipped on read and absent fields fall back to their defaults, so the format is
 * forward- and backward-compatible across versions in `[SnippetArtifactSidecar.MIN_SUPPORTED_VERSION, ∞)`;
 * [SnippetArtifactSidecar.sidecarVersion] is validated only against that lower bound, not for an
 * exact match.
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
                        "version ${SnippetArtifactSidecar.MIN_SUPPORTED_VERSION}; rebuild the prior snippet " +
                        "with a current compiler."
            )
        }
        // Any version >= MIN_SUPPORTED_VERSION -- including one newer than CURRENT_VERSION -- is decoded
        // best-effort: field numbers are append-only, unknown fields were skipped above, and fields
        // absent from an older payload fall back to their defaults. No exact-version match is required.
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

    /** Minimal protobuf-wire reader over a [ByteArray]. */
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

/**
 * Plugin id under which the stateless REPL [SnippetArtifactSidecar] is embedded into the snippet
 * wrapper class's `.kotlin_metadata` (via the generic `ProtoBuf.CompilerPluginData` channel).
 */
const val REPL_SIDECAR_PLUGIN_ID: String = "org.jetbrains.kotlin.scripting.repl.stateless"


