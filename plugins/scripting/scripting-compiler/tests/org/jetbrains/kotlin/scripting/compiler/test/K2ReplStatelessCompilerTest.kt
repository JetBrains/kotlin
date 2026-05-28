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
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactJsonCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeSidecar
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

        val sidecar1 = artifact1.decodeSidecar()
        assertEquals(0, sidecar1.historyIndex, "snippet 1 historyIndex should be 0")
        assertTrue(artifact1.classFiles.isNotEmpty(), "snippet 1 must emit at least one .class file")
        val declNames1 = sidecar1.replSnippetDeclarations.map { it.name }.toSet()
        assertTrue(
            "x" in declNames1,
            "snippet 1 sidecar must list `x` as a repl-snippet declaration; got: $declNames1"
        )

        // 2. Compile snippet 2 against [artifact1]: `x + 1`.
        //    Successful compile = the entire stateless reconstruction chain worked.
        val artifact2 = compileStateless(
            compiler,
            priorSnippets = listOf(artifact1),
            source = "x + 1",
            name = "s2.repl.kts",
        ).valueOrThrowExplained("stateless snippet 2 compile failed (cross-snippet resolution likely broken)")

        val sidecar2 = artifact2.decodeSidecar()
        assertEquals(1, sidecar2.historyIndex, "snippet 2 historyIndex should be 1")
        assertTrue(artifact2.classFiles.isNotEmpty(), "snippet 2 must emit at least one .class file")
        // Snippet wrapper class names embed the source name; assert at least one classfile mentions `s2`.
        val classKeys = artifact2.classFiles.keys
        assertTrue(
            classKeys.any { it.contains("s2", ignoreCase = true) || it.contains("S2", ignoreCase = true) },
            "snippet 2 classfiles should encode the source name `s2`; got keys: $classKeys"
        )
        // Snippet 2 wrapper class internal name from the sidecar should be present among the classfile keys.
        assertTrue(
            sidecar2.snippetClassInternalName in classKeys ||
                    classKeys.any { it.endsWith("/${sidecar2.snippetClassInternalName.substringAfterLast('/')}") },
            "snippet 2 wrapper class `${sidecar2.snippetClassInternalName}` must be among classfile keys $classKeys"
        )
    }

    @Test
    fun testSidecarJsonRoundtrip() {
        val original = SnippetArtifactSidecar(
            sidecarVersion = SnippetArtifactSidecar.CURRENT_VERSION,
            snippetName = "Snippet_1",
            snippetClassInternalName = "some/pkg/Snippet_1",
            packageFqName = "some.pkg",
            historyIndex = 7,
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
            stateObjectFqName = "some.pkg.MyReplState",
            resultPropertyName = "\$\$result",
            isSynthetic = true,
            isImplicit = true,
        )
        val bytes = SnippetArtifactJsonCodec.encode(original)
        val decoded = SnippetArtifactJsonCodec.decode(bytes)
        assertEquals(original, decoded, "sidecar must round-trip through JSON without loss")

        // Round-trip the synthetic flag with the opposite value too.
        val nonSynthetic = original.copy(isSynthetic = false, resultPropertyName = null)
        val decoded2 = SnippetArtifactJsonCodec.decode(SnippetArtifactJsonCodec.encode(nonSynthetic))
        assertEquals(nonSynthetic, decoded2)
        assertNotEquals(decoded, decoded2)

        // Round-trip `isImplicit` independently of `isSynthetic` (Q10b read-side decoupling):
        // a user-authored snippet that the compilation flagged implicit, *without* the
        // compile-side `_isSyntheticSnippet` flag.
        val implicitNotSynthetic = original.copy(isSynthetic = false, isImplicit = true)
        val decoded3 = SnippetArtifactJsonCodec.decode(SnippetArtifactJsonCodec.encode(implicitNotSynthetic))
        assertEquals(implicitNotSynthetic, decoded3)
        assertTrue(decoded3.isImplicit, "isImplicit must round-trip independently of isSynthetic")
        assertEquals(false, decoded3.isSynthetic, "isSynthetic must round-trip independently of isImplicit")

        // And the inverse — a synthetic snippet that callers chose *not* to surface as implicit.
        val syntheticNotImplicit = original.copy(isSynthetic = true, isImplicit = false)
        val decoded4 = SnippetArtifactJsonCodec.decode(SnippetArtifactJsonCodec.encode(syntheticNotImplicit))
        assertEquals(syntheticNotImplicit, decoded4)
        assertEquals(false, decoded4.isImplicit)
        assertTrue(decoded4.isSynthetic)
    }

    @Test
    fun testStateObjectFqNameMismatchIsRejected() {
        // Build a prior artifact whose sidecar carries a specific `stateObjectFqName`.
        val priorSidecar = SnippetArtifactSidecar(
            sidecarVersion = SnippetArtifactSidecar.CURRENT_VERSION,
            snippetName = "Snippet_1",
            snippetClassInternalName = "Snippet_1",
            packageFqName = "",
            historyIndex = 0,
            replSnippetDeclarations = emptyList(),
            imports = emptyList(),
            stateObjectFqName = "some.pkg.PriorState",
            resultPropertyName = null,
            isSynthetic = false,
        )
        val priorArtifact = SnippetArtifact(
            classFiles = emptyMap(),
            sidecar = SnippetArtifactJsonCodec.encode(priorSidecar),
        )

        // The caller supplies a host configuration whose `replStateObjectFqName` disagrees with the
        // prior artifact's sidecar — the orchestrator must reject this with a clear diagnostic.
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
        // Compile a real snippet to obtain a non-trivial artifact (class files + sidecar bytes)
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
        assertTrue(artifact.sidecar.isNotEmpty(), "codec probe must produce sidecar bytes")

        val encoded = SnippetArtifactCodec.encode(artifact)
        val decoded = SnippetArtifactCodec.decode(encoded)

        // The envelope must preserve content (per-class-file bytes and the sidecar bytes), but
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
        for ((name, bytes) in artifact.classFiles) {
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
