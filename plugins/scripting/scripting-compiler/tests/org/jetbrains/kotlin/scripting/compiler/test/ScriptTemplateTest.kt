/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("unused", "DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.assertHasMessage
import org.jetbrains.kotlin.scripting.compiler.plugin.expectTestToFailOnK2
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptCompilationConfigurationFromLegacyTemplate
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptEvaluationConfigurationFromHostConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.tryConstructClassFromStringArgs
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// TODO: the contents of this file should go into ScriptTest.kt and replace appropriate xml-based functionality,
// as soon as the the latter is removed from the codebase

class ScriptTemplateTest {
    @Test
    fun testScriptWithParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithClassParameter() {
        val messageCollector = MessageCollectorImpl()
        val aClass =
            compileScript("fib_cp.kts", ScriptWithClassParam::class, runIsolated = false, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            val testParamClass = aClass.classLoader.loadClass("org.jetbrains.kotlin.scripting.compiler.test.TestParamClass")
            aClass.getConstructor(testParamClass).newInstance(testParamClass.constructors.first().newInstance(4))
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithBaseClassWithParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass =
            compileScript("fib_dsl.kts", ScriptWithBaseClass::class, runIsolated = false, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE, Integer.TYPE).newInstance(4, 1)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithDependsAnn() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib_ext_ann.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithDependsAnn2() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib_ext_ann2.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testScriptWithoutParams() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("without_params.kts", ScriptWithoutParams::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("10", out)
    }

    @Test
    fun testScriptWithOverriddenParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript(
            "overridden_parameter.kts",
            ScriptBaseClassWithOverriddenProperty::class,
            messageCollector = messageCollector
        )
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed("14", out)
    }

    @Test
    fun testScriptWithArrayParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("array_parameter.kts", ScriptWithArrayParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Array<String>::class.java).newInstance(arrayOf("one", "two"))
        }.let {
            assertEqualsTrimmed("one and two", it)
        }
    }

    @Test
    fun testScriptWithNullableParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("nullable_parameter.kts", ScriptWithNullableParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Int::class.javaObjectType).newInstance(null)
        }.let {
            assertEqualsTrimmed("Param is null", it)
        }
    }

    @Test
    fun testScriptVarianceParams() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("variance_parameters.kts", ScriptVarianceParams::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Array<in Number>::class.java, Array<out Number>::class.java).newInstance(arrayOf("one"), arrayOf(1, 2))
        }.let {
            assertEqualsTrimmed("one and 1", it)
        }
    }

    @Test
    fun testScriptWithNullableProjection() {
        val messageCollector = MessageCollectorImpl()
        val aClass =
            compileScript("nullable_projection.kts", ScriptWithNullableProjection::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Array<String>::class.java).newInstance(arrayOf<String?>(null))
        }.let {
            assertEqualsTrimmed("nullable", it)
        }
    }

    @Test
    fun testScriptWithArray2DParam() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("array2d_param.kts", ScriptWithArray2DParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Array<Array<in String>>::class.java).newInstance(arrayOf(arrayOf("one"), arrayOf("two")))
        }.let {
            assertEqualsTrimmed("first: one, size: 1", it)
        }
    }

    @Test
    fun testScriptWithStandardTemplate() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib_std.kts", ScriptTemplateWithArgs::class, runIsolated = false, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Array<String>::class.java).newInstance(arrayOf("4", "other"))
        }.let {
            assertEqualsTrimmed("$NUM_4_LINE (other)$FIB_SCRIPT_OUTPUT_TAIL", it)
        }
    }

    @Test
    fun testScriptWithPackage() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.pkg.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptWithScriptDefinition() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    // Fails on K2, see KT-62413
    @Test
    fun testScriptWithParamConversion() = expectTestToFailOnK2 {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithIntParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass, listOf("4"))
            assertNotNull(anObj)
        }.let {
            assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, it)
        }
    }

    @Test
    fun testScriptErrorReporting() {
        val messageCollector = MessageCollectorImpl()
        compileScript("fib.kts", ScriptReportingErrors::class, messageCollector = messageCollector)

        messageCollector.assertHasMessage("error", desiredSeverity = CompilerMessageSeverity.ERROR)
        messageCollector.assertHasMessage("warning", desiredSeverity = CompilerMessageSeverity.WARNING)
        messageCollector.assertHasMessage("info", desiredSeverity = CompilerMessageSeverity.INFO)
        messageCollector.assertHasMessage("debug", desiredSeverity = CompilerMessageSeverity.LOGGING)
    }

    @Test
    fun testAsyncResolver() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithAsyncResolver::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        val out = captureOut {
            aClass.getConstructor(Integer.TYPE).newInstance(4)
        }
        assertEqualsTrimmed(NUM_4_LINE + FIB_SCRIPT_OUTPUT_TAIL, out)
    }

    @Test
    fun testAcceptedAnnotationsSync() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsSyncResolver::class,
            messageCollector = messageCollector
        )
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    @Test
    fun testAcceptedAnnotationsAsync() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsAsyncResolver::class,
            messageCollector = messageCollector
        )
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    @Test
    fun testAcceptedAnnotationsLegacy() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript(
            "acceptedAnnotations.kts",
            ScriptWithAcceptedAnnotationsLegacyResolver::class,
            messageCollector = messageCollector
        )
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    @Test
    fun testSeveralConstructors() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithSeveralConstructorsResolver::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    @Test
    fun testConstructorWithDefaultArgs() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("fib.kts", ScriptWithDefaultArgsResolver::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    @Test
    fun testThrowing() {
        val messageCollector = MessageCollectorImpl()
        compileScript("fib.kts", ScriptWithThrowingResolver::class, messageCollector = messageCollector)

        messageCollector.assertHasMessage(
            "Failed to resolve dependencies. resolver=ThrowingResolver() of type=ThrowingResolver",
            desiredSeverity = CompilerMessageSeverity.ERROR
        )
    }

    @Test
    fun testSmokeScriptException() {
        val messageCollector = MessageCollectorImpl()
        val aClass = compileScript("smoke_exception.kts", ScriptWithArrayParam::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
        var exceptionThrown = false
        try {
            tryConstructClassFromStringArgs(aClass, emptyList())
        } catch (e: InvocationTargetException) {
            assertTrue(e.cause is IllegalStateException)
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun testScriptWithNoMatchingTemplate() {
        val messageCollector = MessageCollectorImpl()
        val aClass =
            compileScript("without_params.kts", ScriptWithDifferentFileNamePattern::class, messageCollector = messageCollector)
        assertNotNull(aClass, "Compilation failed:\n$messageCollector")
    }

    private fun compileScript(
        scriptPath: String,
        scriptTemplate: KClass<out Any>,
        runIsolated: Boolean = true,
        messageCollector: MessageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false),
        includeKotlinRuntime: Boolean = true,
    ): Class<*>? {
        val rootDisposable = Disposer.newDisposable("Disposable for ${ScriptTemplateTest::class.simpleName}")
        try {
            val additionalClasspath = buildList {
                System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator).orEmpty()
                    .map { File(it) }
                    .filterTo(this) { file -> file.exists() }
                add(ForTestCompileRuntime.scriptingTestsRuntimeClasspathForTests())
            }
            val configuration = KotlinTestUtils.newConfiguration(
                if (includeKotlinRuntime) ConfigurationKind.ALL else ConfigurationKind.JDK_ONLY,
                TestJdkKind.FULL_JDK,
                *additionalClasspath.toTypedArray()
            ).apply {
                useFir = true
            }
            configuration.updateWithBaseCompilerArguments()
            @OptIn(MessageCollectorAccess::class) // write access
            configuration.messageCollector = messageCollector
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromConfigurations(
                    defaultJvmScriptingHostConfiguration,
                    ScriptCompilationConfigurationFromLegacyTemplate(
                        defaultJvmScriptingHostConfiguration,
                        scriptTemplate
                    ),
                    ScriptEvaluationConfigurationFromHostConfiguration(
                        defaultJvmScriptingHostConfiguration
                    )
                )
            )
            configuration.put(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION, true)

            val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
                    System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true

            if (isK2) {
                configuration.put(CommonConfigurationKeys.USE_FIR, true)
            }

            loadScriptingPlugin(configuration, rootDisposable)

            @OptIn(CoreEnvironmentDeprecation::class)
            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            return try {
                val res = compileScript(
                    ForTestCompileRuntime.transformTestDataPath("plugins/scripting/scripting-compiler/testData/compiler/$scriptPath")
                        .toScriptSource(),
                    environment,
                    this::class.java.classLoader.takeUnless { runIsolated }
                )
                res.first?.java
            } catch (e: CompilationException) {
                messageCollector.report(
                    CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element)
                )
                null
            } catch (e: IllegalStateException) {
                messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e))
                null
            } catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }
        } finally {
            disposeRootInWriteAction(rootDisposable)
        }
    }
}
