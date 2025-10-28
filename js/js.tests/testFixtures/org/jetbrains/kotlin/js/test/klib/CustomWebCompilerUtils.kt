/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader
import kotlin.test.fail

/**
 * An accessor to "custom" (alternative) Kotlin/JS compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customJsCompilerSettings: CustomWebCompilerSettings by lazy {
    createCustomWebCompilerSettings(
        compilerClasspathPropertyName = "kotlin.internal.js.test.compat.customCompilerClasspath",
        runtimeDependenciesPropertyName = "kotlin.internal.js.test.compat.runtimeDependencies",
        versionPropertyName = "kotlin.internal.js.test.compat.customCompilerVersion",
        stdlibArtifactName = "kotlin-stdlib-js",
        kotlinTestArtifactName = "kotlin-test-js",
    )
}

/**
 * An accessor to "custom" (alternative) Kotlin/Wasm compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customWasmJsCompilerSettings: CustomWebCompilerSettings by lazy {
    createCustomWebCompilerSettings(
        compilerClasspathPropertyName = "kotlin.internal.wasm.test.compat.customCompilerClasspath",
        runtimeDependenciesPropertyName = "kotlin.internal.wasm.test.compat.runtimeDependencies",
        versionPropertyName = "kotlin.internal.wasm.test.compat.customCompilerVersion",
        stdlibArtifactName = "kotlin-stdlib-wasm-js",
        kotlinTestArtifactName = "kotlin-test-wasm-js",
    )
}

/**
 * A "custom" (alternative) Kotlin/JS or Kotlin/Wasm compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
interface CustomWebCompilerSettings {
    val version: String
    val stdlib: File
    val defaultStdlib: File
    val kotlinTest: File
    val customCompiler: CustomWebCompiler
}

val CustomWebCompilerSettings.defaultLanguageVersion: LanguageVersion
    get() = LanguageVersion.fromFullVersionString(version)
        ?: fail("Cannot deduce the default LV from the compiler version: $version")

private fun createCustomWebCompilerSettings(
    compilerClasspathPropertyName: String,
    runtimeDependenciesPropertyName: String,
    versionPropertyName: String,
    stdlibArtifactName: String,
    kotlinTestArtifactName: String,
): CustomWebCompilerSettings = object : CustomWebCompilerSettings {
    private val artifacts: CustomWebCompilerArtifacts by lazy {
        CustomWebCompilerArtifacts.create(compilerClasspathPropertyName, runtimeDependenciesPropertyName, versionPropertyName)
    }

    override val version: String get() = artifacts.version
    override val stdlib: File by lazy { artifacts.runtimeDependency(stdlibArtifactName, "klib") }
    override val defaultStdlib: File by lazy { StandardLibrariesPathProviderForKotlinProject.defaultJsStdlib() }

    override val kotlinTest: File by lazy {
        // Older versions of Kotlin/JS 'kotlin-test' had KLIBs with *.jar file extension.
        artifacts.runtimeDependency(kotlinTestArtifactName, "klib", "jar")
    }

    override val customCompiler: CustomWebCompiler by lazy {
        CustomWebCompiler(artifacts.compilerClassPath)
    }
}

/**
 * An entry point to call a custom Kotlin/JS or Kotlin/Wasm compiler inside an isolated class loader.
 *
 * Note: The class loader is cached to be easily reused in all later calls without reloading the class path.
 * Yet it is cached as a [SoftReference] to allow GC in the case of a need.
 */
class CustomWebCompiler(private val compilerClassPath: List<URL>) {
    private var isolatedClassLoaderSoftRef: SoftReference<URLClassLoader>? = null

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaderSoftRef?.get() ?: synchronized(this) {
        isolatedClassLoaderSoftRef?.get() ?: run {
            val isolatedClassLoader = URLClassLoader(compilerClassPath.toTypedArray(), null)
            isolatedClassLoader.setDefaultAssertionStatus(true)
            isolatedClassLoaderSoftRef = SoftReference(isolatedClassLoader)
            isolatedClassLoader
        }
    }

    fun callCompiler(output: PrintStream, vararg args: List<String>?): ExitCode {
        val allArgs = args.flatMap { it.orEmpty() }.toTypedArray()
        return callCompiler(output, *allArgs)
    }

    fun callCompiler(output: PrintStream, vararg args: String): ExitCode {
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

private sealed interface CustomWebCompilerArtifacts {
    val version: String

    /**
     * The classpath of the custom compiler.
     */
    val compilerClassPath: List<URL>

    /**
     * Resolves the '$baseName-$version.$extension' runtime dependency artifact, where $extension is one of the passed [extensions].
     */
    fun runtimeDependency(baseName: String, vararg extensions: String): File

    private class Resolvable(
        override val version: String,
        override val compilerClassPath: List<URL>,
        private val runtimeDependencies: List<File>,
    ) : CustomWebCompilerArtifacts {
        override fun runtimeDependency(baseName: String, vararg extensions: String): File {
            val candidates = runtimeDependencies.filter { file ->
                file.isFile && file.extension in extensions && file.nameWithoutExtension == "$baseName-$version"
            }

            return when (candidates.size) {
                0 -> fail("Artifact $baseName is not found.")
                1 -> candidates.first()
                else -> fail("More than one $baseName artifact is found: $candidates")
            }
        }
    }

    private class Unresolvable(val reason: String) : CustomWebCompilerArtifacts {
        override val version get() = fail(reason)
        override val compilerClassPath get() = fail(reason)
        override fun runtimeDependency(baseName: String, vararg extensions: String) = fail(reason)
    }

    companion object {
        fun create(
            compilerClassPathPropertyName: String,
            runtimeDependenciesPropertyName: String,
            versionPropertyName: String
        ): CustomWebCompilerArtifacts {
            val version: String = readProperty(versionPropertyName)
                ?: return propertyNotFound(versionPropertyName)

            val compilerClassPath: List<URL> = readProperty(compilerClassPathPropertyName)
                ?.split(File.pathSeparatorChar)
                ?.map { File(it).toURI().toURL() }
                ?: return propertyNotFound(compilerClassPathPropertyName)

            val runtimeDependencies = readProperty(runtimeDependenciesPropertyName)
                ?.split(File.pathSeparatorChar)
                ?.map(::File)
                ?: return propertyNotFound(runtimeDependenciesPropertyName)

            return Resolvable(version, compilerClassPath, runtimeDependencies)
        }

        private fun readProperty(propertyName: String): String? =
            System.getProperty(propertyName)?.trim(Char::isWhitespace)?.takeIf(String::isNotEmpty)

        private fun propertyNotFound(propertyName: String): Unresolvable = Unresolvable(
            "The Gradle property \"$propertyName\" is not specified. " +
                    "Please check the README.md in the root of the `klib-compatibility` project."
        )
    }
}
