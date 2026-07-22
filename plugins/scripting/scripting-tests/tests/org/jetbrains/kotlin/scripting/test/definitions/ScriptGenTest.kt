/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definitions

import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptEvaluationConfigurationFromHostConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.ScriptTemplateDefinition

private const val testDataPath = "plugins/scripting/scripting-tests/testData/definitions/scriptCustom"

class ScriptGenTest : CodegenTestCase() {
    companion object {
        @Suppress("DEPRECATION")
        private val FIB_SCRIPT_DEFINITION =
            ScriptDefinition.FromConfigurations(
                defaultJvmScriptingHostConfiguration,
                org.jetbrains.kotlin.scripting.definitions.ScriptCompilationConfigurationFromLegacyTemplate(
                    defaultJvmScriptingHostConfiguration,
                    ScriptWithIntParam::class
                ),
                ScriptEvaluationConfigurationFromHostConfiguration(
                    defaultJvmScriptingHostConfiguration
                )
            )

        @Suppress("DEPRECATION")
        private val NO_PARAM_SCRIPT_DEFINITION =
            ScriptDefinition.FromConfigurations(
                defaultJvmScriptingHostConfiguration,
                org.jetbrains.kotlin.scripting.definitions.ScriptCompilationConfigurationFromLegacyTemplate(
                    defaultJvmScriptingHostConfiguration,
                    Any::class
                ),
                ScriptEvaluationConfigurationFromHostConfiguration(
                    defaultJvmScriptingHostConfiguration
                )
            )
    }

    @BeforeEach
    fun setUp() {
        additionalDependencies =
            System.getenv("PROJECT_CLASSES_DIRS")?.split(File.pathSeparator)?.map { File(it) }
                ?: listOf(
                    "compiler/build/classes/kotlin/test",
                    "build/compiler/classes/kotlin/test",
                    "out/test/compiler.test",
                    "out/test/compiler_test"
                )
                    .mapNotNull { File(it).canonicalFile.takeIf(File::isDirectory) }
                    .takeIf { it.isNotEmpty() }
                        ?: throw IllegalStateException("Unable to get classes output dirs, set PROJECT_CLASSES_DIRS environment variable")
    }

    override val firParser: FirParser
        get() = FirParser.Psi

    @Test
    fun testLanguage(): Unit = muteTest {
        setUpEnvironment("fib.lang.kts")

        val aClass = generateClass("Fib_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    @Test
    fun testLanguageWithPackage(): Unit = muteTest {
        setUpEnvironment("fibwp.lang.kts")

        val aClass = generateClass("test.Fibwp_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val script = constructor.newInstance(5)
        assertEquals(8, result.get(script))
    }

    @Test
    fun testDependentScripts(): Unit = muteTest {
        setUpEnvironment(listOf("fibwp.lang.kts", "fibwprunner.kts"))

        val aClass = generateClass("Fibwprunner")
        val constructor = aClass.getConstructor()
        val result = aClass.getDeclaredField("result")
        result.isAccessible = true
        val resultMethod = aClass.getDeclaredMethod("getResult")
        assertTrue(resultMethod.modifiers and Opcodes.ACC_FINAL != 0)
        assertTrue(resultMethod.modifiers and Opcodes.ACC_PUBLIC != 0)
        assertTrue(result.modifiers and Opcodes.ACC_PRIVATE != 0)
        val script = constructor.newInstance()
        assertEquals(8, result.get(script))
        assertEquals(8, resultMethod.invoke(script))
    }

    @Test
    fun testScriptWhereMethodHasClosure(): Unit = muteTest {
        setUpEnvironment("methodWithClosure.lang.kts")

        val aClass = generateClass("MethodWithClosure_lang")
        val constructor = aClass.getConstructor(Integer.TYPE)
        val script = constructor.newInstance(239)
        val fib = aClass.getMethod("method")
        val invoke = fib.invoke(script)
        assertEquals(239, invoke as Int / 2)
    }

    @Test
    fun testNameSanitation() {
        setUpEnvironment("1#@2.kts")

        val aClass = generateClass("_1__2")
        assertEquals("OK", aClass.getDeclaredMethod("getResult")(aClass.newInstance()))
    }

    private fun setUpEnvironment(sourcePath: String) {
        setUpEnvironment(listOf(sourcePath))
    }

    private fun setUpEnvironment(sourcePaths: List<String>) {
        val configuration = createConfiguration(
            ConfigurationKind.ALL, TestJdkKind.FULL_JDK, additionalDependencies
        ).apply {
            @OptIn(MessageCollectorAccess::class) // write access
            messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
            add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, FIB_SCRIPT_DEFINITION)
            add(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, NO_PARAM_SCRIPT_DEFINITION)

            addKotlinSourceRoots(sourcePaths.map { ForTestCompileRuntime.transformTestDataPath(testDataPath + File.separator + it).path })
        }
        loadScriptingPlugin(configuration, testRootDisposable)

        @OptIn(CoreEnvironmentDeprecation::class)
        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        loadScriptFiles(sourcePaths)
    }

    private fun loadScriptFiles(names: List<String>) {
        val baseDir = ForTestCompileRuntime.transformTestDataPath(testDataPath).path
        val files = names.map { name ->
            val content = KtTestUtil.doLoadFile(baseDir, name)
            KtTestUtil.createFile(name, content, myEnvironment!!.project)
        }
        myFiles = CodegenTestFiles.create(files)
    }

    private inline fun muteTest(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            return
        }
        throw AssertionError("Test could be unmuted")
    }
}

@Suppress("unused")
@ScriptTemplateDefinition(scriptFilePattern = ".*\\.lang\\.kts")
abstract class ScriptWithIntParam(val num: Int)
