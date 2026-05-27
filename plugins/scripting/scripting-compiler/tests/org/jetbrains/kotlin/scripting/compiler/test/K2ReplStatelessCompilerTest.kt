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
                SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.PROPERTY, "x", descriptor = "I"
                ),
                SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.FUNCTION, "foo", descriptor = null
                ),
                SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.CLASS, "Nested", descriptor = null
                ),
                SnippetArtifactSidecar.MemberRef(
                    SnippetArtifactSidecar.MemberRef.Kind.TYPEALIAS, "Alias", descriptor = null
                ),
            ),
            imports = listOf(
                SnippetArtifactSidecar.ImportEntry("kotlin.random.Random", isAllUnder = false, aliasName = null),
                SnippetArtifactSidecar.ImportEntry("java.util", isAllUnder = true, aliasName = "ju"),
            ),
            stateObjectFqName = "some.pkg.MyReplState",
            resultPropertyName = "\$\$result",
            isSynthetic = true,
        )
        val bytes = SnippetArtifactJsonCodec.encode(original)
        val decoded = SnippetArtifactJsonCodec.decode(bytes)
        assertEquals(original, decoded, "sidecar must round-trip through JSON without loss")

        // Round-trip the synthetic flag with the opposite value too.
        val nonSynthetic = original.copy(isSynthetic = false, resultPropertyName = null)
        val decoded2 = SnippetArtifactJsonCodec.decode(SnippetArtifactJsonCodec.encode(nonSynthetic))
        assertEquals(nonSynthetic, decoded2)
        assertNotEquals(decoded, decoded2)
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
