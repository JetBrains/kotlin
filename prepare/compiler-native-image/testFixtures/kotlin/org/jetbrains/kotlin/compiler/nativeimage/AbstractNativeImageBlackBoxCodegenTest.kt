/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_STATE_MACHINE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TAIL_CALL_OPTIMIZATION
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TYPE
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.CHECK_TYPE_WITH_EXACT
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.INFERENCE_HELPERS
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.preprocessors.JvmInlineSourceTransformer
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractNativeImageBlackBoxCodegenTest {
    @TempDir
    lateinit var workingDir: File

    private val nativeImageDist: File by lazy { ForTestCompileRuntime.kotlinNativeImageDistForTests() }

    private val nativeImageExecutable: File by lazy {
        val launcher = when {
            System.getProperty("os.name").startsWith("windows", ignoreCase = true) -> "kotlinc-native-image.bat"
            else -> "kotlinc-native-image.sh"
        }
        nativeImageDist.resolve("bin").resolve(launcher)
    }

    private val javaHome: String = System.getProperty("java.home")

    private val compilationClasspath: List<File> by lazy {
        listOf(
            ForTestCompileRuntime.runtimeJarForTests(),
            ForTestCompileRuntime.kotlinTestJarForTests(),
        )
    }

    private val reflectClasspath: File by lazy { ForTestCompileRuntime.reflectJarForTests() }

    private val mockJdkRtJar: File by lazy { KtTestUtil.findMockJdkRtJar() }

    open fun runTest(filePath: String) {
        val testFile = ForTestCompileRuntime.transformTestDataPath(filePath)
        val source = testFile.readText()
        val directives = parseDirectives(source)

        val skipReason = shouldSkip(source, directives)
        assumeTrue(skipReason == null) { "skipped: $skipReason" }

        val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in directives
        val withFullJdk = JvmEnvironmentConfigurationDirectives.FULL_JDK in directives

        val boxFile = File(workingDir, "box.kt").apply { writeText(prepareSource(source)) }
        val outDir = File(workingDir, "ni-out").apply { mkdirs() }

        val (exitCode, compilerStdout) = runNativeImageCompiler(
            arguments = buildCompilerArgs(boxFile, outDir, directives, withFullJdk),
            classpath = buildClasspath(withReflect, withFullJdk),
        )
        assertEquals(0, exitCode, "native-image compilation failed:\n$compilerStdout")

        val result = invokeBox(outDir, boxClassName(source), withReflect)
        assertEquals("OK", result, "box() != 'OK'")
    }

    private fun runNativeImageCompiler(
        arguments: List<String>,
        classpath: List<File>,
    ): Pair<Int, String> {
        val cmd = listOf(
            nativeImageExecutable.absolutePath,
            "-Dkotlinc.test.allow.testonly.language.features=true",
            "-cp", classpath.joinToString(File.pathSeparator),
        ) + arguments
        val builder = ProcessBuilder(cmd).directory(workingDir).redirectErrorStream(true)
        builder.environment().putIfAbsent("JAVA_HOME", javaHome)
        val proc = builder.start()
        val out = proc.inputStream.reader().use { it.readText() }
        return proc.waitFor() to out
    }

    private fun buildCompilerArgs(
        boxFile: File,
        outDir: File,
        directives: RegisteredDirectives,
        withFullJdk: Boolean,
    ): List<String> = buildList {
        directives[LanguageSettingsDirectives.LANGUAGE].forEach { add("-XXLanguage:$it") }
        addAll(directives.valueDirectiveFlags())
        directives[LanguageSettingsDirectives.OPT_IN].forEach { add("-opt-in=$it") }
        if (!withFullJdk) add("-no-jdk")
        for ((directive, path) in HELPER_FILES) {
            if (directive in directives) {
                add(materializeHelperFile(path).absolutePath)
            }
        }
        add(boxFile.absolutePath)
        add("-d")
        add(outDir.absolutePath)
    }

    private fun buildClasspath(withReflect: Boolean, withFullJdk: Boolean): List<File> = buildList {
        addAll(compilationClasspath)
        if (withReflect) add(reflectClasspath)
        if (!withFullJdk) add(mockJdkRtJar)
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

    private fun invokeBox(classDir: File, boxClass: String, withReflect: Boolean): String? {
        URLClassLoader(arrayOf(classDir.toURI().toURL()), sharedRuntimeLoader(withReflect)).use { loader ->
            val method = loader.loadClass(boxClass).getMethod("box")
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = loader
            return try {
                method.invoke(null) as? String
            } catch (e: InvocationTargetException) {
                throw e.cause ?: e
            } finally {
                thread.contextClassLoader = previous
            }
        }
    }

    private fun materializeHelperFile(relativePath: String): File {
        val resource = this::class.java.classLoader.getResource(relativePath)
            ?: error("Helper file resource not found: $relativePath")
        return workingDir.resolve(relativePath).also {
            it.parentFile.mkdirs()
            it.writeText(resource.readText())
        }
    }

    private fun sharedRuntimeLoader(withReflect: Boolean): URLClassLoader =
        sharedRuntimeLoaders.computeIfAbsent(withReflect) {
            val cp = compilationClasspath + if (withReflect) listOf(reflectClasspath) else emptyList()
            URLClassLoader(
                cp.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader().parent,
            )
        }

    companion object {
        private val BACKEND = TargetBackend.JVM_IR

        private val sharedRuntimeLoaders = ConcurrentHashMap<Boolean, URLClassLoader>()

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
        )

        private val MULTI_FILE_MARKER = Regex("""(?m)^// FILE:""")
        private val HELPERS_IMPORT = Regex("""(?m)^import helpers\.""")

        private fun prepareSource(source: String): String =
            clearTextFromDiagnosticMarkup(JvmInlineSourceTransformer.computeModifier(BACKEND).invoke(source))

        private fun parseDirectives(source: String): RegisteredDirectives {
            val parser = RegisteredDirectivesParser(DIRECTIVES_CONTAINER, JUnit5Assertions)
            for (line in source.lineSequence()) {
                if (line.startsWith("//")) parser.parse(line)
            }
            return parser.build()
        }

        private fun shouldSkip(source: String, directives: RegisteredDirectives): String? = when {
            MULTI_FILE_MARKER.containsMatchIn(source) -> "multi-file (// FILE:) tests are not supported"
            HELPERS_IMPORT.containsMatchIn(source) -> "tests importing helpers.* are not supported"
            "+MultiPlatformProjects" in directives[LanguageSettingsDirectives.LANGUAGE] -> "multiplatform projects are not supported"
            isBackendIgnored(directives) -> "ignored on $BACKEND via directive"
            else -> null
        }

        private fun isBackendIgnored(directives: RegisteredDirectives): Boolean {
            fun ValueDirective<TargetBackend>.matchesIgnore() =
                directives[this].any { TargetBackend.ANY == it || BACKEND.isTransitivelyCompatibleWith(it) }
            if (CodegenTestDirectives.IGNORE_BACKEND.matchesIgnore()) return true
            if (CodegenTestDirectives.IGNORE_BACKEND_K2.matchesIgnore()) return true
            return !InTextDirectivesUtils.isCompatibleTarget(
                BACKEND,
                directives[ConfigurationDirectives.TARGET_BACKEND],
                directives[ConfigurationDirectives.DONT_TARGET_EXACT_BACKEND],
            )
        }

        private const val BOX_FILE_CLASS = "BoxKt"

        private fun boxClassName(source: String): String {
            val pkg = source.lineSequence()
                .firstOrNull { it.trimStart().startsWith("package ") }
                ?.substringAfter("package ")
                ?.trim()
            return if (pkg.isNullOrBlank()) BOX_FILE_CLASS else "$pkg.$BOX_FILE_CLASS"
        }
    }
}
