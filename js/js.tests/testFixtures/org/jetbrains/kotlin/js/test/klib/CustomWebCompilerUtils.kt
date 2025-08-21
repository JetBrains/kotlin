/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URLClassLoader
import kotlin.test.fail

/**
 * An accessor to "custom" (alternative) Kotlin/JS compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customJsCompilerSettings: CustomWebCompilerSettings by lazy {
    createCustomWebCompilerSettings(
        artifactsDirPropertyName = "kotlin.internal.js.test.compat.customCompilerArtifactsDir",
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
        artifactsDirPropertyName = "kotlin.internal.wasm.test.compat.customCompilerArtifactsDir",
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
    val kotlinTest: File
    val customCompiler: CustomWebCompiler
}

val CustomWebCompilerSettings.defaultLanguageVersion: LanguageVersion
    get() = LanguageVersion.fromFullVersionString(version)
        ?: fail("Cannot deduce the default LV from the compiler version: $version")

private fun createCustomWebCompilerSettings(
    artifactsDirPropertyName: String,
    versionPropertyName: String,
    stdlibArtifactName: String,
    kotlinTestArtifactName: String,
): CustomWebCompilerSettings = object : CustomWebCompilerSettings {
    private val artifacts: CustomWebCompilerArtifacts by lazy {
        CustomWebCompilerArtifacts.create(artifactsDirPropertyName, versionPropertyName)
    }

    override val version: String get() = artifacts.version
    override val stdlib: File by lazy { artifacts.resolve(stdlibArtifactName, "klib") }

    override val kotlinTest: File by lazy {
        // Older versions of Kotlin/JS 'kotlin-test' had KLIBs with *.jar file extension.
        artifacts.resolve(kotlinTestArtifactName, "klib", "jar")
    }

    override val customCompiler: CustomWebCompiler by lazy {
        CustomWebCompiler(
            listOfNotNull(
                artifacts.resolve("kotlin-compiler-embeddable", "jar"),
                artifacts.resolveOptionalTrove4j(),
                artifacts.resolve("kotlin-stdlib", "jar"),
            )
        )
    }
}

/**
 * An entry point to call a custom Kotlin/JS or Kotlin/Wasm compiler inside an isolated class loader.
 *
 * Note: The class loader is cached to be easily reused in all later calls without reloading the class path.
 * Yet it is cached as a [SoftReference] to allow GC in the case of a need.
 */
class CustomWebCompiler(private val compilerClassPath: List<File>) {
    private var isolatedClassLoaderSoftRef: SoftReference<URLClassLoader>? = null

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaderSoftRef?.get() ?: synchronized(this) {
        isolatedClassLoaderSoftRef?.get() ?: run {
            val isolatedClassLoader = URLClassLoader(compilerClassPath.map { it.toURI().toURL() }.toTypedArray(), null)
            isolatedClassLoader.setDefaultAssertionStatus(true)
            isolatedClassLoaderSoftRef = SoftReference(isolatedClassLoader)
            isolatedClassLoader
        }
    }

    fun callCompiler(output: PrintStream, args: Array<String>): ExitCode {
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
     * Resolves the mandatory artifact '$baseName-$version.$extension', where $extension is one of the passed [extensions].
     */
    fun resolve(baseName: String, vararg extensions: String): File

    /**
     * This artifact was removed in Kotlin 2.2.0-Beta1.
     * But it is still available in older compiler versions, where we need to load it.
     */
    fun resolveOptionalTrove4j(): File?

    private class Resolvable(override val version: String, private val artifactsDir: File) : CustomWebCompilerArtifacts {
        override fun resolve(baseName: String, vararg extensions: String): File {
            val candidates: List<File> = extensions.map { extension -> artifactsDir.resolve("$baseName-$version.$extension") }
            return candidates.firstOrNull { it.exists() } ?: fail("Artifact $baseName is not found. Candidates tested: $candidates")
        }

        override fun resolveOptionalTrove4j(): File? {
            return artifactsDir.listFiles()?.firstOrNull { file ->
                file.isFile && file.name.run { startsWith("trove4j") && endsWith("jar") }
            }
        }
    }

    private class Unresolvable(val reason: String) : CustomWebCompilerArtifacts {
        override val version get() = fail(reason)
        override fun resolve(baseName: String, vararg extensions: String) = fail(reason)
        override fun resolveOptionalTrove4j() = fail(reason)
    }

    companion object {
        fun create(artifactsDirPropertyName: String, versionPropertyName: String): CustomWebCompilerArtifacts {
            val version: String = readProperty(versionPropertyName)
                ?: return propertyNotFound(versionPropertyName)

            val artifactsDir: File = readProperty(artifactsDirPropertyName)?.let(::File)
                ?: return propertyNotFound(artifactsDirPropertyName)

            return Resolvable(version, artifactsDir)
        }

        private fun readProperty(propertyName: String): String? =
            System.getProperty(propertyName)?.trim(Char::isWhitespace)?.takeIf(String::isNotEmpty)

        private fun propertyNotFound(propertyName: String): Unresolvable = Unresolvable(
            "The Gradle property \"$propertyName\" is not specified. " +
                    "Please check the README.md in the root of the `klib-compatibility` project."
        )
    }
}
