/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File

class ScriptingCompilerPluginTest : TestCaseWithTmpdir() {

    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-cli/testData"
    }

    private val kotlinPaths: KotlinPaths by lazy {
        val paths = PathUtil.kotlinPathsForDistDirectory
        TestCase.assertTrue("Lib directory doesn't exist. Run 'ant dist'", paths.libPath.absoluteFile.isDirectory)
        paths
    }

    val compilerClasspath = listOf(kotlinPaths.compilerPath)
    val runtimeClasspath = listOf( kotlinPaths.stdlibPath, kotlinPaths.scriptRuntimePath, kotlinPaths.reflectPath)
    val scriptingClasspath = listOf("kotlin-scripting-common.jar", "kotlin-scripting-misc.jar").map { File(kotlinPaths.libPath, it) }

    private fun createEnvironment(
        sources: List<String>, destDir: File, messageCollector: MessageCollector, confBody: CompilerConfiguration.() -> Unit
    ): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK).apply {
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            addKotlinSourceRoots(sources)
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destDir)
            confBody()
        }

        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    fun testScriptResolverEnvironmentArgsParsing() {

        val longStr = (1..100).joinToString { """\" $it aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \\""" }
        val unescapeRe = """\\(["\\])""".toRegex()
        val cmdlineProcessor = ScriptingCommandLineProcessor()
        val configuration = CompilerConfiguration()

        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION,
            """abc=def,11="ab cd \\ \"",long="$longStr"""",
            configuration
        )

        val res = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)

        Assert.assertEquals(
            hashMapOf("abc" to "def", "11" to "ab cd \\ \"", "long" to unescapeRe.replace(longStr, "\$1")),
            res
        )
    }

    fun testLazyScriptDefinition() {

        // Two tests in one function: the discovery code separately, and as a part of regular compilation
        // tests are combined to avoid double compilation of script definition modules

        val defsOut = File(tmpdir, "testLazyScriptDefinition/out/defs")
        val defsSrc = File(TEST_DATA_DIR, "lazyDefinitions/definitions")
        val scriptsOut = File(tmpdir, "testLazyScriptDefinition/out/scripts")
        val scriptsSrc = File(TEST_DATA_DIR, "lazyDefinitions/scripts")
        val scriptsOut2 = File(tmpdir, "testLazyScriptDefinition/out/scripts2")
        val defClasses = listOf("TestScriptWithReceivers", "TestScriptWithSimpleEnvVars")

        val messageCollector = TestMessageCollector()

        val definitionsCompileResult = KotlinToJVMBytecodeCompiler.compileBunchOfSources(
            createEnvironment(defClasses.map { File(defsSrc, "$it.kt").canonicalPath }, defsOut, messageCollector) {
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

        val lazyDefsSeq =
            discoverScriptTemplatesInClasspath(listOf(defsOut), emptyList(), messageCollector)

        assertTrue(messageCollector.messages.isEmpty()) {
            "Unexpected messages from discovery sequence (should be empty):\n$messageCollector"
        }

        val lazyDefs = lazyDefsSeq.toList()

        for (def in defClasses) {
            assertTrue(
                messageCollector.messages.any { it.message.contains("Configure scripting: Added template $def") }
            ) {
                "Missing messages from discovery sequence (should contain \"Added template $def\"):\n$messageCollector"
            }
            assertTrue(
                messageCollector.messages.none { it.message.contains("Configure scripting: loading script definition class $def") }
            ) {
                "Unexpected messages from discovery sequence (should not contain \"loading script definition class $def\"):\n$messageCollector"
            }
        }

        messageCollector.clear()

        val scriptFiles = scriptsSrc.listFiles { file: File -> file.extension == "kts" }.map { it.canonicalPath}

        val scriptsCompileEnv = createEnvironment(scriptFiles, scriptsOut, messageCollector) {
            addJvmClasspathRoots(runtimeClasspath)
            addJvmClasspathRoots(scriptingClasspath)
            addJvmClasspathRoot(defsOut)
            addAll(JVMConfigurationKeys.SCRIPT_DEFINITIONS, lazyDefs)
        }

        val res = KotlinToJVMBytecodeCompiler.compileBunchOfSources(scriptsCompileEnv)

        assertTrue(res) {
            "Failed to compile scripts:\n$messageCollector"
        }

        val cp = (runtimeClasspath + scriptingClasspath + defsOut).joinToString(File.pathSeparator)
        val exitCode = K2JVMCompiler().exec(System.err, "-cp", cp, *(scriptFiles.toTypedArray()), "-d", scriptsOut2.canonicalPath)

        Assert.assertEquals(ExitCode.OK, exitCode)
    }
}

class TestMessageCollector : MessageCollector {
    data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageLocation?)

    val messages = arrayListOf<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        messages.add(Message(severity, message, location))
    }

    override fun hasErrors(): Boolean = messages.any { it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR }

    override fun toString(): String {
        return messages.joinToString("\n") { "${it.severity}: ${it.message}${it.location?.let{" at $it"} ?: ""}" }
    }
}

fun TestMessageCollector.assertHasMessage(msg: String, desiredSeverity: CompilerMessageSeverity? = null) {
    assert(messages.any { it.message.contains(msg) && (desiredSeverity == null || it.severity == desiredSeverity) }) {
        "Expecting message \"$msg\" with severity ${desiredSeverity?.toString() ?: "Any"}, actual:\n" +
                messages.joinToString("\n") { it.severity.toString() + ": " + it.message }
    }
}

fun assertTrue(exp: Boolean, msg: () -> String) {
    if (!exp) {
        Assert.fail(msg())
    }
}