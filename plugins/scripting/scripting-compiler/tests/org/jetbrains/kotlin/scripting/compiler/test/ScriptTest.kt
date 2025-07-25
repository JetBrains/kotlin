/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.StandardScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.tryConstructClassFromStringArgs
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.test.*

class ScriptTest {
    @Test
    fun testStandardScriptWithParams() {
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition)
        assertNotNull(aClass)
        val out = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass, listOf("4", "comment"))
            assertNotNull(anObj)
        }
        assertEqualsTrimmed("$NUM_4_LINE (comment)$FIB_SCRIPT_OUTPUT_TAIL", out)
    }

    @Test
    fun testStandardScriptWithoutParams() {
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition)
        assertNotNull(aClass)
        val out = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass, emptyList())
            assertNotNull(anObj)
        }
        assertEqualsTrimmed("$NUM_4_LINE (none)$FIB_SCRIPT_OUTPUT_TAIL", out)
    }

    @Test
    fun testStandardScriptWithSaving(@TempDir tmpdir: File) {
        val aClass = compileScript("fib_std.kts", StandardScriptDefinition, saveClassesDir = tmpdir)
        assertNotNull(aClass)
        val out1 = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass, emptyList())
            assertNotNull(anObj)
        }
        assertEqualsTrimmed("$NUM_4_LINE (none)$FIB_SCRIPT_OUTPUT_TAIL", out1)
        val savedClassLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), aClass.classLoader)
        val aClassSaved = savedClassLoader.loadClass(aClass.name)
        assertNotNull(aClassSaved)
        val out2 = captureOut {
            val anObjSaved = tryConstructClassFromStringArgs(aClassSaved, emptyList())
            assertNotNull(anObjSaved)
        }
        assertEqualsTrimmed("$NUM_4_LINE (none)$FIB_SCRIPT_OUTPUT_TAIL", out2)
    }

    @Test
    fun testUseCompilerInternals() {
        val scriptClass = compileScript("use_compiler_internals.kts", StandardScriptDefinition)!!
        assertEquals("OK", captureOut {
            tryConstructClassFromStringArgs(scriptClass, emptyList())!!
        })
    }

    @Test
    fun testKt42530() {
        val aClass = compileScript("kt42530.kts", StandardScriptDefinition)
        assertNotNull(aClass)
        val out = captureOut {
            val anObj = tryConstructClassFromStringArgs(aClass, emptyList())
            assertNotNull(anObj)
        }
        assertEqualsTrimmed("[(1, a)]", out)
    }

    @Test
    fun testMetadataFlag() {
        // Test that we're writing the flag to [Metadata.extraInt] that distinguishes scripts from other classes.

        fun Class<*>.isFlagSet(): Boolean {
            val metadata = annotations.single { it.annotationClass.java.name == Metadata::class.java.name }
            val extraInt = metadata.javaClass.methods.single { it.name == JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME }
            return (extraInt(metadata) as Int) and JvmAnnotationNames.METADATA_SCRIPT_FLAG != 0
        }

        val scriptClass = compileScript("metadata_flag.kts", StandardScriptDefinition) ?: throw AssertionError("compilation failed")
        assertTrue(scriptClass.isFlagSet(), "Script class SHOULD have the metadata flag set")
        assertFalse(
            scriptClass.classLoader.loadClass("Metadata_flag\$RandomClass").isFlagSet(),
            "Non-script class in a script should NOT have the metadata flag set"
        )
    }

    private fun compileScript(
        scriptPath: String,
        scriptDefinition: KotlinScriptDefinition,
        runIsolated: Boolean = true,
        suppressOutput: Boolean = false,
        saveClassesDir: File? = null
    ): Class<*>? {
        val messageCollector =
            if (suppressOutput) MessageCollector.NONE
            else PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        val rootDisposable = Disposer.newDisposable("Disposable for ${ScriptTest::class.simpleName}")
        try {
            val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
            configuration.updateWithBaseCompilerArguments()
            configuration.messageCollector = messageCollector
            configuration.add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromLegacy(
                    defaultJvmScriptingHostConfiguration,
                    scriptDefinition
                )
            )
            if (saveClassesDir != null) {
                configuration.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, saveClassesDir)
            }

            loadScriptingPlugin(configuration, rootDisposable)

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            try {
                return compileScript(
                    File("plugins/scripting/scripting-compiler/testData/compiler/$scriptPath").toScriptSource(),
                    environment,
                    this::class.java.classLoader.takeUnless { runIsolated }
                ).first?.java
            } catch (e: CompilationException) {
                messageCollector.report(
                    CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element)
                )
                return null
            } catch (t: Throwable) {
                MessageCollectorUtil.reportException(messageCollector, t)
                throw t
            }
        } finally {
            disposeRootInWriteAction(rootDisposable)
        }
    }
}
