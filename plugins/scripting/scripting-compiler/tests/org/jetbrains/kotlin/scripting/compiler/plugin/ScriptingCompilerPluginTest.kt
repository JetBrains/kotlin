/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeHeader
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.reporter
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.SCRIPT_DEFINITION_MARKERS_PATH
import org.jetbrains.kotlin.scripting.definitions.discoverScriptTemplatesInClasspath
import org.jetbrains.kotlin.scripting.definitions.loadScriptTemplatesFromClasspath
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ScriptingCompilerPluginTest {

    companion object {
        val TEST_DATA_DIR: String = ForTestCompileRuntime.transformTestDataPath("plugins/scripting/scripting-compiler/testData").path
    }

    init {
        setIdeaIoUseFallback()
    }

    val runtimeClasspath: List<File> = listOf(
        ForTestCompileRuntime.runtimeJarForTests(),
        ForTestCompileRuntime.scriptRuntimeJarForTests(),
        ForTestCompileRuntime.reflectJarForTests(),
    )
    val scriptingClasspath: List<File> = listOf(ForTestCompileRuntime.getFileFromProperty("kotlin.scripting.common.jar"))

    private fun createEnvironment(
        sources: List<String>,
        destDir: File,
        messageCollector: MessageCollector,
        disposable: Disposable,
        confBody: CompilerConfiguration.() -> Unit
    ): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK).apply {
            updateWithBaseCompilerArguments()
            @OptIn(MessageCollectorAccess::class) // write access
            this.messageCollector = messageCollector
            addKotlinSourceRoots(sources)
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destDir)
            confBody()
        }
        configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ScriptingK2CompilerPluginRegistrar())

        @OptIn(CoreEnvironmentDeprecation::class)
        return KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    @Test
    fun testScriptResolverEnvironmentArgsParsing() {

        val longStr = (1..100).joinToString("\\,") { """\" $it aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \\""" }
        val unescapeRe = """\\(["\\,])""".toRegex()
        val cmdlineProcessor = ScriptingCommandLineProcessor()
        val configuration = CompilerConfiguration.create()

        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION as AbstractCliOption,
            """abc=def,11="ab cd \\ \"",long="$longStr"""",
            configuration
        )

        val res = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)

        assertEquals(
            hashMapOf("abc" to "def", "11" to "ab cd \\ \"", "long" to unescapeRe.replace(longStr, "\$1")),
            res
        )
    }

    @Test
    fun testReplSnippetCompilationOptionsParsing() {
        val cmdlineProcessor = ScriptingCommandLineProcessor()
        val configuration = CompilerConfiguration.create()

        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.REPL_SNIPPET_MODE_OPTION as AbstractCliOption, "true", configuration
        )
        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.REPL_SNIPPET_PRIOR_ARTIFACT_OPTION as AbstractCliOption, "/tmp/s1.artifact", configuration
        )
        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.REPL_SNIPPET_PRIOR_ARTIFACT_OPTION as AbstractCliOption, "/tmp/s2.artifact", configuration
        )
        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.REPL_SNIPPET_ARTIFACT_OUTPUT_OPTION as AbstractCliOption, "/tmp/out.artifact", configuration
        )
        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.REPL_SNIPPET_NAME_OPTION as AbstractCliOption, "s3.repl.kts", configuration
        )

        assertEquals(true, configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE))
        assertEquals(
            listOf(File("/tmp/s1.artifact"), File("/tmp/s2.artifact")),
            configuration.getList(ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_ARTIFACTS)
        )
        assertEquals(File("/tmp/out.artifact"), configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT))
        assertEquals("s3.repl.kts", configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_NAME))
    }

    @Test
    fun testReplSnippetCompilationPipelineBranch() {
        // Drives the snippet-mode consumer (`compileReplSnippet`) that the regular compile entry
        // routes to when `REPL_SNIPPET_COMPILATION_MODE` is set: read priors -> drive
        // `K2ReplStatelessCompiler` -> write the produced artifact. Proves the keys are consumed,
        // the produced artifact round-trips through `SnippetArtifactCodec`, prior snippets are fed
        // (snippet 2 references snippet 1's binding), and a failing snippet does not write output.
        val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
                System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true
        if (!isK2) return

        withTempDir { tmpdir ->
            val scriptClasspath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
                ?.mapNotNull { File(it).takeIf { file -> file.exists() } }.orEmpty()
            val snippetClasspath = scriptClasspath + runtimeClasspath

            fun snippetConfig(collector: MessageCollectorImpl): CompilerConfiguration =
                CompilerConfiguration.create().apply {
                    @OptIn(MessageCollectorAccess::class) // write access
                    this.messageCollector = collector
                    addJvmClasspathRoots(snippetClasspath)
                    put(ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE, true)
                }

            // 1. Snippet 1 (no priors): `val x = 42`.
            val artifact1File = File(tmpdir, "s1.artifact")
            val collector1 = MessageCollectorImpl()
            val config1 = snippetConfig(collector1)
            config1.put(ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT, artifact1File)
            val exit1 = compileReplSnippet("val x = 42".toScriptSource("s1.repl.kts"), config1)
            assertEquals(ExitCode.OK, exit1, "snippet 1 should compile:\n$collector1")
            assertTrue(artifact1File.exists()) { "snippet 1 artifact must be written" }
            val artifact1 = SnippetArtifactCodec.decode(artifact1File.readBytes())
            assertTrue(artifact1.classFiles.isNotEmpty()) { "snippet 1 must emit at least one class file" }
            // After the "full cut", declarations are no longer carried in the artifact header; that
            // `x` is a recognised repl declaration is proven by snippet 2 resolving it below.
            assertTrue(artifact1.decodeHeader().snippetClassInternalName.isNotEmpty()) {
                "snippet 1 header must record the wrapper class internal name"
            }

            // 2. Snippet 2 against snippet 1: `x + 1` — proves the prior artifact is consumed.
            val artifact2File = File(tmpdir, "s2.artifact")
            val collector2 = MessageCollectorImpl()
            val config2 = snippetConfig(collector2)
            config2.put(ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_ARTIFACTS, listOf(artifact1File))
            config2.put(ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT, artifact2File)
            val exit2 = compileReplSnippet("x + 1".toScriptSource("s2.repl.kts"), config2)
            assertEquals(ExitCode.OK, exit2, "snippet 2 should compile against snippet 1:\n$collector2")
            assertTrue(artifact2File.exists()) { "snippet 2 artifact must be written" }
            val artifact2 = SnippetArtifactCodec.decode(artifact2File.readBytes())
            assertTrue(artifact2.classFiles.isNotEmpty()) { "snippet 2 must emit at least one class file" }
            assertTrue(artifact2.decodeHeader().snippetClassInternalName.isNotEmpty()) {
                "snippet 2 header must record the wrapper class internal name"
            }

            // 3. Error path: an unresolved reference must fail and write no artifact.
            val artifact3File = File(tmpdir, "s3.artifact")
            val config3 = snippetConfig(MessageCollectorImpl())
            config3.put(ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT, artifact3File)
            val exit3 = compileReplSnippet("thisSymbolDoesNotExistZzz + 1".toScriptSource("s3.repl.kts"), config3)
            assertEquals(ExitCode.COMPILATION_ERROR, exit3, "snippet referencing an undefined symbol must fail")
            assertTrue(!artifact3File.exists()) { "no artifact must be written on a failed snippet compile" }
        }
    }

    @Test
    fun testReplSnippetCompilationViaCli() {
        // End-to-end proof that the snippet-mode branch is reachable through the *regular* compiler
        // entry: drive the real `K2JVMCompiler` with `-expression` + the scripting-plugin `-P`
        // options that switch on snippet mode and name the output artifact. This is the same
        // invocation shape a `CompileService.compile(...)` call would carry (plugin args forwarded
        // verbatim), so it exercises `eval` -> `compileReplSnippet`, `-P` parsing, classpath
        // threading and artifact production through the real compiler. (Cross-snippet prior
        // consumption is covered by `testReplSnippetCompilationPipelineBranch`, which can give the
        // snippets distinct names — `-expression` always names the synthetic source `script.kts`.)
        val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
                System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true
        if (!isK2) return

        withTempDir { tmpdir ->
            val cp = (runtimeClasspath + scriptingClasspath).joinToString(File.pathSeparator)

            val artifactFile = File(tmpdir, "s1.artifact")
            val exitCode = K2JVMCompiler().exec(
                System.err,
                K2JVMCompilerArguments::classpath.cliArgument, cp,
                K2JVMCompilerArguments::expression.cliArgument, "val x = 42",
                "-P", "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-mode=true",
                "-P", "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-artifact-output=${artifactFile.absolutePath}",
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
            )
            assertEquals(ExitCode.OK, exitCode)
            assertTrue(artifactFile.exists()) { "the CLI snippet-mode compile must write the output artifact" }

            val artifact = SnippetArtifactCodec.decode(artifactFile.readBytes())
            assertTrue(artifact.classFiles.isNotEmpty()) { "CLI-produced artifact must contain class files" }
            val header = artifact.decodeHeader()
            assertTrue(header.snippetClassInternalName.isNotEmpty()) {
                "CLI-produced artifact header must record the wrapper class internal name"
            }
        }
    }

    @Test
    fun testReplSnippetCompilationViaKotlincSubprocess() {
        // Like `testReplSnippetCompilationViaCli`, but compiles each snippet in a *genuinely separate
        // OS process* — a freshly forked JVM running `K2JVMCompiler` off this test's classpath (the
        // scripting plugin is auto-discovered there via its `META-INF/services` files). This is the
        // strongest proof that the stateless snippet sequence works
        // out-of-process via the regular compile path: the artifact of snippet 1 is handed to a
        // *second*, independent compiler process as a prior, and snippet 2 (`x + 1`) resolves `x`
        // across that process boundary. Snippets are given distinct names via `repl-snippet-name`
        // because `-expression` always names the synthetic source `script.kts`.
        val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
                System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true
        if (!isK2) return

        withTempDir { tmpdir ->
            val scriptCp = (runtimeClasspath + scriptingClasspath).joinToString(File.pathSeparator)

            fun snippetArgs(source: String, name: String, output: File, priors: List<File>): List<String> = buildList {
                add(K2JVMCompilerArguments::classpath.cliArgument); add(scriptCp)
                add(K2JVMCompilerArguments::expression.cliArgument); add(source)
                add("-P"); add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-mode=true")
                add("-P"); add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-name=$name")
                for (prior in priors) {
                    add("-P"); add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-prior-artifact=${prior.absolutePath}")
                }
                add("-P"); add("plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:repl-snippet-artifact-output=${output.absolutePath}")
                add(CommonCompilerArguments::suppressVersionWarnings.cliArgument)
            }

            // Snippet 1 (no priors), compiled in its own process.
            val out1 = File(tmpdir, "s1.artifact")
            val result1 = runCompilerSubprocess(snippetArgs("val x = 42", "s1.repl.kts", out1, emptyList()))
            assertEquals(ExitCode.OK.code, result1.first, "snippet 1 subprocess compile must succeed; output:\n${result1.second}")
            assertTrue(out1.exists()) { "snippet 1 artifact must be written by the subprocess" }

            // Snippet 2 (`x + 1`), compiled in a *second* process with snippet 1 as a prior.
            val out2 = File(tmpdir, "s2.artifact")
            val result2 = runCompilerSubprocess(snippetArgs("x + 1", "s2.repl.kts", out2, listOf(out1)))
            assertEquals(ExitCode.OK.code, result2.first, "snippet 2 subprocess compile against the prior must succeed; output:\n${result2.second}")
            assertTrue(out2.exists()) { "snippet 2 artifact must be written by the subprocess" }

            val artifact2 = SnippetArtifactCodec.decode(out2.readBytes())
            assertTrue(artifact2.classFiles.isNotEmpty()) { "snippet 2 artifact must contain class files" }
            // The prior artifact having been decoded and consumed across the process boundary is
            // proven by snippet 2's `x + 1` compiling at all (its `ExitCode.OK` above) — `x` resolves
            // only from the prior. The header just records snippet 2's own wrapper class.
            assertTrue(artifact2.decodeHeader().snippetClassInternalName.isNotEmpty()) {
                "snippet 2 header must record the wrapper class internal name"
            }
        }
    }

    /**
     * Runs `K2JVMCompiler` in a freshly forked JVM (this test's classpath, which carries the
     * scripting plugin auto-discovered via `META-INF/services`). Returns the process exit code and
     * the merged stdout+stderr, the latter only for failure diagnostics.
     */
    private fun runCompilerSubprocess(compilerArgs: List<String>): Pair<Int, String> {
        val javaExe = File(File(System.getProperty("java.home"), "bin"), "java").absolutePath
        val command = buildList {
            add(javaExe)
            add("-cp"); add(System.getProperty("java.class.path"))
            add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            addAll(compilerArgs)
        }
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            fail("Compiler subprocess timed out")
        }
        return process.exitValue() to output
    }

    @Test
    fun testLazyScriptDefinitionDiscovery() {

        withTempDir { tmpdir ->
            withDisposable { disposable ->
                // Three tests in one function: the direct loading, the discovery code separately, and as a part of regular compilation
                // tests are combined to avoid multiple compilation of script definition modules

                val defsOut = File(tmpdir, "testLazyScriptDefinition/out/defs")
                val defsSrc = File(TEST_DATA_DIR, "lazyDefinitions/definitions")
                val scriptsOut = File(tmpdir, "testLazyScriptDefinition/out/scripts")
                val scriptsSrc = File(TEST_DATA_DIR, "lazyDefinitions/scripts")
                val scriptsOut2 = File(tmpdir, "testLazyScriptDefinition/out/scripts2")
                val defClasses = listOf("TestScriptWithReceivers", "TestScriptWithSimpleEnvVars")

                val messageCollector = MessageCollectorImpl()

                val definitionsCompileResult = KotlinToJVMBytecodeCompiler.compileBunchOfSources(
                    createEnvironment(defClasses.map { File(defsSrc, "$it.kt").canonicalPath }, defsOut, messageCollector, disposable) {
                        addJvmClasspathRoots(runtimeClasspath)
                        addJvmClasspathRoots(scriptingClasspath)
                    }
                )

                assertTrue(definitionsCompileResult) {
                    "Compilation of script definitions failed: $messageCollector"
                }

                messageCollector.clear()

                loadScriptTemplatesFromClasspath(
                    listOf("TestScriptWithReceivers", "TestScriptWithSimpleEnvVars"),
                    listOf(defsOut),
                    emptyList(),
                    this::class.java.classLoader,
                    defaultJvmScriptingHostConfiguration,
                    messageCollector.reporter
                ).toList()

                for (def in defClasses) {
                    assertTrue(messageCollector.messages.any { it.message.contains("Configure scripting: Added template $def") }) {
                        "Missing messages from loading sequence (should contain \"Added template $def\"):\n$messageCollector"
                    }
                    assertTrue(messageCollector.messages.none { it.message.contains("Configure scripting: loading script definition class $def") }) {
                        "Unexpected messages from loading sequence (should not contain \"loading script definition class $def\"):\n$messageCollector"
                    }
                }

                messageCollector.clear()

                // chacking lazy discovery

                val templatesDir = File(defsOut, SCRIPT_DEFINITION_MARKERS_PATH).also { it.mkdirs() }
                for (def in defClasses) {
                    File(templatesDir, def).createNewFile()
                }

                val lazyDefsSeq =
                    discoverScriptTemplatesInClasspath(
                        listOf(defsOut),
                        this::class.java.classLoader,
                        defaultJvmScriptingHostConfiguration,
                        messageCollector.reporter
                    )

                assertTrue(messageCollector.messages.isEmpty()) {
                    "Unexpected messages from discovery sequence (should be empty):\n$messageCollector"
                }

                val lazyDefs = lazyDefsSeq.toList()

                for (def in defClasses) {
                    assertTrue(messageCollector.messages.any { it.message.contains("Configure scripting: Added template $def") }) {
                        "Missing messages from discovery sequence (should contain \"Added template $def\"):\n$messageCollector"
                    }
                    assertTrue(messageCollector.messages.none { it.message.contains("Configure scripting: loading script definition class $def") }) {
                        "Unexpected messages from discovery sequence (should not contain \"loading script definition class $def\"):\n$messageCollector"
                    }
                }

                messageCollector.clear()

                val scriptFiles = scriptsSrc.listFiles { file: File -> file.extension == "kts" }.map { it.canonicalPath }

                val scriptsCompileEnv = createEnvironment(scriptFiles, scriptsOut, messageCollector, disposable) {
                    addJvmClasspathRoots(runtimeClasspath)
                    addJvmClasspathRoots(scriptingClasspath)
                    addJvmClasspathRoot(defsOut)
                    addAll(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, lazyDefs)
                }

                val res = KotlinToJVMBytecodeCompiler.compileBunchOfSources(scriptsCompileEnv)

                assertTrue(res) {
                    "Failed to compile scripts:\n$messageCollector"
                }

                val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("${CommonCompilerArguments::languageVersion.cliArgument} 1.9") != true &&
                        System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("${CommonCompilerArguments::languageVersion.cliArgument} 1.9") != true

                val cp = (runtimeClasspath + scriptingClasspath + defsOut).joinToString(File.pathSeparator)
                val exitCode = K2JVMCompiler().exec(
                    System.err,
                    K2JVMCompilerArguments::classpath.cliArgument,
                    cp,
                    *(scriptFiles.toTypedArray()),
                    K2JVMCompilerArguments::destination.cliArgument,
                    scriptsOut2.canonicalPath,
                    K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument,
                    @Suppress("DEPRECATION") K2JVMCompilerArguments::useFirLT.cliArgument("false"),
                    CommonCompilerArguments::languageVersion.cliArgument,
                    if (isK2) "2.0" else "1.9",
                    CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                )

                assertEquals(ExitCode.OK, exitCode)
            }
        }
    }

    @Test
    fun testLazyScriptDefinitionOtherAnnotation() {

        withTempDir { tmpdir ->
            withDisposable { disposable ->
                val defsOut = File(tmpdir, "testLazyScriptDefinition/out/otherAnn")
                val defsSrc = File(TEST_DATA_DIR, "lazyDefinitions/definitions")
                val defClasses = listOf("TestScriptWithOtherAnnotation")

                val messageCollector = MessageCollectorImpl()

                val definitionsCompileResult = KotlinToJVMBytecodeCompiler.compileBunchOfSources(
                    createEnvironment(defClasses.map { File(defsSrc, "$it.kt").canonicalPath }, defsOut, messageCollector, disposable) {
                        addJvmClasspathRoots(runtimeClasspath)
                        addJvmClasspathRoots(scriptingClasspath)
                    }
                )

                assertTrue(definitionsCompileResult) {
                    "Compilation of script definitions failed: $messageCollector"
                }

                val templatesDir = File(defsOut, SCRIPT_DEFINITION_MARKERS_PATH).also { it.mkdirs() }
                for (def in defClasses) {
                    File(templatesDir, def).createNewFile()
                }

                messageCollector.clear()

                discoverScriptTemplatesInClasspath(
                    listOf(defsOut),
                    this::class.java.classLoader,
                    defaultJvmScriptingHostConfiguration,
                    messageCollector.reporter
                ).toList()

                assertTrue(
                    messageCollector.messages.isNotEmpty()
                            && messageCollector.messages.all { it.message.contains("s not marked with any known kotlin script annotation") }
                ) {
                    "Unexpected messages from discovery sequence:\n$messageCollector"
                }
            }
        }
    }
}

fun MessageCollectorImpl.assertHasMessage(msg: String, desiredSeverity: CompilerMessageSeverity? = null) {
    assert(messages.any { it.message.contains(msg) && (desiredSeverity == null || it.severity == desiredSeverity) }) {
        "Expecting message \"$msg\" with severity ${desiredSeverity?.toString() ?: "Any"}, actual:\n" +
                messages.joinToString("\n") { it.severity.toString() + ": " + it.message }
    }
}

fun assertTrue(exp: Boolean, msg: () -> String) {
    if (!exp) {
        fail(msg())
    }
}
