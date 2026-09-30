/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definitions

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.scripting.compiler.plugin.requiresLegacyScriptRuntime
import org.jetbrains.kotlin.scripting.compiler.test.ScriptWithIntParam
import org.jetbrains.kotlin.scripting.definitions.ScriptCompilationConfigurationFromLegacyTemplate
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.test.runWithK2JVMCompiler
import org.jetbrains.kotlin.scripting.test.withTempDir
import java.io.File
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyScriptRuntimeTest {

    @Test
    fun testDefaultDefinitionDoesNotRequireScriptRuntime() {
        val default = ScriptDefinition.getDefault(defaultJvmScriptingHostConfiguration)
        assertFalse(default.compilationConfiguration.requiresLegacyScriptRuntime)
    }

    @Test
    fun testStandardTemplateBaseClassRequiresScriptRuntime() {
        val configuration = ScriptCompilationConfiguration {
            baseClass(KotlinType("kotlin.script.templates.standard.ScriptTemplateWithArgs"))
        }
        assertTrue(configuration.requiresLegacyScriptRuntime)
    }

    @Test
    fun testLegacyTemplateRequiresScriptRuntime() {
        @Suppress("DEPRECATION")
        val configuration = ScriptCompilationConfigurationFromLegacyTemplate(defaultJvmScriptingHostConfiguration, ScriptWithIntParam::class)
        assertTrue(configuration.requiresLegacyScriptRuntime)
    }

    @Test
    fun testClasspathDiscoveredLegacyTemplateGetsScriptRuntime() = withTempDir { dir ->
        compileScriptWithDiscoveredLegacyTemplate(dir, noStdlib = false, expectedExitCode = ExitCode.OK)
    }

    @Test
    fun testNoStdlibDisablesScriptRuntimeForDiscoveredLegacyTemplate() = withTempDir { dir ->
        compileScriptWithDiscoveredLegacyTemplate(
            dir, noStdlib = true, expectedExitCode = ExitCode.COMPILATION_ERROR,
            expectedErrPatterns = listOf(".*cannot access 'kotlin\\.script\\.templates\\.standard\\.ScriptTemplateWithArgs'.*"),
        )
    }

    private fun compileScriptWithDiscoveredLegacyTemplate(
        dir: File,
        noStdlib: Boolean,
        expectedExitCode: ExitCode,
        expectedErrPatterns: List<String>? = null,
    ) {
        val templateDir = File(dir, "template")
        val templateSource = File(dir, "LegacyTemplate.kt").apply {
            writeText(
                """
                package legacy
                @Suppress("DEPRECATION")
                @kotlin.script.templates.ScriptTemplateDefinition(scriptFilePattern = ".*\\.legacy\\.kts")
                abstract class LegacyTemplate(args: Array<String>) : kotlin.script.templates.standard.ScriptTemplateWithArgs(args) {
                    val fromLegacyTemplate = 42
                }
                """.trimIndent()
            )
        }
        runWithK2JVMCompiler(
            arrayOf(
                K2JVMCompilerArguments::kotlinHome.cliArgument, ForTestCompileRuntime.distKotlincForTests().path,
                K2JVMCompilerArguments::classpath.cliArgument, ForTestCompileRuntime.scriptRuntimeJarForTests().path,
                K2JVMCompilerArguments::destination.cliArgument, templateDir.path,
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                templateSource.path,
            )
        )
        File(templateDir, "META-INF/kotlin/script/templates/legacy.LegacyTemplate").apply { parentFile.mkdirs() }.writeText("")

        val script = File(dir, "hello.legacy.kts").apply { writeText("val argsCount = args.size + fromLegacyTemplate") }
        val classpath = listOf(ForTestCompileRuntime.runtimeJarForTests(), templateDir)
        runWithK2JVMCompiler(
            arrayOf(
                K2JVMCompilerArguments::kotlinHome.cliArgument, ForTestCompileRuntime.distKotlincForTests().path,
                K2JVMCompilerArguments::classpath.cliArgument, classpath.joinToString(File.pathSeparator),
                K2JVMCompilerArguments::destination.cliArgument, File(dir, "out").path,
                CommonCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument,
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                *(if (noStdlib) arrayOf(K2JVMCompilerArguments::noStdlib.cliArgument) else emptyArray()),
                script.path,
            ),
            expectedExitCode = expectedExitCode.code,
            expectedSomeErrPatterns = expectedErrPatterns,
        )
    }
}
