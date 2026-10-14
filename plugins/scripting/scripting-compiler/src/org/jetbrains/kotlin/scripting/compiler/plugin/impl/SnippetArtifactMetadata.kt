/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * REPL-specific metadata for reconstructing REPL history from snippets compiled to classfiles
 *
 * Intended for transient use during a single REPL session when the stateless compilation of the snippet
 * (see `REPL_SNIPPET_STATELESS_MODE_OPTION`) is used. Produced by the compiler (scripting plugin) on compiling the snippet
 * and consumed by the compiler (scripting plugin) on compiling the subsequent snippets to restore the state of the REPL session.
 *
 * Versioning should provide compatibility for some peace of mind and error detection, but in general this artefact is not intended
 * for any long preservation or publishing.
 */
data class SnippetArtifactMetadata(
    val version: Int,
    val priorSnippetClassId: String?,
    val replSnippetDeclarations: List<MemberRef>,
    val imports: List<ImportEntry>,
    val serializedCompilationConfiguration: ByteArray?,
) {
    /**
     * Reference to a top-level member of the snippet wrapper class that had `isReplSnippetDeclaration == true` at compile time.
     */
    data class MemberRef(
        val kind: Kind,
        val name: String,
        val signature: String?,
        val visibility: Visibility = Visibility.UNKNOWN,
    ) {
        enum class Kind(val wireId: Int) { PROPERTY(1), FUNCTION(2), CLASS(3), TYPEALIAS(4) }
        enum class Visibility(val wireId: Int) { PUBLIC(1), INTERNAL(2), PROTECTED(3), PRIVATE(4), UNKNOWN(0) }
    }

    data class ImportEntry(
        val fqName: String,
        val isAllUnder: Boolean,
        val aliasName: String?,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnippetArtifactMetadata) return false
        return version == other.version &&
                priorSnippetClassId == other.priorSnippetClassId &&
                replSnippetDeclarations == other.replSnippetDeclarations &&
                imports == other.imports &&
                serializedCompilationConfiguration.contentEquals(other.serializedCompilationConfiguration)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + (priorSnippetClassId?.hashCode() ?: 0)
        result = 31 * result + replSnippetDeclarations.hashCode()
        result = 31 * result + imports.hashCode()
        result = 31 * result + serializedCompilationConfiguration.contentHashCode()
        return result
    }

    companion object {
        const val CURRENT_VERSION: Int = 1
        const val MIN_SUPPORTED_VERSION: Int = 1
    }
}

object SnippetArtifactMetadataCodec {

    fun encode(metadata: SnippetArtifactMetadata): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(metadata.version)
            // Immediately after the version, so that decodeChainLinkOnly can stop here
            out.writeNullableUTF(metadata.priorSnippetClassId)
            out.writeInt(metadata.replSnippetDeclarations.size)
            for (m in metadata.replSnippetDeclarations) {
                out.writeByte(m.kind.wireId)
                out.writeUTF(m.name)
                out.writeNullableUTF(m.signature)
                out.writeByte(m.visibility.wireId)
            }
            out.writeInt(metadata.imports.size)
            for (i in metadata.imports) {
                out.writeUTF(i.fqName)
                out.writeBoolean(i.isAllUnder)
                out.writeNullableUTF(i.aliasName)
            }
            out.writeNullableBytes(metadata.serializedCompilationConfiguration)
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): SnippetArtifactMetadata = DataInputStream(ByteArrayInputStream(bytes)).use { inp ->
        val version = inp.readVersion()
        val priorSnippetClassId = inp.readNullableUTF()
        val declarations = inp.readList {
            SnippetArtifactMetadata.MemberRef(
                kind = memberRefKind(readUnsignedByte()),
                name = readUTF(),
                signature = readNullableUTF(),
                visibility = visibility(readUnsignedByte()),
            )
        }
        val imports = inp.readList {
            SnippetArtifactMetadata.ImportEntry(
                fqName = readUTF(),
                isAllUnder = readBoolean(),
                aliasName = readNullableUTF(),
            )
        }
        SnippetArtifactMetadata(
            version = version,
            priorSnippetClassId = priorSnippetClassId,
            replSnippetDeclarations = declarations,
            imports = imports,
            serializedCompilationConfiguration = inp.readNullableBytes(),
        )
    }

    fun decodeChainLinkOnly(bytes: ByteArray): String? = DataInputStream(ByteArrayInputStream(bytes)).use { inp ->
        inp.readVersion()
        inp.readNullableUTF()
    }

    private fun DataInputStream.readVersion(): Int {
        val version = readInt()
        if (version < SnippetArtifactMetadata.MIN_SUPPORTED_VERSION || version > SnippetArtifactMetadata.CURRENT_VERSION) {
            error(
                "SnippetArtifactMetadata: metadataVersion=$version is outside the supported range " +
                        "[${SnippetArtifactMetadata.MIN_SUPPORTED_VERSION}..${SnippetArtifactMetadata.CURRENT_VERSION}]; " +
                        "rebuild the previous snippet with a matching compiler."
            )
        }
        return version
    }

    private fun memberRefKind(id: Int): SnippetArtifactMetadata.MemberRef.Kind =
        SnippetArtifactMetadata.MemberRef.Kind.entries.firstOrNull { it.wireId == id }
            ?: error("SnippetArtifactMetadata: unknown MemberRef.Kind id=$id")

    private fun visibility(id: Int): SnippetArtifactMetadata.MemberRef.Visibility =
        SnippetArtifactMetadata.MemberRef.Visibility.entries.firstOrNull { it.wireId == id }
            ?: SnippetArtifactMetadata.MemberRef.Visibility.UNKNOWN

    private fun DataOutputStream.writeNullableUTF(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readNullableUTF(): String? = if (readBoolean()) readUTF() else null

    private fun DataOutputStream.writeNullableBytes(value: ByteArray?) {
        writeInt(value?.size ?: -1)
        if (value != null) write(value)
    }

    private fun DataInputStream.readNullableBytes(): ByteArray? {
        val size = readInt()
        if (size < 0) return null
        if (size > available()) {
            error("SnippetArtifactMetadata: implausible byte count $size")
        }
        return ByteArray(size).also { readFully(it) }
    }

    private fun <T> DataInputStream.readList(readElement: DataInputStream.() -> T): List<T> {
        val size = readInt()
        if (size < 0 || size > available() + 1) {
            error("SnippetArtifactMetadata: implausible element count $size")
        }
        return List(size) { readElement() }
    }
}

internal const val REPL_SNIPPET_ARTIFACT_PLUGIN_ID: String = "org.jetbrains.kotlin.scripting.repl.stateless"


