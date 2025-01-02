/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.REPL_SNIPPET_EVAL_FUN_NAME
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.computeTestRuntimeClasspath
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirReplFrontendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirScriptAndReplCodegenTest
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator.Companion.TEST_CONFIGURATION_KIND_KEY
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

open class AbstractReplWithTestExtensionsDiagnosticsTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        baseFirDiagnosticTestConfiguration(frontendFacade = ::FirReplFrontendFacade)
        enableLazyResolvePhaseChecking()
        configureFirParser(FirParser.Psi)
        useConfigurators(
            ::ReplConfigurator
        )
        defaultDirectives {
            +WITH_STDLIB
        }
    }
}

open class AbstractReplWithTestExtensionsCodegenTest : AbstractFirScriptAndReplCodegenTest(::FirReplFrontendFacade) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(
                ::ReplConfigurator
            )
            defaultDirectives {
                +WITH_STDLIB
            }
            irHandlersStep {
                useHandlers(::IrDiagnosticsHandler)
            }
            jvmArtifactsHandlersStep {
                useHandlers(
                    ::ReplRunChecker
                )
            }
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class ReplConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
            repl {
                firReplHistoryProvider(FirReplHistoryProviderImpl())
                isReplSnippetSource { sourceFile, scriptSource -> true }
            }
        }
        configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ReplCompilerPluginRegistrar(hostConfiguration))
    }
}

private class ReplRunChecker(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {

    private var scriptProcessed = false
    private var currentReplClassloader: GeneratedClassLoader? = null
    private val classLoadersToDispose: MutableList<GeneratedClassLoader> = mutableListOf()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val fileInfos = info.fileInfos.ifEmpty { return }
        currentReplClassloader = generatedReplSnippetTestClassLoader(testServices, module, info.classFileFactory, currentReplClassloader)
        for (fileInfo in fileInfos) {
            when (val sourceFile = fileInfo.sourceFile) {
                is KtPsiSourceFile -> (sourceFile.psiFile as? KtFile)?.let { ktFile ->
                    ktFile.script?.fqName?.let { scriptFqName ->
                        runAndCheckSnippet(ktFile, scriptFqName, currentReplClassloader!!)
                        scriptProcessed = true
                    }
                }
                else -> {
                    assertions.fail { "Only PSI scripts are supported so far" }
                }
            }
        }
    }

    private fun generatedReplSnippetTestClassLoader(
        testServices: TestServices,
        module: TestModule,
        classFileFactory: ClassFileFactory,
        previousSnippetClassLoader: GeneratedClassLoader?,
    ): GeneratedClassLoader {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val classpath = computeTestRuntimeClasspath(testServices, module)
        if (previousSnippetClassLoader == null) {
            val libPathProvider = testServices.standardLibrariesPathProvider
            classpath += libPathProvider.runtimeJarForTests()
            if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
                classpath += libPathProvider.reflectJarForTests()
            }
            classpath += libPathProvider.scriptRuntimeJarForTests()
            classpath += libPathProvider.kotlinTestJarForTests()
        }
        return GeneratedClassLoader(classFileFactory, previousSnippetClassLoader, *classpath.map { it.toURI().toURL() }.toTypedArray()).also {
            classLoadersToDispose.add(it)
        }
    }

    private fun runAndCheckSnippet(
        ktFile: KtFile,
        scriptFqName: FqName,
        classLoader: GeneratedClassLoader,
    ) {
        val expectedOut = Regex("// EXPECTED_OUT: (.*)").findAll(ktFile.text).map {
            it.groups[1]!!.value
        }.joinToString("\n")
        val expected = Regex("// EXPECTED: (\\S+) *== *\"?([^\"]*)\"?").findAll(ktFile.text).map {
            it.groups[1]!!.value to it.groups[2]!!.value
        }

        val evalFunName = REPL_SNIPPET_EVAL_FUN_NAME.asString()
        val snippetClass = classLoader.loadClass(scriptFqName.asString())
        val eval = snippetClass.methods.find { it.name == evalFunName }!!

        val snippet = snippetClass.getField("INSTANCE").get(null)
        val (out, _, res) = captureOutErrRet {
            eval.invoke(snippet)
        }

        for ((fieldName, expectedValue) in expected) {
            if (expectedValue == "<missing>") {
                try {
                    snippetClass.getDeclaredField(fieldName)
                    assertions.fail { "must have no field $fieldName" }
                } catch (_: NoSuchFieldException) {
                    continue
                }
            }
            val result = if (fieldName == "<res>") {
                res
            } else {
                val field = snippetClass.getDeclaredField(fieldName)
                field.isAccessible = true
                field[snippet]
            }
            val resultString = result?.toString() ?: "null"
            assertions.assertEquals(expectedValue.trim(), resultString) { "comparing variable $fieldName" }
        }

        assertions.assertEquals(expectedOut, out)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        classLoadersToDispose.forEach { it.dispose() }
        if (!scriptProcessed) {
            assertions.fail { "Can't find script to test" }
        }
    }
}

internal fun <T> captureOutErrRet(body: () -> T): Triple<String, String, T> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    val ret = try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return Triple(outStream.toString().trim(), errStream.toString().trim(), ret)
}
