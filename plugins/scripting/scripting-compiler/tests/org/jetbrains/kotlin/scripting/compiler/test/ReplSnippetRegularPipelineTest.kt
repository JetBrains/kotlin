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
import org.jetbrains.kotlin.scripting.compiler.plugin.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.runWithK2JVMCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.withTempDir
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.script.experimental.api.CompiledSnippet
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleFromClassPath
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet
import kotlin.script.experimental.util.LinkedSnippetImpl
import kotlin.script.experimental.util.add
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests exercising REPL-snippet chaining on the **regular** JVM frontend/backend, driven directly
 * through `K2JVMCompiler` -- the same `repl-snippet-regular-mode`/`repl-snippet-prior-class`
 * invocation shape `DaemonReplCompiler` (`libraries/examples/scripting/jsr223-daemon`) uses against a
 * compile daemon -- since that regular pipeline is what production callers (the on-daemon JSR-223
 * example) actually use, plus the [SnippetArtifactSidecar] wire-format round-trip that the pipeline's
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

            // 1. Compile snippet 1 (no priors on the classpath yet): `val x = 42`.
            compiler.compile("val x = 42", "s1.repl.kts")
            val classFiles1 = compiler.lastOutputDir.classFileNames()
            assertTrue(classFiles1.isNotEmpty(), "snippet 1 must emit at least one .class file")
            // Note: `x` being a recognised repl declaration is proven by snippet 2 resolving it below —
            // the regular pipeline carries no artifact header; declarations are read straight from
            // the wrapper class's own embedded `.kotlin_metadata` via `ClasspathBackedFirReplHistoryProvider`.

            // 2. Compile snippet 2 against snippet 1's own `-d` output directory, fed back purely via
            //    the classpath plus its predicted `ClassId` (see RegularPipelineReplCompiler): `x + 1`.
            //    Successful compile = the entire regular-pipeline cross-snippet resolution chain worked.
            compiler.compile("x + 1", "s2.repl.kts")
            val classFiles2 = compiler.lastOutputDir.classFileNames()
            assertTrue(classFiles2.isNotEmpty(), "snippet 2 must emit at least one .class file")
            // Snippet wrapper class names embed the source name; assert at least one classfile mentions `s2`.
            assertTrue(
                classFiles2.any { it.contains("s2", ignoreCase = true) },
                "snippet 2 classfiles should encode the source name `s2`; got: $classFiles2"
            )
            // The predicted wrapper class (the same ClassId fed to the daemon-based compiler's
            // `repl-snippet-prior-class` option) must actually be among the emitted classfiles.
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

            // Compile+evaluate a 3-snippet session, one snippet at a time -- exactly the same
            // compile-then-eval loop `KotlinJsr223JvmScriptEngineBase` drives against
            // `DaemonReplCompiler`/`K2ReplEvaluator` in the on-daemon JSR-223 example, just in-process
            // (via `RegularPipelineReplCompiler`, calling `K2JVMCompiler` directly) and without a
            // compile daemon.
            //   s1: `val x = 42`
            //   s2: `val y = x + 1`   (cross-snippet declaration reference)
            //   s3: `x + y`           (cross-snippet expression — produces a result value)
            chain = chain.add(compiler.compile("val x = 42", "s1.repl.kts"))
            val evaluated1 = evalOrThrow(evaluator, chain, "snippet 1 eval failed")

            chain = chain.add(compiler.compile("val y = x + 1", "s2.repl.kts"))
            val evaluated2 = evalOrThrow(evaluator, chain, "snippet 2 eval failed")

            chain = chain.add(compiler.compile("x + y", "s3.repl.kts"))
            val evaluated3 = evalOrThrow(evaluator, chain, "snippet 3 eval failed")

            // s1 introduced `x = 42`; reading its backing field on the snippet-1 instance must show 42.
            assertEquals(42, evaluated1.readDeclaredField("x"), "snippet 1 `x` must hold 42 after eval")

            // s2 introduced `y = x + 1`; the cross-snippet read of `x` must have resolved to 42 at
            // runtime, yielding `y == 43`.
            assertEquals(43, evaluated2.readDeclaredField("y"), "snippet 2 `y` must hold x+1 == 43 after eval")

            // s3 is an expression `x + y`; 42 + 43 == 85 proves both prior snippets contributed at
            // runtime.
            val resultValue = evaluated3.get().result as? ResultValue.Value
                ?: fail("expected snippet 3 to produce a ResultValue.Value, got: ${evaluated3.get().result}")
            assertEquals(85, resultValue.value, "snippet 3 expression result `x + y` must be 85")
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
 * In-process counterpart of `DaemonReplCompiler` (`libraries/examples/scripting/jsr223-daemon`):
 * compiles a `.repl.kts` source as a chained REPL snippet on the **regular** JVM frontend/backend by
 * calling `K2JVMCompiler` directly instead of a compile daemon -- the same
 * `-Xallow-any-scripts-in-source-roots`/`repl-snippet-regular-mode`/`repl-snippet-prior-class`
 * invocation shape (see `DaemonReplCompiler.buildSnippetCompilerArguments`'s KDoc for the full
 * design rationale), just without any daemon RMI plumbing. Each snippet's classes land in their own
 * `-d` output directory under [workRoot]; prior snippets are fed back to later compiles purely via
 * the classpath plus their predicted [ClassId] ([NameUtils.getSnippetTargetClassName]) -- no
 * artifact blob/header of any kind.
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
