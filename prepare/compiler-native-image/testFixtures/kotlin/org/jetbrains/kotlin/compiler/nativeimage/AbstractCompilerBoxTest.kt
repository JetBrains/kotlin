/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.preprocessors.JvmInlineSourceTransformer
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs the box tests: compiles the preprocessed test file and invokes its `box()` function
 */
abstract class AbstractCompilerBoxTest(
    runner: CompilerRunner,
) : AbstractCompilerTest(runner) {

    override fun prepareSourceFile(source: String): File =
        File(workingDir, "box.kt").apply { writeText(prepareSource(source)) }

    override fun checkCompilationResult(
        result: CompilerInvocationResult,
        outDir: File,
        source: String,
        withReflect: Boolean,
    ) {
        assertEquals(0, result.exitCode, "compilation failed:\n${result.output}")

        val result = invokeBox(outDir, boxClassName(source), withReflect)
        assertEquals("OK", result, "box() != 'OK'")
    }

    override fun shouldSkip(source: String, directives: RegisteredDirectives): String? = when {
        MULTI_FILE_MARKER.containsMatchIn(source) -> "multi-file (// FILE:) tests are not supported"
        HELPERS_IMPORT.containsMatchIn(source) -> "tests importing helpers.* are not supported"
        "+MultiPlatformProjects" in directives[LanguageSettingsDirectives.LANGUAGE] -> "multiplatform projects are not supported"
        isBackendIgnored(directives) -> "ignored on $BACKEND via directive"
        else -> null
    }

    protected open fun runtimeClasspath(withReflect: Boolean): List<File> =
        if (withReflect) listOf(reflectClasspath) else emptyList()

    private fun invokeBox(classDir: File, boxClass: String, withReflect: Boolean): String? {
        URLClassLoader(
            arrayOf(classDir.toURI().toURL()),
            sharedRuntimeLoader(runtimeClasspath(withReflect))
        ).use { loader ->
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

    private fun sharedRuntimeLoader(runtimeClasspath: List<File>): URLClassLoader {
        val classpath = compilationClasspath + runtimeClasspath
        return sharedRuntimeLoaders.computeIfAbsent(classpath) {
            URLClassLoader(
                classpath.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader().parent,
            )
        }
    }

    companion object {
        private val BACKEND = TargetBackend.JVM_IR

        private val sharedRuntimeLoaders = ConcurrentHashMap<List<File>, URLClassLoader>()

        private val MULTI_FILE_MARKER = Regex("""(?m)^// FILE:""")
        private val HELPERS_IMPORT = Regex("""(?m)^import helpers\.""")

        private const val BOX_FILE_CLASS = "BoxKt"

        private fun prepareSource(source: String): String {
            val transformers = listOf(
                JvmInlineSourceTransformer.computeModifier(BACKEND),
                ReplacingSourceTransformer("BACKEND_UNDER_TEST", "\"$BACKEND\""),
            )
            return clearTextFromDiagnosticMarkup(transformers.fold(source) { acc, transformer -> transformer.invokeForTestFile(acc) })
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

        private fun boxClassName(source: String): String {
            val pkg = source.lineSequence()
                .firstOrNull { it.trimStart().startsWith("package ") }
                ?.substringAfter("package ")
                ?.trim()
            return if (pkg.isNullOrBlank()) BOX_FILE_CLASS else "$pkg.$BOX_FILE_CLASS"
        }
    }
}
