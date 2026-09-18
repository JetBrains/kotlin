/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplSnippetConfigurationCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadata
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadataCodec
import org.jetbrains.kotlin.scripting.test.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.test.runWithK2JVMCompiler
import org.jetbrains.kotlin.scripting.test.withTempDir
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.script.experimental.api.*
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.K2ReplEvaluator
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleFromClassPath
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add
import kotlin.test.*

/**
 * Stateless REPL-snippet chaining, using the same invocation shape as `DaemonReplCompiler`, plus
 * the [SnippetArtifactMetadata] wire-format round-trip.
 */
class ReplSnippetStatelessCompilationTest {

    private val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
            System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true

    @Test
    fun testStatelessReplCompilesSnippetAgainstPriorArtifact() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)

            compiler.compile("val x = 42", "s1.repl.kts")
            val classFiles1 = compiler.lastOutputDir.classFileNames()
            assertTrue(classFiles1.isNotEmpty(), "snippet 1 must emit at least one .class file")

            // Resolving `x` here goes through snippet 1's `.kotlin_metadata`: a stateless compile
            // carries no artifact header.
            compiler.compile("x + 1", "s2.repl.kts")
            val classFiles2 = compiler.lastOutputDir.classFileNames()
            assertTrue(classFiles2.isNotEmpty(), "snippet 2 must emit at least one .class file")
            assertTrue(
                classFiles2.any { it.contains("s2", ignoreCase = true) },
                "snippet 2 classfiles should encode the source name `s2`; got: $classFiles2"
            )
            assertTrue(
                classFiles2.any { it == "${compiler.lastClassId.shortClassName.asString()}.class" },
                "snippet 2 wrapper class `${compiler.lastClassId}` must be among classfiles $classFiles2"
            )
        }
    }

    @Test
    fun testStatelessReplExecutesMultiSnippetSequence() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // Mirrors the compile-then-eval loop `KotlinJsr223JvmScriptEngineBase` drives.
            chain = chain.add(compiler.compile("val x = 42", "s1.repl.kts"))
            val evaluated1 = evalOrThrow(evaluator, chain, "snippet 1 eval failed")

            chain = chain.add(compiler.compile("val y = x + 1", "s2.repl.kts"))
            val evaluated2 = evalOrThrow(evaluator, chain, "snippet 2 eval failed")

            chain = chain.add(compiler.compile("x + y", "s3.repl.kts"))
            val evaluated3 = evalOrThrow(evaluator, chain, "snippet 3 eval failed")

            assertEquals(42, evaluated1.readDeclaredField("x"), "snippet 1 `x` must hold 42 after eval")
            assertEquals(43, evaluated2.readDeclaredField("y"), "snippet 2 `y` must hold x+1 == 43 after eval")

            val resultValue = evaluated3.get().result as? ResultValue.Value
                ?: fail("expected snippet 3 to produce a ResultValue.Value, got: ${evaluated3.get().result}")
            assertEquals(85, resultValue.value, "snippet 3 expression result `x + y` must be 85")
        }
    }

    @Test
    fun testStatelessReplResolvesOverloadedPriorFunctions() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // Two overloads sharing name `f`: name-only reconstruction would collapse them, only the
            // serialized MemberRef.descriptor keeps them apart.
            chain = chain.add(compiler.compile("fun f(a: Int) = a + 1\nfun f(a: String) = a.length", "s1.repl.kts"))
            evalOrThrow(evaluator, chain, "snippet 1 eval failed")

            chain = chain.add(compiler.compile("f(10) + f(\"abcd\")", "s2.repl.kts"))
            val evaluated2 = evalOrThrow(evaluator, chain, "snippet 2 eval failed")

            val resultValue = evaluated2.get().result as? ResultValue.Value
                ?: fail("expected snippet 2 to produce a ResultValue.Value, got: ${evaluated2.get().result}")
            assertEquals(15, resultValue.value, "both overloads of the prior `f` must resolve: f(10)+f(\"abcd\") == 15")
        }
    }

    @Test
    fun testStatelessReplKeepsVisibilitiesOfOverloadedPriorFunctions() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // The private overload comes first on purpose: only matching the serialized MemberRef signature tells the
            // two `h`-s apart, while falling back to the first candidate would make the public one private as well.
            chain = chain.add(
                compiler.compile("private fun h(a: String) = a.length\nfun h(a: Int) = a + 1", "s1.repl.kts")
            )
            evalOrThrow(evaluator, chain, "snippet 1 eval failed")

            chain = chain.add(compiler.compile("h(10)", "s2.repl.kts"))
            val evaluated2 = evalOrThrow(evaluator, chain, "snippet 2 eval failed")

            val resultValue = evaluated2.get().result as? ResultValue.Value
                ?: fail("expected snippet 2 to produce a ResultValue.Value, got: ${evaluated2.get().result}")
            assertEquals(11, resultValue.value, "the public overload of the prior `h` must stay public: h(10) == 11")
        }
    }

    @Test
    fun testStatelessReplUsesNestedClassOfPriorSnippet() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // Only `Outer` is a snippet-level declaration: `Nested` and its members are accessed through
            // an instance of `Nested`, so the reconstruction must not treat them as snippet members.
            chain = chain.add(
                compiler.compile("class Outer { class Nested { val v = \"O\"\nfun f() = \"K\" } }", "s1.repl.kts")
            )
            evalOrThrow(evaluator, chain, "snippet 1 eval failed")

            chain = chain.add(compiler.compile("val n = Outer.Nested()\nn.v + n.f()", "s2.repl.kts"))
            val evaluated2 = evalOrThrow(evaluator, chain, "snippet 2 eval failed")

            val resultValue = evaluated2.get().result as? ResultValue.Value
                ?: fail("expected snippet 2 to produce a ResultValue.Value, got: ${evaluated2.get().result}")
            assertEquals("OK", resultValue.value, "the nested class of the prior snippet and its members must resolve")
        }
    }

    @Test
    fun testStatelessReplRecoversLongSessionFromSingleBackLink() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = StatelessReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            val snippetCount = 10
            for (i in 1..snippetCount) {
                chain = chain.add(compiler.compile("val v$i = $i", "s$i.repl.kts"))
                evalOrThrow(evaluator, chain, "snippet $i eval failed")
            }

            // Only snippet 10's ClassId is passed (the harness mirrors the hosts): resolving `v1`
            // requires the nine older snippets to be recovered from the metadata back-links.
            chain = chain.add(compiler.compile((1..snippetCount).joinToString(" + ") { "v$it" }, "sum.repl.kts"))
            val evaluated = evalOrThrow(evaluator, chain, "summing snippet eval failed")

            val resultValue = evaluated.get().result as? ResultValue.Value
                ?: fail("expected the summing snippet to produce a ResultValue.Value, got: ${evaluated.get().result}")
            assertEquals(55, resultValue.value, "every snippet of the recovered history must be visible")
        }
    }

    @Test
    fun testFullPriorChainAgreesWithBackLinkWalk() {
        if (!isK2) return

        // The option augments the history rather than defining it, so passing the whole chain and
        // passing only its last element must converge to the same history.
        val sources = listOf("val a = 1", "val b = a + 1", "val c = b + 1", "a + b + c")
        val results = listOf(false, true).map { passFullPriorChain ->
            withTempDir { workRoot ->
                val compiler = StatelessReplCompiler(workRoot, passFullPriorChain = passFullPriorChain)
                val evaluator = K2ReplEvaluator()
                var chain: LinkedSnippetImpl<CompiledSnippet>? = null
                var last: LinkedSnippet<KJvmEvaluatedSnippet>? = null
                for ([index, source] in sources.withIndex()) {
                    chain = chain.add(compiler.compile(source, "s$index.repl.kts"))
                    last = evalOrThrow(evaluator, chain, "snippet $index eval failed (passFullPriorChain=$passFullPriorChain)")
                }
                val lastResult = last?.get()?.result ?: fail("no snippet was evaluated")
                (lastResult as? ResultValue.Value)?.value ?: fail("expected a ResultValue.Value, got: $lastResult")
            }
        }
        assertEquals(6, results.first(), "`a + b + c` must be 6 when only the last prior id is passed")
        assertEquals(results.first(), results.last(), "the explicit chain must produce the same history as the walk")
    }

    @Test
    fun testSnippetDefinitionComesFromTheTransportedConfiguration() {
        if (!isK2) return

        withTempDir { workRoot ->
            // A definition with its own extension and default imports, as a host would supply it.
            val hostConfiguration = ScriptCompilationConfiguration {
                fileExtension("custom.kts")
                defaultImports("kotlin.math.PI")
            }
            val compiler = StatelessReplCompiler(workRoot, snippetConfiguration = hostConfiguration)
            val evaluator = K2ReplEvaluator()

            // `.custom.kts`, not `.kts`: the snippet extension is derived from the configuration.
            // `PI` without an import proves the definition's defaultImports reach the snippet, which
            // the previous ScriptTemplateWithArgs-based snippet definition could not do.
            // (The definition's `baseClass` is deliberately not asserted: a snippet never extends it -
            // FirReplSnippetConfiguratorExtensionImpl gives a snippet only its implicit receivers.)
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null
            chain = chain.add(compiler.compile("val x = PI", "s1.repl.custom.kts"))
            evalOrThrow(evaluator, chain, "snippet 1 eval failed")
            chain = chain.add(compiler.compile("x > 3", "s2.repl.custom.kts"))
            val evaluated = evalOrThrow(evaluator, chain, "snippet 2 eval failed")
            assertEquals(true, (evaluated.get().result as? ResultValue.Value)?.value)

            // Conversely, a plain `.custom.kts` source is a script, so it sees no snippet history.
            compiler.compileExpectingFailure("x > 3", "plain.custom.kts", listOf("unresolved reference 'x'"))
        }
    }

    @Test
    fun testSnippetMetadataCodecRoundtrip() {
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
                // PRIVATE exercises the consumer-side visibility filter.
                SnippetArtifactMetadata.MemberRef(
                    kind = SnippetArtifactMetadata.MemberRef.Kind.TYPEALIAS,
                    name = "Alias",
                    signature = null,
                    visibility = SnippetArtifactMetadata.MemberRef.Visibility.PRIVATE,
                ),
                // Pre-v3 producers can omit visibility, hence UNKNOWN.
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
        assertEquals(original, decoded, "metadata must round-trip through the codec without loss")

        // The chain link is written right after the version, so the fast path must agree with a full decode.
        assertEquals(original.priorSnippetClassId, SnippetArtifactMetadataCodec.decodeChainLinkOnly(bytes))

        // An empty repeated field must decode back to an empty list, not to a distinct value.
        val noImports = original.copy(imports = emptyList())
        val decoded2 = SnippetArtifactMetadataCodec.decode(SnippetArtifactMetadataCodec.encode(noImports))
        assertEquals(noImports, decoded2)
        assertNotEquals(decoded, decoded2)

        // The first snippet of a session has no predecessor, and a configuration may fail to resolve.
        val root = original.copy(priorSnippetClassId = null, serializedCompilationConfiguration = null)
        val rootBytes = SnippetArtifactMetadataCodec.encode(root)
        assertEquals(root, SnippetArtifactMetadataCodec.decode(rootBytes))
        assertNull(SnippetArtifactMetadataCodec.decodeChainLinkOnly(rootBytes))
    }

    @Test
    fun testSnippetMetadataVersionGating() {
        val member = SnippetArtifactMetadata.MemberRef(
            kind = SnippetArtifactMetadata.MemberRef.Kind.FUNCTION,
            name = "f",
            signature = "(kotlin/Int)",
            visibility = SnippetArtifactMetadata.MemberRef.Visibility.PUBLIC,
        )

        // A payload tagged with the oldest still-supported version decodes fine.
        val older = SnippetArtifactMetadata(
            SnippetArtifactMetadata.MIN_SUPPORTED_VERSION, null, listOf(member), emptyList(), null
        )
        assertEquals(older, SnippetArtifactMetadataCodec.decode(SnippetArtifactMetadataCodec.encode(older)))

        // The layout is positional, so a version outside the supported range must be rejected with a
        // clear, typed error rather than misparsed.
        for (unsupportedVersion in listOf(
            SnippetArtifactMetadata.MIN_SUPPORTED_VERSION - 1,
            SnippetArtifactMetadata.CURRENT_VERSION + 7,
        )) {
            val unsupported = SnippetArtifactMetadata(unsupportedVersion, null, listOf(member), emptyList(), null)
            val encoded = SnippetArtifactMetadataCodec.encode(unsupported)
            for (decode in listOf(SnippetArtifactMetadataCodec::decode, SnippetArtifactMetadataCodec::decodeChainLinkOnly)) {
                val ex = assertFailsWith<IllegalStateException> { decode(encoded) }
                assertTrue(
                    ex.message?.contains("outside the supported range") == true,
                    "unexpected error message: ${ex.message}",
                )
            }
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

    private fun evalOrThrow(
        evaluator: K2ReplEvaluator,
        chain: LinkedSnippet<CompiledSnippet>,
        context: String,
    ): LinkedSnippet<KJvmEvaluatedSnippet> {
        @Suppress("DEPRECATION_ERROR")
        return internalScriptingRunSuspend { evaluator.eval(chain, ScriptEvaluationConfiguration()) }
            .valueOrThrowExplained(context)
    }
}

/**
 * In-process counterpart of `DaemonReplCompiler`, driving `K2JVMCompiler` directly with the same
 * argument shape (see `DaemonReplCompiler.buildSnippetCompilerArguments`). Each snippet compiles
 * into its own `-d` directory under [workRoot]; prior snippets are fed back purely via the
 * classpath plus their predicted [ClassId].
 */
private class StatelessReplCompiler(
    private val workRoot: File,
    /**
     * The hosts pass only the immediately preceding snippet and let the compiler walk the metadata
     * back-links; `true` passes the whole chain instead, which must converge to the same history.
     */
    private val passFullPriorChain: Boolean = false,
    /**
     * The host's compilation configuration, transported as a file exactly as the hosts do; `null`
     * leaves snippets on the default `.repl.kts` definition.
     */
    private val snippetConfiguration: ScriptCompilationConfiguration? = null,
) {
    private val snippetConfigurationFile: File? by lazy {
        snippetConfiguration?.let { File(workRoot, "snippet-configuration.bin").also { f -> ReplSnippetConfigurationCodec.writeTo(it, f) } }
    }
    private val priorOutputDirs = mutableListOf<File>()
    private val priorClassIds = mutableListOf<ClassId>()
    private var counter = 0

    lateinit var lastOutputDir: File
        private set

    lateinit var lastClassId: ClassId
        private set

    fun compile(source: String, name: String): KJvmCompiledScript {
        val outputDir = File(workRoot, "out-${counter++}").also { it.mkdirs() }
        withSourceFile(source, name) { scriptFile ->
            runWithK2JVMCompiler(buildArguments(scriptFile, outputDir).toTypedArray())
        }
        val classId = ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(name))
        priorOutputDirs += outputDir
        priorClassIds += classId
        lastOutputDir = outputDir
        lastClassId = classId
        return KJvmCompiledScript(
            sourceLocationId = name,
            compilationConfiguration = ScriptCompilationConfiguration(),
            scriptClassFQName = classId.asSingleFqName().asString(),
            resultField = "\$\$result" to KotlinType("kotlin.Any"),
            otherScripts = emptyList(),
            compiledModule = KJvmCompiledModuleFromClassPath(listOf(outputDir)),
        )
    }

    /** Asserts the compile fails with [expectedErrorPatterns]; the session is left untouched. */
    fun compileExpectingFailure(source: String, name: String, expectedErrorPatterns: List<String>) {
        val outputDir = File(workRoot, "out-failing-${counter++}").also { it.mkdirs() }
        withSourceFile(source, name) { scriptFile ->
            runWithK2JVMCompiler(
                buildArguments(scriptFile, outputDir).toTypedArray(),
                expectedExitCode = ExitCode.COMPILATION_ERROR.code,
                expectedSomeErrPatterns = expectedErrorPatterns,
            )
        }
    }

    private fun <R> withSourceFile(source: String, name: String, body: (File) -> R): R {
        val sourceDir = Files.createTempDirectory("k2-repl-cli-src-").toFile()
        try {
            return body(File(sourceDir, name).also { it.writeText(source) })
        } finally {
            sourceDir.deleteRecursively()
        }
    }

    private fun buildArguments(scriptFile: File, outputDir: File): List<String> {
        val classpathEntries = listOf(ForTestCompileRuntime.runtimeJarForTests()) + priorOutputDirs
        return buildList {
            add(K2JVMCompilerArguments::classpath.cliArgument)
            add(classpathEntries.joinToString(File.pathSeparator) { it.absolutePath })
            add(K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument)
            add(@Suppress("DEPRECATION") K2JVMCompilerArguments::useFirLT.cliArgument("false"))
            add("-P")
            add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-stateless-mode=true")
            snippetConfigurationFile?.let {
                add("-P")
                add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-configuration=${it.absolutePath}")
            }
            for (classId in if (passFullPriorChain) priorClassIds else priorClassIds.takeLast(1)) {
                add("-P")
                add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-prior-class=${classId.asString()}")
            }
            add(K2JVMCompilerArguments::destination.cliArgument)
            add(outputDir.absolutePath)
            add(CommonCompilerArguments::suppressVersionWarnings.cliArgument)
            add(scriptFile.absolutePath)
        }
    }
}

private fun File.classFileNames(): List<String> =
    walkTopDown().filter { it.isFile && it.extension == "class" }.map { it.name }.toList()

private fun LinkedSnippet<KJvmEvaluatedSnippet>.readDeclaredField(fieldName: String): Any? {
    val result = get().result
    val scriptClass = result.scriptClass ?: fail("evaluated snippet has no scriptClass (result=$result)")
    val field = scriptClass.java.getDeclaredField(fieldName).apply { isAccessible = true }
    return field.get(result.scriptInstance)
}
