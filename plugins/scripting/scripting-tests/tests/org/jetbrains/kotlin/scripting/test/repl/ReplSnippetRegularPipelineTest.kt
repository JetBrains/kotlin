/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.scripting.compiler.plugin.KOTLIN_SCRIPTING_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec
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
 * Exercises REPL-snippet chaining on the regular JVM frontend/backend via `K2JVMCompiler`, using
 * the same `repl-snippet-regular-mode`/`repl-snippet-prior-class` invocation shape as
 * `DaemonReplCompiler`, plus the [SnippetArtifactSidecar] wire-format round-trip that
 * `ClasspathBackedFirReplHistoryProvider` relies on.
 */
class ReplSnippetRegularPipelineTest {

    private val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
            System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true

    @Test
    fun testStatelessReplCompilesSnippetAgainstPriorArtifact() {
        if (!isK2) return

        withTempDir { workRoot ->
            val compiler = RegularPipelineReplCompiler(workRoot)

            compiler.compile("val x = 42", "s1.repl.kts")
            val classFiles1 = compiler.lastOutputDir.classFileNames()
            assertTrue(classFiles1.isNotEmpty(), "snippet 1 must emit at least one .class file")

            // Snippet 2 resolving `x` proves cross-snippet declaration lookup: the regular pipeline
            // carries no artifact header, so declarations come from snippet 1's own
            // `.kotlin_metadata` via `ClasspathBackedFirReplHistoryProvider`.
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
            val compiler = RegularPipelineReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // Mirrors the compile-then-eval loop `KotlinJsr223JvmScriptEngineBase` drives, in-process
            // via `RegularPipelineReplCompiler` and without a compile daemon.
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
            val compiler = RegularPipelineReplCompiler(workRoot)
            val evaluator = K2ReplEvaluator()
            var chain: LinkedSnippetImpl<CompiledSnippet>? = null

            // Two overloads sharing name `f` — name-only reconstruction (associateBy { it.name })
            // would collapse them; overload-safe reconstruction pairs each with its own MemberRef
            // via the serialized overload signature (MemberRef.descriptor).
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
    fun testSidecarForwardBackwardCompatibleVersions() {
        val member = SnippetArtifactSidecar.MemberRef(
            kind = SnippetArtifactSidecar.MemberRef.Kind.FUNCTION,
            name = "f",
            descriptor = "(kotlin/Int)",
            visibility = SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC,
            returnTypeSignature = "kotlin.Int",
        )

        // A payload tagged with the oldest still-supported version decodes fine (backward compatible).
        val older = SnippetArtifactSidecar(SnippetArtifactSidecar.MIN_SUPPORTED_VERSION, listOf(member), emptyList())
        assertEquals(older, SnippetArtifactSidecarProtoCodec.decode(SnippetArtifactSidecarProtoCodec.encode(older)))

        // A payload tagged with a version newer than this codec knows decodes best-effort (forward
        // compatible) rather than throwing — any not-yet-known fields would simply be skipped.
        val newer = SnippetArtifactSidecar(SnippetArtifactSidecar.CURRENT_VERSION + 7, listOf(member), emptyList())
        assertEquals(newer, SnippetArtifactSidecarProtoCodec.decode(SnippetArtifactSidecarProtoCodec.encode(newer)))

        // A version below the supported floor is rejected with a clear, typed error.
        val tooOld = SnippetArtifactSidecar(SnippetArtifactSidecar.MIN_SUPPORTED_VERSION - 1, listOf(member), emptyList())
        val ex = assertFailsWith<IllegalStateException> {
            SnippetArtifactSidecarProtoCodec.decode(SnippetArtifactSidecarProtoCodec.encode(tooOld))
        }
        assertTrue(
            ex.message?.contains("older than the minimum supported") == true,
            "unexpected error message: ${ex.message}",
        )
    }

    // ----- helpers -----

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
 * In-process counterpart of `DaemonReplCompiler`: compiles a `.repl.kts` source as a chained REPL
 * snippet on the regular JVM frontend/backend via `K2JVMCompiler` directly, using the same
 * `-Xallow-any-scripts-in-source-roots`/`repl-snippet-regular-mode`/`repl-snippet-prior-class`
 * invocation shape (see `DaemonReplCompiler.buildSnippetCompilerArguments`'s KDoc), without any
 * daemon RMI plumbing. Each snippet's classes land in their own `-d` directory under [workRoot];
 * prior snippets are fed back purely via the classpath plus their predicted [ClassId]
 * ([NameUtils.getSnippetTargetClassName]).
 */
private class RegularPipelineReplCompiler(private val workRoot: File) {
    private val priorOutputDirs = mutableListOf<File>()
    private val priorClassIds = mutableListOf<ClassId>()
    private var counter = 0

    /** The `-d` output directory of the most recently compiled snippet. */
    lateinit var lastOutputDir: File
        private set

    /** The predicted [ClassId] of the most recently compiled snippet's wrapper class. */
    lateinit var lastClassId: ClassId
        private set

    fun compile(source: String, name: String): KJvmCompiledScript {
        val outputDir = File(workRoot, "out-${counter++}").also { it.mkdirs() }
        val sourceDir = Files.createTempDirectory("k2-repl-cli-src-").toFile()
        try {
            val scriptFile = File(sourceDir, name).also { it.writeText(source) }
            val classpathEntries = listOf(ForTestCompileRuntime.runtimeJarForTests().absolutePath) +
                    priorOutputDirs.map { it.absolutePath }
            val args = buildList {
                add(K2JVMCompilerArguments::classpath.cliArgument)
                add(classpathEntries.joinToString(File.pathSeparator))
                add(K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument)
                add(@Suppress("DEPRECATION") K2JVMCompilerArguments::useFirLT.cliArgument("false"))
                add("-P")
                add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-regular-mode=true")
                for (classId in priorClassIds) {
                    add("-P")
                    add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-prior-class=${classId.asString()}")
                }
                add(K2JVMCompilerArguments::destination.cliArgument)
                add(outputDir.absolutePath)
                add(CommonCompilerArguments::suppressVersionWarnings.cliArgument)
                add(scriptFile.absolutePath)
            }
            runWithK2JVMCompiler(args.toTypedArray())

            val classId = ClassId(FqName.ROOT, NameUtils.getSnippetTargetClassName(scriptFile.name))
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
        } finally {
            sourceDir.deleteRecursively()
        }
    }
}

/** Names of every `.class` file emitted directly or nested under this directory. */
private fun File.classFileNames(): List<String> =
    walkTopDown().filter { it.isFile && it.extension == "class" }.map { it.name }.toList()

/** Reflectively reads declared field [fieldName] off this evaluated snippet's instance. */
private fun LinkedSnippet<KJvmEvaluatedSnippet>.readDeclaredField(fieldName: String): Any? {
    val result = get().result
    val scriptClass = result.scriptClass ?: fail("evaluated snippet has no scriptClass (result=$result)")
    val field = scriptClass.java.getDeclaredField(fieldName).apply { isAccessible = true }
    return field.get(result.scriptInstance)
}
