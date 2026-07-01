/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.scripting.compiler.plugin.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplStatelessCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactHeader
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactHeaderProtoCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeHeader
import org.jetbrains.kotlin.scripting.compiler.plugin.services.replStateObjectFqName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * End-to-end test for the **stateless K2 REPL compiler** prototype
 * (`K2ReplStatelessCompiler`).
 *
 * The happy-path scenario forces every Q5a reconstruction concern to surface
 * together — successful compilation of `x + 1` against a prior artifact for
 * `val x = 42` requires:
 *
 *  * `getClassLikeSymbolByClassId` to find the deserialized snippet-1 wrapper class,
 *  * declaration tagging (`isReplSnippetDeclaration` + `originalReplSnippetSymbol`)
 *    on the deserialized FIR declarations to take effect before the resolve extension walks them,
 *  * `FirReplSnippetResolveExtensionImpl.getSnippetScope` to surface `x`,
 *  * `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet` /
 *    `getFir2IrLazyClass` to produce a usable `IrClass` parent for the
 *    `REPL_FROM_OTHER_SNIPPET` synthesised declarations,
 *  * JVM codegen to complete.
 *
 * Any failure in the chain manifests as either a `ResultWithDiagnostics.Failure`
 * or an exception out of `K2ReplStatelessCompiler.compile`.
 */
class K2ReplStatelessCompilerTest {

    private val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
            System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true

    @Test
    fun testStatelessReplCompilesSnippetAgainstPriorArtifact() {
        if (!isK2) return

        val compiler = K2ReplStatelessCompiler()

        // 1. Compile snippet 1 (empty priors): `val x = 42`.
        val artifact1 = compileStateless(
            compiler,
            priorSnippets = emptyList(),
            source = "val x = 42",
            name = "s1.repl.kts",
        ).valueOrThrowExplained("snippet 1 compile failed")

        val header1 = artifact1.decodeHeader()
        assertTrue(artifact1.classFiles.isNotEmpty(), "snippet 1 must emit at least one .class file")
        assertTrue(
            header1.snippetClassInternalName.isNotEmpty(),
            "snippet 1 header must record the wrapper class internal name"
        )
        // Note: `x` being a recognised repl declaration is proven by snippet 2 resolving it below —
        // after the "full cut" the declaration list is no longer carried in the artifact header; it
        // lives only in the wrapper class's embedded `.kotlin_metadata`.

        // 2. Compile snippet 2 against [artifact1]: `x + 1`.
        //    Successful compile = the entire stateless reconstruction chain worked.
        val artifact2 = compileStateless(
            compiler,
            priorSnippets = listOf(artifact1),
            source = "x + 1",
            name = "s2.repl.kts",
        ).valueOrThrowExplained("stateless snippet 2 compile failed (cross-snippet resolution likely broken)")

        val header2 = artifact2.decodeHeader()
        assertTrue(artifact2.classFiles.isNotEmpty(), "snippet 2 must emit at least one .class file")
        // Snippet wrapper class names embed the source name; assert at least one classfile mentions `s2`.
        val classKeys = artifact2.classFiles.keys
        assertTrue(
            classKeys.any { it.contains("s2", ignoreCase = true) || it.contains("S2", ignoreCase = true) },
            "snippet 2 classfiles should encode the source name `s2`; got keys: $classKeys"
        )
        // Snippet 2 wrapper class internal name from the header should be present among the classfile keys.
        assertTrue(
            header2.snippetClassInternalName in classKeys ||
                    classKeys.any { it.endsWith("/${header2.snippetClassInternalName.substringAfterLast('/')}") },
            "snippet 2 wrapper class `${header2.snippetClassInternalName}` must be among classfile keys $classKeys"
        )
    }

    /**
     * Falsifiable proof that, after the **full cut**, the reconstruction payload (declarations +
     * imports) is carried **only** inside the wrapper class's `.kotlin_metadata` — never in the
     * artifact's out-of-band header — and that it survives the [SnippetArtifactCodec] wire envelope.
     *
     * Snippet 1 (`val x = 42`) is encoded to wire bytes and decoded back. The decoded artifact's
     * header has no declaration list (the [SnippetArtifactHeader] type physically has no such
     * field), so the declaration `x` is recoverable *only* from the embedded `.kotlin_metadata` that
     * rode inside the class-file payload. Compiling snippet 2 (`x + 1`) against the round-tripped
     * prior can only resolve `x` if reconstruction read the declaration list from that embedded copy
     * — proving the cut end-to-end. Were the embedded sidecar missing or ignored, `x` would be left
     * untagged and snippet 2 would fail with an unresolved reference.
     */
    @Test
    fun testStatelessReplReconstructsDeclarationsFromEmbeddedMetadataAcrossWire() {
        if (!isK2) return

        val compiler = K2ReplStatelessCompiler()

        // 1. Compile snippet 1 and round-trip the whole artifact (header + class files) through the
        //    wire envelope, exactly as an out-of-process BTA caller would.
        val artifact1 = compileStateless(compiler, emptyList(), "val x = 42", "s1.repl.kts")
            .valueOrThrowExplained("snippet 1 compile failed")
        val artifact1Wire = SnippetArtifactCodec.decode(SnippetArtifactCodec.encode(artifact1))

        // 2. The out-of-band header still travels, but it carries no declaration list (compile-time
        //    guarantee — the type has no such field). The declaration `x` lives only in the embedded
        //    `.kotlin_metadata` inside the class files.
        assertTrue(
            artifact1Wire.header.isNotEmpty(),
            "the wire-encoded artifact must still carry a (minimal) out-of-band header"
        )

        // 3. Compile snippet 2 against the round-tripped prior. Success ⇒ `x` was tagged from the
        //    embedded `.kotlin_metadata` sidecar that survived the wire envelope, proving the cut.
        compileStateless(compiler, listOf(artifact1Wire), "x + 1", "s2.repl.kts")
            .valueOrThrowExplained(
                "snippet 2 failed to resolve `x` after a wire round-trip — reconstruction must source " +
                        "declarations from the embedded `.kotlin_metadata` sidecar, not the artifact header"
            )
    }

    @Test
    fun testStatelessReplExecutesMultiSnippetSequence() {
        if (!isK2) return

        val compiler = K2ReplStatelessCompiler()

        // Compile a 3-snippet session, threading each produced artifact into the next compile as a
        // prior snippet — exactly as a real stateless host would.
        //   s1: `val x = 42`
        //   s2: `val y = x + 1`   (cross-snippet declaration reference)
        //   s3: `x + y`           (cross-snippet expression — produces a result field)
        val a1 = compileStateless(compiler, emptyList(), "val x = 42", "s1.repl.kts")
            .valueOrThrowExplained("snippet 1 compile failed")
        val a2 = compileStateless(compiler, listOf(a1), "val y = x + 1", "s2.repl.kts")
            .valueOrThrowExplained("snippet 2 compile failed")
        val a3 = compileStateless(compiler, listOf(a1, a2), "x + y", "s3.repl.kts")
            .valueOrThrowExplained("snippet 3 compile failed")

        // Replay the whole session: materialise every snippet's class bytes onto one in-memory
        // classloader and invoke each `$$eval` in history order. A correct run proves the artifacts
        // are *runnable*, not merely diagnostically equivalent — and that cross-snippet state
        // (`x`, `y`) actually propagates at runtime.
        val evalResult = SnippetArtifactEvaluator().evaluate(listOf(a1, a2, a3))

        assertEquals(3, evalResult.snippetInstances.size, "all three snippets must have been instantiated + run")

        // s1 introduced `x = 42`; reading its backing field on the snippet-1 instance must show 42.
        assertEquals(42, evalResult.readDeclaredField(0, "x"), "snippet 1 `x` must hold 42 after eval")

        // s2 introduced `y = x + 1`; the cross-snippet read of `x` must have resolved to 42 at
        // runtime, yielding `y == 43`.
        assertEquals(43, evalResult.readDeclaredField(1, "y"), "snippet 2 `y` must hold x+1 == 43 after eval")

        // s3 is an expression `x + y`; its value is captured in the REPL result field named by the
        // header's `resultPropertyName`. For a REPL snippet that is `res<snippetId>` (e.g. `res2`,
        // the `resultFieldPrefix`+id form), not the `$$result` config default — the header must
        // therefore record the *emitted* field name for the value to be readable. 42 + 43 == 85
        // proves both prior snippets contributed at runtime.
        val resultFieldName = assertNotNull(
            evalResult.resultFieldName,
            "expression snippet must record a result-field name in its header"
        )
        assertTrue(
            resultFieldName.startsWith("res"),
            "REPL result field should be a `res<id>` field, was `$resultFieldName`"
        )
        assertEquals(85, evalResult.lastResultValue, "snippet 3 expression result `x + y` must be 85")
    }

    @Test
    fun testSidecarProtoRoundtrip() {
        val original = SnippetArtifactSidecar(
            sidecarVersion = SnippetArtifactSidecar.CURRENT_VERSION,
            replSnippetDeclarations = listOf(
                // PROPERTY with PUBLIC visibility + a concrete return type signature — the common case.
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.PROPERTY,
                    name = "x",
                    descriptor = "I",
                    visibility = SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC,
                    returnTypeSignature = "kotlin.Int",
                ),
                // FUNCTION with INTERNAL visibility + a function-shaped return type signature.
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.FUNCTION,
                    name = "foo",
                    descriptor = null,
                    visibility = SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL,
                    returnTypeSignature = "kotlin.Unit",
                ),
                // CLASS with PROTECTED visibility and *no* return type (the type *is* the declaration).
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.CLASS,
                    name = "Nested",
                    descriptor = null,
                    visibility = SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED,
                    returnTypeSignature = null,
                ),
                // TYPEALIAS with PRIVATE visibility — exercises the consumer-side filter.
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS,
                    name = "Alias",
                    descriptor = null,
                    visibility = SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE,
                    returnTypeSignature = null,
                ),
                // UNKNOWN — pre-v3 producers can omit visibility; the field defaults gracefully.
                SnippetArtifactSidecar.MemberRef(
                    kind = SnippetArtifactSidecar.MemberRef.Kind.PROPERTY,
                    name = "unknownVisibility",
                    descriptor = null,
                    visibility = SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN,
                    returnTypeSignature = null,
                ),
            ),
            imports = listOf(
                SnippetArtifactSidecar.ImportEntry("kotlin.random.Random", isAllUnder = false, aliasName = null),
                SnippetArtifactSidecar.ImportEntry("java.util", isAllUnder = true, aliasName = "ju"),
            ),
        )
        val bytes = SnippetArtifactSidecarProtoCodec.encode(original)
        val decoded = SnippetArtifactSidecarProtoCodec.decode(bytes)
        assertEquals(original, decoded, "sidecar must round-trip through protobuf without loss")

        // A declaration-only sidecar (no imports) must also round-trip — an empty repeated field
        // decodes back to an empty list, not a distinct value.
        val noImports = original.copy(imports = emptyList())
        val decoded2 = SnippetArtifactSidecarProtoCodec.decode(SnippetArtifactSidecarProtoCodec.encode(noImports))
        assertEquals(noImports, decoded2)
        assertNotEquals(decoded, decoded2)
    }

    @Test
    fun testHeaderProtoRoundtrip() {
        val original = SnippetArtifactHeader(
            headerVersion = SnippetArtifactHeader.CURRENT_VERSION,
            snippetName = "Snippet_1",
            snippetClassInternalName = "some/pkg/Snippet_1",
            packageFqName = "some.pkg",
            stateObjectFqName = "some.pkg.MyReplState",
            resultPropertyName = "res1",
            isImplicit = true,
        )
        val decoded = SnippetArtifactHeaderProtoCodec.decode(SnippetArtifactHeaderProtoCodec.encode(original))
        assertEquals(original, decoded, "header must round-trip through protobuf without loss")

        // Optional `resultPropertyName` + the `isImplicit` flag round-trip independently.
        val noResult = original.copy(resultPropertyName = null, isImplicit = false)
        val decoded2 = SnippetArtifactHeaderProtoCodec.decode(SnippetArtifactHeaderProtoCodec.encode(noResult))
        assertEquals(noResult, decoded2)
        assertNotEquals(decoded, decoded2)
        assertEquals(null, decoded2.resultPropertyName, "null resultPropertyName must round-trip as absent")
        assertEquals(false, decoded2.isImplicit)
    }

    @Test
    fun testStateObjectFqNameMismatchIsRejected() {
        // Build a prior artifact whose header carries a specific `stateObjectFqName`.
        val priorHeader = SnippetArtifactHeader(
            headerVersion = SnippetArtifactHeader.CURRENT_VERSION,
            snippetName = "Snippet_1",
            snippetClassInternalName = "Snippet_1",
            packageFqName = "",
            stateObjectFqName = "some.pkg.PriorState",
            resultPropertyName = null,
            isImplicit = false,
        )
        val priorArtifact = SnippetArtifact(
            classFiles = emptyMap(),
            header = SnippetArtifactHeaderProtoCodec.encode(priorHeader),
        )

        // The caller supplies a host configuration whose `replStateObjectFqName` disagrees with the
        // prior artifact's header — the orchestrator must reject this with a clear diagnostic.
        val mismatchHostConfig = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            repl {
                replStateObjectFqName("some.pkg.CallerState")
            }
        }
        val compiler = K2ReplStatelessCompiler()
        @Suppress("DEPRECATION_ERROR")
        val result: ResultWithDiagnostics<SnippetArtifact> = internalScriptingRunSuspend {
            compiler.compile(
                priorSnippets = listOf(priorArtifact),
                snippet = "1".toScriptSource("s2.repl.kts"),
                scriptCompilationConfiguration = baseCompilationConfiguration,
                hostConfiguration = mismatchHostConfig,
            )
        }
        assertTrue(
            result is ResultWithDiagnostics.Failure,
            "expected stateObjectFqName mismatch to produce Failure, got: $result"
        )
        val message = (result as ResultWithDiagnostics.Failure).reports.joinToString("\n") { it.message }
        assertTrue(
            message.contains("some.pkg.PriorState") && message.contains("some.pkg.CallerState"),
            "diagnostic must name both fqs (`some.pkg.PriorState` and `some.pkg.CallerState`); was: $message"
        )
    }

    @Test
    fun testSnippetArtifactCodecRoundtrip() {
        // Compile a real snippet to obtain a non-trivial artifact (class files + header bytes)
        // that exercises both fields of the envelope.
        if (!isK2) return
        val compiler = K2ReplStatelessCompiler()
        val artifact = compileStateless(
            compiler,
            priorSnippets = emptyList(),
            source = "val codecProbe = 7",
            name = "codec_probe.repl.kts",
        ).valueOrThrowExplained("codec probe snippet failed to compile")
        assertTrue(artifact.classFiles.isNotEmpty(), "codec probe must produce class files")
        assertTrue(artifact.header.isNotEmpty(), "codec probe must produce header bytes")

        val encoded = SnippetArtifactCodec.encode(artifact)
        val decoded = SnippetArtifactCodec.decode(encoded)

        // The envelope must preserve content (per-class-file bytes and the header bytes), but
        // class-file key ordering need not survive — `SnippetArtifactCodec.encode` deliberately
        // sorts keys for deterministic output. So compare by `equals` (which checks key set +
        // per-key contents) rather than re-encoding bytes.
        assertEquals(artifact, decoded, "roundtripped artifact must equal the original")

        // Encoding is deterministic: re-encoding the decoded artifact must yield identical bytes.
        val reencoded = SnippetArtifactCodec.encode(decoded)
        assertTrue(
            encoded.contentEquals(reencoded),
            "SnippetArtifactCodec.encode must be deterministic across encode/decode/encode"
        )

        // Class-file content is preserved byte-for-byte (not just structurally).
        for ([name, bytes] in artifact.classFiles) {
            val roundtripped = decoded.classFiles[name]
                ?: fail("decoded artifact missing class file `$name`")
            assertTrue(
                bytes.contentEquals(roundtripped),
                "class file `$name` bytes must roundtrip identically"
            )
        }
    }

    // Note: a BTA-op end-to-end roundtrip test (driving `CompileReplSnippetOperation` through
    // `KotlinToolchains.loadImplementation`) is intentionally **not** included here. The BTA
    // impl module ships a shadow-jar with relocated scripting-compiler classes, so naively
    // adding `testImplementation(":kotlin-build-tools-impl")` here would put two copies of the
    // scripting-compiler symbols on the test classpath (the unshaded api copy + the embedded
    // relocated copy inside the shaded impl jar). The `CompileReplSnippetOperationImpl` is also
    // `internal`, ruling out direct construction across modules. The right home for a smoke test
    // is `kotlin-build-tools-impl/src/test` itself — added as a follow-up iteration; see
    // `iterations/2026-05-28c_stateless-repl-bta-transport.md` §"Follow-ups".

    // ----- helpers -----

    private fun compileStateless(
        compiler: K2ReplStatelessCompiler,
        priorSnippets: List<SnippetArtifact>,
        source: String,
        name: String,
    ): ResultWithDiagnostics<SnippetArtifact> {
        @Suppress("DEPRECATION_ERROR")
        return internalScriptingRunSuspend {
            compiler.compile(
                priorSnippets = priorSnippets,
                snippet = source.toScriptSource(name),
                scriptCompilationConfiguration = baseCompilationConfiguration,
            )
        }
    }

    private fun <T> ResultWithDiagnostics<T>.valueOrThrowExplained(context: String): T {
        return when (this) {
            is ResultWithDiagnostics.Success -> value
            is ResultWithDiagnostics.Failure -> {
                val diagnostics = reports.joinToString(separator = "\n") { "  ${it.severity}: ${it.message}" }
                fail("$context\nDiagnostics:\n$diagnostics")
            }
        }
    }

    companion object {
        private val baseCompilationConfiguration: ScriptCompilationConfiguration =
            ScriptCompilationConfiguration {
                val classpath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
                    ?.mapNotNull { File(it).takeIf { file -> file.exists() } }.orEmpty()
                updateClasspath(classpath + ForTestCompileRuntime.runtimeJarForTests())
                compilerOptions("-Xrender-internal-diagnostic-names=true")
            }
    }
}
