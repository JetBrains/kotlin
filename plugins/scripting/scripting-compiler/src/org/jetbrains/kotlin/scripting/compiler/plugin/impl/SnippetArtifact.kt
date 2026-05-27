/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Portable representation of a compiled REPL snippet for the **stateless K2 REPL compilation**
 * prototype (see `.junie/plans/k2-stateless-repl-prototype-step1.md`).
 *
 * A [SnippetArtifact] is the full per-snippet handoff between a stateless-compiler caller and the
 * compiler: the wrapper class plus its nested classes as raw `.class` bytes, paired with a small
 * JSON-encoded [SnippetArtifactSidecar] that carries information which is **not** preserved in
 * `.kotlin_metadata` — most notably the REPL-only `isReplSnippetDeclaration` markers, the snippet's
 * imports, and the REPL state-object FQ name.
 *
 * The paired-JSON layout is **only** for the prototype. The field set is unstable; once the field
 * set stabilises the sidecar will be promoted to a protobuf extension attached to the snippet
 * wrapper class's `.kotlin_metadata` (step 3 of the original proposal).
 *
 * Everything in this file is `internal` to `scripting-compiler`. It is **not** part of any public
 * API surface (`libraries/scripting/common`).
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.nio.charset.StandardCharsets

/**
 * A compiled REPL snippet packaged for stateless-compiler consumption.
 *
 * @property classFiles `.class` bytes of every JVM class emitted for this snippet. The key is the
 *   class file's relative path **without** the trailing `.class` (i.e. the JVM internal name, e.g.
 *   `"Snippet_1"` or `"some/pkg/Snippet_1$Nested"`).
 * @property sidecar JSON-encoded [SnippetArtifactSidecar]. See [SnippetArtifactJsonCodec].
 */
internal data class SnippetArtifact(
    val classFiles: Map<String, ByteArray>,
    val sidecar: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnippetArtifact) return false
        if (classFiles.keys != other.classFiles.keys) return false
        for ((k, v) in classFiles) {
            val o = other.classFiles[k] ?: return false
            if (!v.contentEquals(o)) return false
        }
        if (!sidecar.contentEquals(other.sidecar)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for ((k, v) in classFiles) {
            result = 31 * result + k.hashCode()
            result = 31 * result + v.contentHashCode()
        }
        result = 31 * result + sidecar.contentHashCode()
        return result
    }
}

/**
 * Information about a compiled snippet that is *not* recoverable from `.kotlin_metadata` alone and
 * which the stateless compiler needs to reconstruct a `FirReplSnippetSymbol` view of a prior
 * snippet during the next compile.
 *
 * Field set is unstable; bumping [sidecarVersion] is mandatory on any structural change.
 */
internal data class SnippetArtifactSidecar(
    val sidecarVersion: Int,
    val snippetName: String,
    /** JVM internal name of the wrapper class containing `$$eval`, e.g. `"Snippet_1"`. */
    val snippetClassInternalName: String,
    val packageFqName: String,
    val historyIndex: Int,
    val replSnippetDeclarations: List<MemberRef>,
    val imports: List<ImportEntry>,
    /** Fully-qualified name of the REPL state-object class (e.g. `kotlin.script.experimental.repl.ReplState`). */
    val stateObjectFqName: String,
    val resultPropertyName: String?,
    val isSynthetic: Boolean,
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
     */
    data class MemberRef(
        val kind: Kind,
        val name: String,
        val descriptor: String?,
    ) {
        enum class Kind { PROPERTY, FUNCTION, CLASS, TYPEALIAS }
    }

    /** A file-level `FirImport` entry of the snippet's containing file. */
    data class ImportEntry(
        val fqName: String,
        val isAllUnder: Boolean,
        val aliasName: String?,
    )

    companion object {
        const val CURRENT_VERSION: Int = 1
    }
}

/**
 * Hand-rolled JSON writer/reader for [SnippetArtifactSidecar].
 *
 * Hand-rolled rather than using `kotlinx.serialization` because:
 *  1. The sidecar is a throwaway prototype format that will be replaced by protobuf-in-metadata.
 *  2. Avoids pulling a serialization runtime into `scripting-compiler`.
 *
 * Supports only the strict subset of JSON that this format needs: objects, string arrays, strings,
 * integers, booleans, and `null`. No whitespace tolerance beyond what the writer emits.
 */
internal object SnippetArtifactJsonCodec {

    fun encode(sidecar: SnippetArtifactSidecar): ByteArray {
        val sb = StringBuilder()
        sb.append('{')
        sb.appendField("sidecarVersion", sidecar.sidecarVersion); sb.append(',')
        sb.appendField("snippetName", sidecar.snippetName); sb.append(',')
        sb.appendField("snippetClassInternalName", sidecar.snippetClassInternalName); sb.append(',')
        sb.appendField("packageFqName", sidecar.packageFqName); sb.append(',')
        sb.appendField("historyIndex", sidecar.historyIndex); sb.append(',')
        sb.appendArrayField("replSnippetDeclarations", sidecar.replSnippetDeclarations) { sb, m ->
            sb.append('{')
            sb.appendField("kind", m.kind.name); sb.append(',')
            sb.appendField("name", m.name); sb.append(',')
            sb.appendNullableField("descriptor", m.descriptor)
            sb.append('}')
        }
        sb.append(',')
        sb.appendArrayField("imports", sidecar.imports) { sb, i ->
            sb.append('{')
            sb.appendField("fqName", i.fqName); sb.append(',')
            sb.appendField("isAllUnder", i.isAllUnder); sb.append(',')
            sb.appendNullableField("aliasName", i.aliasName)
            sb.append('}')
        }
        sb.append(',')
        sb.appendField("stateObjectFqName", sidecar.stateObjectFqName); sb.append(',')
        sb.appendNullableField("resultPropertyName", sidecar.resultPropertyName); sb.append(',')
        sb.appendField("isSynthetic", sidecar.isSynthetic)
        sb.append('}')
        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SnippetArtifactSidecar {
        val parser = JsonParser(String(bytes, StandardCharsets.UTF_8))
        val obj = parser.parseObject()
        parser.skipWhitespace()
        if (!parser.eof) error("SnippetArtifactSidecar: trailing content at offset ${parser.pos}")
        val version = (obj["sidecarVersion"] as? Long)?.toInt()
            ?: error("SnippetArtifactSidecar: missing 'sidecarVersion'")
        if (version != SnippetArtifactSidecar.CURRENT_VERSION) {
            error(
                "SnippetArtifactSidecar: unsupported sidecarVersion=$version " +
                        "(expected ${SnippetArtifactSidecar.CURRENT_VERSION}). " +
                        "The paired-JSON sidecar format is unstable; rebuild prior snippets with the matching compiler version."
            )
        }
        @Suppress("UNCHECKED_CAST")
        return SnippetArtifactSidecar(
            sidecarVersion = version,
            snippetName = obj.req("snippetName") as String,
            snippetClassInternalName = obj.req("snippetClassInternalName") as String,
            packageFqName = obj.req("packageFqName") as String,
            historyIndex = (obj.req("historyIndex") as Long).toInt(),
            replSnippetDeclarations = (obj.req("replSnippetDeclarations") as List<Map<String, Any?>>).map { m ->
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.valueOf(m["kind"] as String),
                    name = m["name"] as String,
                    descriptor = m["descriptor"] as String?,
                )
            },
            imports = (obj.req("imports") as List<Map<String, Any?>>).map { m ->
                SnippetArtifactSidecar.ImportEntry(
                    fqName = m["fqName"] as String,
                    isAllUnder = m["isAllUnder"] as Boolean,
                    aliasName = m["aliasName"] as String?,
                )
            },
            stateObjectFqName = obj.req("stateObjectFqName") as String,
            resultPropertyName = obj["resultPropertyName"] as String?,
            isSynthetic = obj.req("isSynthetic") as Boolean,
        )
    }

    private fun Map<String, Any?>.req(key: String): Any =
        this[key] ?: error("SnippetArtifactSidecar: missing required field '$key'")

    private fun StringBuilder.appendField(name: String, value: String) {
        appendJsonString(name); append(':'); appendJsonString(value)
    }

    private fun StringBuilder.appendField(name: String, value: Int) {
        appendJsonString(name); append(':'); append(value)
    }

    private fun StringBuilder.appendField(name: String, value: Boolean) {
        appendJsonString(name); append(':'); append(if (value) "true" else "false")
    }

    private fun StringBuilder.appendNullableField(name: String, value: String?) {
        appendJsonString(name); append(':')
        if (value == null) append("null") else appendJsonString(value)
    }

    private inline fun <T> StringBuilder.appendArrayField(
        name: String,
        items: List<T>,
        writeItem: (StringBuilder, T) -> Unit,
    ) {
        appendJsonString(name); append(":[")
        for ((idx, item) in items.withIndex()) {
            if (idx > 0) append(',')
            writeItem(this, item)
        }
        append(']')
    }

    private fun StringBuilder.appendJsonString(s: String) {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (c.code < 0x20) {
                    append("\\u")
                    val hex = c.code.toString(16)
                    repeat(4 - hex.length) { append('0') }
                    append(hex)
                } else append(c)
            }
        }
        append('"')
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

/** Encode the given sidecar into a [SnippetArtifact] together with the supplied class files. */
internal fun SnippetArtifactSidecar.toArtifact(classFiles: Map<String, ByteArray>): SnippetArtifact =
    SnippetArtifact(classFiles, SnippetArtifactJsonCodec.encode(this))

/** Decode this artifact's sidecar back into a [SnippetArtifactSidecar]. */
internal fun SnippetArtifact.decodeSidecar(): SnippetArtifactSidecar =
    SnippetArtifactJsonCodec.decode(sidecar)
