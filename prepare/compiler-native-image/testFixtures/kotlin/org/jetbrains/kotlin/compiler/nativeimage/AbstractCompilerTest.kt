/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TAIL_CALL_OPTIMIZATION
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TYPE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TYPE_WITH_EXACT
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.INFERENCE_HELPERS
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractCompilerTest(private val runner: CompilerRunner) {
    @TempDir
    lateinit var workingDir: File

    protected val compilationClasspath: List<File> by lazy {
        listOf(
            ForTestCompileRuntime.runtimeJarForTests(),
            ForTestCompileRuntime.kotlinTestJarForTests(),
        )
    }

    protected val reflectClasspath: File by lazy { ForTestCompileRuntime.reflectJarForTests() }

    private val mockJdkRtJar: File by lazy { KtTestUtil.findMockJdkRtJar() }

    fun runTest(filePath: String) {
        val testFile = ForTestCompileRuntime.transformTestDataPath(filePath)
        val source = testFile.readText()
        val directives = parseDirectives(source)

        val skipReason = shouldSkip(source, directives)
        assumeTrue(skipReason == null) { "skipped: $skipReason" }

        val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in directives
        val withFullJdk = JvmEnvironmentConfigurationDirectives.FULL_JDK in directives

        val outDir = File(workingDir, "ni-out").apply { mkdirs() }

        val sourceFile = prepareSourceFile(source)
        val compilerInvocationResult = runner.run(
            workingDir = workingDir,
            arguments = buildCompilerArgs(sourceFile, outDir, directives, withFullJdk),
            classpath = buildClasspath(withReflect, withFullJdk),
        )
        checkCompilationResult(compilerInvocationResult, outDir, source, withReflect)
    }

    protected abstract fun prepareSourceFile(source: String): File

    protected abstract fun checkCompilationResult(
        result: CompilerInvocationResult,
        outDir: File,
        source: String,
        withReflect: Boolean,
    )

    protected open fun shouldSkip(source: String, directives: RegisteredDirectives): String? = null

    protected open fun buildCompilerArgs(
        testFile: File,
        outDir: File,
        directives: RegisteredDirectives,
        withFullJdk: Boolean,
    ): List<String> = buildList {
        directives[LanguageSettingsDirectives.LANGUAGE].forEach { add("-XXLanguage:$it") }
        addAll(directives.valueDirectiveFlags())
        directives[LanguageSettingsDirectives.OPT_IN].forEach { add("-opt-in=$it") }
        if (!withFullJdk) add("-no-jdk")
        for ([directive, path] in HELPER_FILES) {
            if (directive in directives) {
                add(materializeHelperFile(path).absolutePath)
            }
        }
        add(testFile.absolutePath)
        add("-d")
        add(outDir.absolutePath)
    }

    protected open fun buildClasspath(withReflect: Boolean, withFullJdk: Boolean): List<File> = buildList {
        addAll(compilationClasspath)
        if (withReflect) add(reflectClasspath)
        if (!withFullJdk) add(mockJdkRtJar)
    }

    private fun parseDirectives(source: String): RegisteredDirectives {
        val parser = RegisteredDirectivesParser(DIRECTIVES_CONTAINER, JUnit5Assertions)
        for (line in source.lineSequence()) {
            if (line.startsWith("//")) parser.parse(line)
        }
        return parser.build()
    }

    private fun RegisteredDirectives.valueDirectiveFlags(): List<String> = listOfNotNull(
        renderValueFlag(LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE, "-Xreturn-value-checker") { it.state },
        renderValueFlag(JvmEnvironmentConfigurationDirectives.ASSERTIONS_MODE, "-Xassertions") { it.description },
        renderValueFlag(JvmEnvironmentConfigurationDirectives.LAMBDAS, "-Xlambdas") { it.description },
        renderValueFlag(JvmEnvironmentConfigurationDirectives.SAM_CONVERSIONS, "-Xsam-conversions") { it.description },
    )

    private inline fun <T : Any> RegisteredDirectives.renderValueFlag(
        directive: ValueDirective<T>,
        flagPrefix: String,
        render: (T) -> String,
    ): String? = this[directive].singleOrNull()?.let { "$flagPrefix=${render(it)}" }

    private fun materializeHelperFile(relativePath: String): File {
        val resource = this::class.java.classLoader.getResource(relativePath)
            ?: error("Helper file resource not found: $relativePath")
        return workingDir.resolve(relativePath).also {
            it.parentFile.mkdirs()
            it.writeText(resource.readText())
        }
    }

    companion object {
        private const val HELPERS_PATH = "diagnostics/helpers"

        private val HELPER_FILES: Map<SimpleDirective, String> = mapOf(
            CHECK_TYPE to "${HELPERS_PATH}/types/checkType.kt",
            CHECK_TYPE_WITH_EXACT to "${HELPERS_PATH}/types/checkTypeWithExact.kt",
            INFERENCE_HELPERS to "${HELPERS_PATH}/inference/inferenceUtils.kt",
            WITH_COROUTINES to "${HELPERS_PATH}/coroutines/CoroutineHelpers.kt",
            CHECK_STATE_MACHINE to "${HELPERS_PATH}/coroutines/StateMachineChecker.kt",
            CHECK_TAIL_CALL_OPTIMIZATION to "${HELPERS_PATH}/coroutines/TailCallOptimizationChecker.kt",
        )

        private val DIRECTIVES_CONTAINER = ComposedDirectivesContainer(
            LanguageSettingsDirectives,
            ConfigurationDirectives,
            CodegenTestDirectives,
            JvmEnvironmentConfigurationDirectives,
            AdditionalFilesDirectives,
            NativeImagePluginDirectives,
        )
    }
}
