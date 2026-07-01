/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Portable representation of a compiled REPL snippet for the **stateless K2 REPL compilation**
 * prototype (see `.junie/plans/k2-stateless-repl-prototype-step1.md`).
 *
 * A [SnippetArtifact] is the full per-snippet handoff between a stateless-compiler caller and the
 * compiler: the wrapper class plus its nested classes as raw `.class` bytes, plus a small
 * [SnippetArtifactHeader] — a minimal out-of-band index.
 *
 * The REPL-only reconstruction data that is **not** preserved by stock `.kotlin_metadata` — the
 * `isReplSnippetDeclaration` markers, their source-level visibilities, and the file-level imports —
 * is **no longer** carried alongside the class files. After the "full cut" it lives **only** inside
 * the snippet wrapper class's own `.kotlin_metadata`, embedded via the generic
 * `ProtoBuf.CompilerPluginData` channel (keyed by [REPL_SIDECAR_PLUGIN_ID]) as a
 * [SnippetArtifactSidecar]. The [SnippetArtifactHeader] retains only what a consumer needs *before*
 * (or *without*) deserializing a class's metadata — see its docs.
 *
 * Everything in this file is `internal` to `scripting-compiler`. It is **not** part of any public
 * API surface (`libraries/scripting/common`).
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * A compiled REPL snippet packaged for stateless-compiler consumption.
 *
 * @property classFiles `.class` bytes of every JVM class emitted for this snippet. The key is the
 *   class file's relative path **without** the trailing `.class` (i.e. the JVM internal name, e.g.
 *   `"Snippet_1"` or `"some/pkg/Snippet_1$Nested"`).
 * @property header protobuf-wire-encoded [SnippetArtifactHeader] — the minimal out-of-band index.
 *   See [SnippetArtifactHeaderProtoCodec]. The bulky reconstruction payload is **not** here; it is
 *   embedded in [classFiles]' `.kotlin_metadata` (see the class doc).
 */
data class SnippetArtifact(
    val classFiles: Map<String, ByteArray>,
    val header: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnippetArtifact) return false
        if (classFiles.keys != other.classFiles.keys) return false
        for ([k, v] in classFiles) {
            val o = other.classFiles[k] ?: return false
            if (!v.contentEquals(o)) return false
        }
        if (!header.contentEquals(other.header)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for ([k, v] in classFiles) {
            result = 31 * result + k.hashCode()
            result = 31 * result + v.contentHashCode()
        }
        result = 31 * result + header.contentHashCode()
        return result
    }
}

/**
 * The REPL-only reconstruction payload of a compiled snippet — the information that is *not*
 * recoverable from stock `.kotlin_metadata` alone and which the stateless compiler needs to
 * reconstruct a `FirReplSnippetSymbol` view of a prior snippet during the next compile: the
 * `isReplSnippetDeclaration` member refs (with their source-level visibilities) and the file-level
 * imports.
 *
 * After the "full cut" this is carried **only** embedded inside the snippet wrapper class's own
 * `.kotlin_metadata` (via the generic `ProtoBuf.CompilerPluginData` channel, keyed by
 * [REPL_SIDECAR_PLUGIN_ID]); it is no longer duplicated in a standalone blob alongside the class
 * files. The read path (`ArtifactBackedFirReplHistoryProvider`) finds the wrapper class via the
 * out-of-band [SnippetArtifactHeader], then reads this sidecar from the located class's metadata.
 * Everything a consumer needs *before/without* deserializing the metadata (class id, snippet name,
 * state-object FQ name, emitted result-field name, `isImplicit`) lives on the [SnippetArtifactHeader]
 * instead — so the sidecar carries only the two fields the read path sources from the embedded copy.
 *
 * Field set is unstable; bumping [sidecarVersion] is mandatory on any structural change.
 */
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
     * @property descriptor JVM descriptor for [Kind.PROPERTY] (field descriptor) / [Kind.FUNCTION]
     *   (method descriptor); JVM internal name for [Kind.CLASS] / [Kind.TYPEALIAS]. May be `null`
     *   when the descriptor cannot be derived in the prototype (e.g. type aliases).
     * @property visibility source-level visibility of the declaration as seen at compile time.
     *   Defaults to [Visibility.UNKNOWN] for compatibility with sidecars produced before v3.
     *   Consumers (e.g. `ArtifactBackedFirReplHistoryProvider`) use this to decide whether to
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
         * |         | config-only / header-duplicated fields (`snippetName`, |
         * |         | `snippetClassInternalName`, `packageFqName`, `historyIndex`, |
         * |         | `stateObjectFqName`, `resultPropertyName`, `isSynthetic`, `isImplicit`) |
         * |         | after the "full cut": they are unused on the embedded copy — every fact |
         * |         | a consumer needs out-of-band is read from the [SnippetArtifactHeader]. |
         */
        const val CURRENT_VERSION: Int = 4
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
 * Unknown fields are skipped on read (forward-compatible), but [SnippetArtifactSidecar.sidecarVersion]
 * is still validated for an exact match — the field set is not yet declared stable.
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
        if (version != SnippetArtifactSidecar.CURRENT_VERSION) {
            error(
                "SnippetArtifactSidecar: unsupported sidecarVersion=$version " +
                        "(expected ${SnippetArtifactSidecar.CURRENT_VERSION}). " +
                        "The sidecar field set is unstable; rebuild prior snippets with the matching compiler version."
            )
        }
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
 * Minimal JSON parser. Only supports the value types the sidecar uses: object, array, string,
 * integer (as `Long`), boolean, `null`.
 */
private class JsonParser(private val src: String) {
    var pos: Int = 0
        private set

    val eof: Boolean get() = pos >= src.length

    fun skipWhitespace() {
        while (pos < src.length) {
            val c = src[pos]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++ else return
        }
    }

    fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) error("unexpected EOF at $pos")
        return when (src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }

    fun parseObject(): Map<String, Any?> {
        skipWhitespace()
        expect('{')
        val result = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (peek() == '}') { pos++; return result }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            result[key] = value
            skipWhitespace()
            when (val c = peek()) {
                ',' -> { pos++; continue }
                '}' -> { pos++; return result }
                else -> error("expected ',' or '}' at $pos, got '$c'")
            }
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val result = ArrayList<Any?>()
        skipWhitespace()
        if (peek() == ']') { pos++; return result }
        while (true) {
            result.add(parseValue())
            skipWhitespace()
            when (val c = peek()) {
                ',' -> { pos++; continue }
                ']' -> { pos++; return result }
                else -> error("expected ',' or ']' at $pos, got '$c'")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            when (val c = src[pos++]) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= src.length) error("unterminated escape at $pos")
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            if (pos + 4 > src.length) error("truncated \\u escape at $pos")
                            sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                        else -> error("unsupported escape '\\$esc' at ${pos - 1}")
                    }
                }
                else -> sb.append(c)
            }
        }
        error("unterminated string starting before $pos")
    }

    private fun parseBoolean(): Boolean {
        return when {
            src.startsWith("true", pos) -> { pos += 4; true }
            src.startsWith("false", pos) -> { pos += 5; false }
            else -> error("expected boolean at $pos")
        }
    }

    private fun parseNull(): Any? {
        if (src.startsWith("null", pos)) { pos += 4; return null }
        error("expected null at $pos")
    }

    private fun parseNumber(): Long {
        val start = pos
        if (peek() == '-') pos++
        while (pos < src.length && src[pos].isDigit()) pos++
        if (pos == start) error("expected number at $start")
        return src.substring(start, pos).toLong()
    }

    private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'

    private fun expect(c: Char) {
        if (pos >= src.length || src[pos] != c) error("expected '$c' at $pos, got '${peek()}'")
        pos++
    }
}

/**
 * Minimal out-of-band index carried alongside a stateless REPL snippet's class files in a
 * [SnippetArtifact].
 *
 * After the "full cut" of the standalone [SnippetArtifactSidecar] blob, this header holds **only**
 * the facts a consumer needs *without* deserializing a class's `.kotlin_metadata`:
 *
 *  * [snippetClassInternalName] + [packageFqName] — the wrapper class id. The read path
 *    (`ArtifactBackedFirReplHistoryProvider`) must *find* the wrapper class before it can read the
 *    embedded [SnippetArtifactSidecar] from that class's metadata, and the reflection-based
 *    [SnippetArtifactEvaluator] uses it to load the class to run.
 *  * [snippetName] — used to name the reconstructed `FirReplSnippet` and in diagnostics.
 *  * [stateObjectFqName] — the REPL state-object FQ name, validated by `K2ReplStatelessCompiler`
 *    *before* any class is compiled or loaded (so it cannot be sourced from metadata).
 *  * [resultPropertyName] — the actual emitted result-field name (e.g. `res2`), read reflectively
 *    by [SnippetArtifactEvaluator]; it is only known post-codegen, and the evaluator does not
 *    deserialize `.kotlin_metadata`.
 *  * [isImplicit] — the Q10b history-provider flag, exposed (via `implicitFlags`) *before*
 *    `materialize()` runs, i.e. before any wrapper class is located.
 *
 * Everything else the read path needs — the `isReplSnippetDeclaration` member refs with their
 * visibilities and the file-level imports — is read from the [SnippetArtifactSidecar] embedded in
 * the wrapper class's `.kotlin_metadata`, never from here.
 */
data class SnippetArtifactHeader(
    val headerVersion: Int,
    val snippetName: String,
    /** JVM internal name of the wrapper class containing `$$eval`, e.g. `"Snippet_1"`. */
    val snippetClassInternalName: String,
    val packageFqName: String,
    /** Fully-qualified name of the REPL state-object class, or empty if generated from defaults. */
    val stateObjectFqName: String,
    /** Actual emitted JVM result-field name (e.g. `res2`), or `null` for a declaration-only snippet. */
    val resultPropertyName: String?,
    /** `true` when this snippet was implicitly prepended (see [SnippetArtifactSidecar.isImplicit]). */
    val isImplicit: Boolean,
) {
    companion object {
        /** Bumped on every structural change to the header. */
        const val CURRENT_VERSION: Int = 1
    }
}

/**
 * Hand-rolled **protobuf wire-format** writer/reader for [SnippetArtifactHeader] — the same
 * self-contained approach as [SnippetArtifactSidecarProtoCodec] (no `.proto`-generation wiring, no
 * dependency on the relocated protobuf runtime's API).
 *
 * ### Field schema (stable field numbers)
 *
 * `1` headerVersion (int32), `2` snippetName, `3` snippetClassInternalName, `4` packageFqName,
 * `5` stateObjectFqName, `6` resultPropertyName (optional), `7` isImplicit (bool).
 *
 * Unknown fields are skipped on read (forward-compatible); [SnippetArtifactHeader.headerVersion] is
 * validated for an exact match.
 */
object SnippetArtifactHeaderProtoCodec {

    private const val WIRETYPE_VARINT = 0
    private const val WIRETYPE_LEN = 2

    fun encode(header: SnippetArtifactHeader): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeInt32Field(1, header.headerVersion)
        out.writeStringField(2, header.snippetName)
        out.writeStringField(3, header.snippetClassInternalName)
        out.writeStringField(4, header.packageFqName)
        out.writeStringField(5, header.stateObjectFqName)
        header.resultPropertyName?.let { out.writeStringField(6, it) }
        out.writeBoolField(7, header.isImplicit)
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): SnippetArtifactHeader {
        val r = ProtoReader(bytes)
        var version = -1
        var snippetName = ""
        var snippetClassInternalName = ""
        var packageFqName = ""
        var stateObjectFqName = ""
        var resultPropertyName: String? = null
        var isImplicit = false
        while (r.hasMore) {
            val tag = r.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            when (field) {
                1 -> version = r.readVarint().toInt()
                2 -> snippetName = r.readString()
                3 -> snippetClassInternalName = r.readString()
                4 -> packageFqName = r.readString()
                5 -> stateObjectFqName = r.readString()
                6 -> resultPropertyName = r.readString()
                7 -> isImplicit = r.readVarint() != 0L
                else -> r.skipField(wireType)
            }
        }
        if (version != SnippetArtifactHeader.CURRENT_VERSION) {
            error(
                "SnippetArtifactHeader: unsupported headerVersion=$version " +
                        "(expected ${SnippetArtifactHeader.CURRENT_VERSION}). " +
                        "The header field set is unstable; rebuild prior snippets with the matching compiler version."
            )
        }
        return SnippetArtifactHeader(
            headerVersion = version,
            snippetName = snippetName,
            snippetClassInternalName = snippetClassInternalName,
            packageFqName = packageFqName,
            stateObjectFqName = stateObjectFqName,
            resultPropertyName = resultPropertyName,
            isImplicit = isImplicit,
        )
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
        writeTag(field, WIRETYPE_LEN)
        val b = value.toByteArray(StandardCharsets.UTF_8)
        writeVarint(b.size.toLong()); write(b)
    }

    /** Minimal protobuf-wire reader over a [ByteArray]. */
    private class ProtoReader(private val buf: ByteArray) {
        private var pos = 0
        val hasMore: Boolean get() = pos < buf.size

        fun readVarint(): Long {
            var shift = 0
            var result = 0L
            while (true) {
                if (pos >= buf.size) error("SnippetArtifactHeader: truncated varint")
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

        fun skipField(wireType: Int) {
            when (wireType) {
                WIRETYPE_VARINT -> readVarint()
                WIRETYPE_LEN -> { val len = readVarint().toInt(); pos += len }
                1 -> pos += 8
                5 -> pos += 4
                else -> error("SnippetArtifactHeader: unsupported wire type $wireType")
            }
        }
    }
}

/**
 * Plugin id under which the stateless REPL [SnippetArtifactSidecar] is embedded into the snippet
 * wrapper class's `.kotlin_metadata` (via the generic `ProtoBuf.CompilerPluginData` channel). After
 * the "full cut" this is the **sole** carrier of the reconstruction payload; the [SnippetArtifact]
 * itself carries only the out-of-band [SnippetArtifactHeader].
 */
const val REPL_SIDECAR_PLUGIN_ID: String = "org.jetbrains.kotlin.scripting.repl.stateless"

/** Encode the given header into a [SnippetArtifact] together with the supplied class files. */
fun SnippetArtifactHeader.toArtifact(classFiles: Map<String, ByteArray>): SnippetArtifact =
    SnippetArtifact(classFiles, SnippetArtifactHeaderProtoCodec.encode(this))

/** Decode this artifact's out-of-band [SnippetArtifactHeader]. */
fun SnippetArtifact.decodeHeader(): SnippetArtifactHeader =
    SnippetArtifactHeaderProtoCodec.decode(header)

/**
 * Wire codec for a complete [SnippetArtifact] (out-of-band [header][SnippetArtifactHeader] **plus**
 * class files), suitable for transport across an out-of-process boundary — most notably the Build
 * Tools API `CompileReplSnippetOperation` (see `iterations/2026-05-28c_stateless-repl-bta-transport.md`).
 *
 * The format is a single root JSON document — the simplest envelope that makes the
 * header+payload pair self-delimited:
 *
 * ```json
 * {
 *   "artifactVersion": 2,
 *   "header": "<base64 of SnippetArtifactHeaderProtoCodec.encode(header)>",
 *   "classFiles": { "ClassName1": "<base64 of bytes>", "ClassName2": "<base64>" }
 * }
 * ```
 *
 * Design notes (answers Q5d's "envelope/framing" sub-question for the prototype):
 *
 *  * **Single self-delimited root.** Each artifact is one JSON object — easy to length-prefix in
 *    any outer transport, or to drop into a `byte[]` BTA op parameter unchanged. No multi-part
 *    framing, no resource handles, no filesystem dependency.
 *  * **Opaque payloads.** Both the header (which is *itself* a protobuf message, but treated here
 *    as an opaque blob) and the class-file bytes are base64-encoded. The bulky reconstruction
 *    payload no longer travels here at all — it rides inside the class files' `.kotlin_metadata`.
 *  * **Versioned.** [ARTIFACT_VERSION] is the envelope-layout version, **separate** from
 *    [SnippetArtifactHeader.CURRENT_VERSION] (the header field set). Bumping the envelope
 *    version covers envelope-shape changes (framing/keys), not field additions inside the
 *    header. Bumped to `2` by the "full cut" (the `sidecar` key became `header`).
 *
 * This codec is **prototype-only** alongside [SnippetArtifactHeaderProtoCodec]. A future hardening
 * could switch the BTA op to a length-delimited binary envelope (`[ver][header-len][header]
 * [n-files][per-file: name-len, name, body-len, body]` or a protobuf root message). The single-root
 * choice here keeps the same envelope semantics: one self-delimited message per artifact.
 */
object SnippetArtifactCodec {

    const val ARTIFACT_VERSION: Int = 2

    fun encode(artifact: SnippetArtifact): ByteArray {
        val b64 = java.util.Base64.getEncoder()
        val sb = StringBuilder()
        sb.append('{')
        appendFieldKey(sb, "artifactVersion"); sb.append(ARTIFACT_VERSION); sb.append(',')
        appendFieldKey(sb, "header"); appendJsonString(sb, b64.encodeToString(artifact.header)); sb.append(',')
        appendFieldKey(sb, "classFiles"); sb.append('{')
        var first = true
        // Sort keys so the encoded bytes are deterministic — useful for digests, debugging diffs,
        // and roundtrip equality assertions that compare ByteArrays directly.
        for (key in artifact.classFiles.keys.sorted()) {
            if (!first) sb.append(',') else first = false
            appendJsonString(sb, key); sb.append(':')
            appendJsonString(sb, b64.encodeToString(artifact.classFiles.getValue(key)))
        }
        sb.append('}').append('}')
        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SnippetArtifact {
        val parser = JsonParser(String(bytes, StandardCharsets.UTF_8))
        val obj = parser.parseObject()
        parser.skipWhitespace()
        if (!parser.eof) error("SnippetArtifactCodec: trailing content at offset ${parser.pos}")
        val version = (obj["artifactVersion"] as? Long)?.toInt()
            ?: error("SnippetArtifactCodec: missing 'artifactVersion'")
        if (version != ARTIFACT_VERSION) {
            error(
                "SnippetArtifactCodec: unsupported artifactVersion=$version " +
                        "(expected $ARTIFACT_VERSION). The artifact wire envelope is unstable; " +
                        "rebuild prior snippets with the matching compiler version."
            )
        }
        val b64 = java.util.Base64.getDecoder()
        val headerBase64 = obj["header"] as? String
            ?: error("SnippetArtifactCodec: missing 'header' field")
        val headerBytes = b64.decode(headerBase64)
        @Suppress("UNCHECKED_CAST")
        val classFilesRaw = obj["classFiles"] as? Map<String, Any?>
            ?: error("SnippetArtifactCodec: missing 'classFiles' field")
        val classFiles = LinkedHashMap<String, ByteArray>(classFilesRaw.size)
        for ([k, v] in classFilesRaw) {
            val s = v as? String ?: error("SnippetArtifactCodec: classFiles['$k'] is not a string")
            classFiles[k] = b64.decode(s)
        }
        return SnippetArtifact(classFiles, headerBytes)
    }

    private fun appendFieldKey(sb: StringBuilder, name: String) {
        appendJsonString(sb, name); sb.append(':')
    }

    private fun appendJsonString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) {
                    sb.append("\\u")
                    val hex = c.code.toString(16)
                    repeat(4 - hex.length) { sb.append('0') }
                    sb.append(hex)
                } else sb.append(c)
            }
        }
        sb.append('"')
    }
}
