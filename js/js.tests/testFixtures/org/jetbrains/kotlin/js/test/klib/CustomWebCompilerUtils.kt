/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URLClassLoader

internal object JsKlibTestSettings {
    val customJsCompilerArtifacts: CustomJsCompilerArtifacts by lazy {
        val artifactsDir: String? = System.getProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir")
        requireNotNull(artifactsDir) { "Custom compiler location is not specified" }

        val version: String? = System.getProperty("kotlin.internal.js.test.compat.customCompilerVersion")
        requireNotNull(version) { "Custom compiler version is not specified" }

        CustomJsCompilerArtifacts(artifactsDir = File(artifactsDir), version)
    }

    val customJsCompiler by lazy {
        CustomJsCompiler(customJsCompilerArtifacts)
    }
}

internal class CustomJsCompilerArtifacts(artifactsDir: File, val version: String) {
    val compilerEmbeddable: File = artifactsDir.resolve("kotlin-compiler-embeddable-$version.jar")

    val baseStdLib: File = artifactsDir.resolve("kotlin-stdlib-$version.jar")

    val jsStdLib: File = artifactsDir.resolve("kotlin-stdlib-js-$version.klib")
}

internal class CustomJsCompiler(private val customWebCompilerArtifacts: CustomJsCompilerArtifacts) {
    private var isolatedClassLoaderSoftRef: SoftReference<URLClassLoader>? = null

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaderSoftRef?.get()
        ?: synchronized(this) {
            isolatedClassLoaderSoftRef?.get()
                ?: createIsolatedClassLoader(customWebCompilerArtifacts).also { isolatedClassLoaderSoftRef = SoftReference(it) }
        }

    fun callCompiler(args: Array<String>, output: PrintStream): ExitCode {
        val isolatedClassLoader = getIsolatedClassLoader()

        val compilerClass = Class.forName("org.jetbrains.kotlin.cli.js.K2JSCompiler", true, isolatedClassLoader)
        val entryPoint = compilerClass.getMethod(
            "execFullPathsInMessages",
            PrintStream::class.java,
            Array<String>::class.java
        )

        val compilerInstance = compilerClass.getDeclaredConstructor().newInstance()
        val result = entryPoint.invoke(compilerInstance, output, args)

        return ExitCode.valueOf(result.toString())
    }
}

private fun createIsolatedClassLoader(customWebCompilerArtifacts: CustomJsCompilerArtifacts): URLClassLoader {
    val compilerClassPath = setOf(
        customWebCompilerArtifacts.compilerEmbeddable,
        customWebCompilerArtifacts.baseStdLib,
    )
        .map { it.toURI().toURL() }
        .toTypedArray()

    return URLClassLoader(compilerClassPath, null).apply { setDefaultAssertionStatus(true) }
}
