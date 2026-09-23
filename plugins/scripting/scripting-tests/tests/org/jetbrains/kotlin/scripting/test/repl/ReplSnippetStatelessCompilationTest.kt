/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadata
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadataCodec
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ReplSnippetRegularPipelineTest {

    @Test
    fun testSidecarCodecRoundTrip() {
        val original = SnippetArtifactMetadata(
            version = SnippetArtifactMetadata.CURRENT_VERSION,
            priorSnippetClassId = "Snippet_0_repl",
            replSnippetDeclarations = listOf(
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.PROPERTY,
                    name = "x",
                    signature = "I",
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.PUBLIC,
                ),
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.FUNCTION,
                    name = "foo",
                    signature = null,
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.INTERNAL,
                ),
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.CLASS,
                    name = "Nested",
                    signature = null,
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.PROTECTED,
                ),
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.TYPEALIAS,
                    name = "Alias",
                    signature = null,
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.PRIVATE,
                ),
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.PROPERTY,
                    name = "unknownVisibility",
                    signature = null,
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.UNKNOWN,
                ),
            ),
            imports = listOf(
                SnippetArtifactMetadata.ImportEntry("kotlin.random.Random", isAllUnder = false, aliasName = null),
                SnippetArtifactMetadata.ImportEntry("java.util", isAllUnder = true, aliasName = "ju"),
            ),
            serializedCompilationConfiguration = byteArrayOf(1, 2, 3, 0, -7),
        )
        val bytes = SnippetArtifactMetadataCodec.encode(original)
        val decoded = SnippetArtifactMetadataCodec.decode(bytes)
        assertEquals(original, decoded, "sidecar must round-trip through the codec without loss")

        assertEquals(original.priorSnippetClassId, SnippetArtifactMetadataCodec.decodeChainLinkOnly(bytes))

        val noImports = original.copy(imports = emptyList())
        val decoded2 = SnippetArtifactMetadataCodec.decode(SnippetArtifactMetadataCodec.encode(noImports))
        assertEquals(noImports, decoded2)
        assertNotEquals(decoded, decoded2)

        val root = original.copy(priorSnippetClassId = null, serializedCompilationConfiguration = null)
        val rootBytes = SnippetArtifactMetadataCodec.encode(root)
        assertEquals(root, SnippetArtifactMetadataCodec.decode(rootBytes))
        assertNull(SnippetArtifactMetadataCodec.decodeChainLinkOnly(rootBytes))
    }

    @Test
    fun testSidecarVersionGating() {
        val member = SnippetArtifactMetadata.MemberRef(
            kind = SnippetArtifactMetadata.MemberRef.Kind.FUNCTION,
            name = "f",
            signature = "(kotlin/Int)",
            visibility = SnippetArtifactMetadata.MemberRef.Visibility.PUBLIC,
        )

        val older = SnippetArtifactMetadata(
            SnippetArtifactMetadata.MIN_SUPPORTED_VERSION, null, listOf(member), emptyList(), null
        )
        assertEquals(older, SnippetArtifactMetadataCodec.decode(SnippetArtifactMetadataCodec.encode(older)))

        for (unsupportedVersion in listOf(
            SnippetArtifactMetadata.MIN_SUPPORTED_VERSION - 1,
            SnippetArtifactMetadata.CURRENT_VERSION + 7,
        )) {
            val unsupported = SnippetArtifactMetadata(unsupportedVersion, null, listOf(member), emptyList(), null)
            val encoded = SnippetArtifactMetadataCodec.encode(unsupported)
            for (decode in listOf(SnippetArtifactMetadataCodec::decode, SnippetArtifactMetadataCodec::decodeChainLinkOnly)) {
                val ex = assertFailsWith<IllegalStateException> { decode(encoded) }
                assertEquals(true, ex.message?.contains("outside the supported range"), "unexpected error message: ${ex.message}")
            }
        }
    }

}
