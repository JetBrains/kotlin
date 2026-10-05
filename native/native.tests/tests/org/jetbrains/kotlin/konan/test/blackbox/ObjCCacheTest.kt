/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import java.util.zip.GZIPInputStream

abstract class ObjCCacheTestBase : AbstractNativeSimpleTest() {
    @BeforeEach
    fun assumeAppleTarget() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
    }

    protected class Library(
        val name: String,
        val source: String,
        val dependencies: List<String> = emptyList(),
        val isExported: Boolean = true,
        val isCached: Boolean = true,
        val hasObjCCache: Boolean = isExported && isCached,
        val objCCacheArgs: List<String> = emptyList(),
    )

    protected class Libraries(val all: List<Library>, val klibs: Map<String, TestCompilationArtifact.KLIB>, val cacheDir: File) {
        fun klib(name: String): File = klibs.getValue(name).klibFile
    }

    protected fun compileLibraries(
        vararg libraries: Library,
        frameworkName: String = "Kt",
        dir: File = buildDir,
        cacheDir: File = dir.resolve("cache"),
    ): Libraries {
        cacheDir.mkdirs()
        val klibs = mutableMapOf<String, TestCompilationArtifact.KLIB>()
        for (library in libraries) {
            val sourceDir = dir.resolve(library.name).apply { mkdirs() }
            sourceDir.resolve("${library.name}.kt").writeText(library.source.trimIndent())
            val klib = compileToLibrary(
                sourceDir, dir.resolve("${library.name}-klib"), TestCompilerArgs("-module-name", library.name),
                library.dependencies.map { klibs.getValue(it).asLibraryDependency() },
            )
            klibs[library.name] = klib
            val dependencyArgs = library.dependencies.flatMap { listOf("-l", klibs.getValue(it).klibFile.absolutePath) }
            val exportArgs = libraries.filter { it.isExported && it.name in library.dependencies }
                .map { "-Xexport-library=${klibs.getValue(it.name).klibFile.absolutePath}" }
            if (library.isCached) produceStaticCache(klib.klibFile, cacheDir, dependencyArgs)
            if (library.hasObjCCache) {
                produceObjCCache(klib.klibFile, cacheDir, dependencyArgs + exportArgs + library.objCCacheArgs, frameworkName)
            }
        }
        return Libraries(libraries.toList(), klibs, cacheDir)
    }

    protected fun produceStaticCache(klib: File, cacheDir: File, extraArgs: List<String> = emptyList()) {
        runCompiler(cacheArgs("static_cache", klib, cacheDir) + extraArgs)
    }

    protected fun produceObjCCache(
        klib: File,
        cacheDir: File,
        extraArgs: List<String> = emptyList(),
        frameworkName: String = "Kt",
        expectedExitCode: ExitCode = ExitCode.OK,
    ): String = runCompiler(
        cacheArgs("objc_cache", klib, cacheDir) + listOf("-module-name", frameworkName) + extraArgs,
        expectedExitCode,
    )

    private fun cacheArgs(kind: String, klib: File, cacheDir: File): List<String> =
        listOf("-produce", kind, "-Xadd-cache=${klib.absolutePath}", "-Xcache-directory=${cacheDir.absolutePath}")

    protected fun runCompiler(args: List<String>, expectedExitCode: ExitCode = ExitCode.OK): String {
        val distCacheDir = testRunSettings.get<KotlinNativeHome>().dir.resolve("klib/cache/${targets.testTarget.name}-gSTATIC-system")
            .takeIf { it.resolve("stdlib-cache").exists() || it.resolve("stdlib-per-file-cache").exists() }
        val compilerArgs = args + listOf("-target", targets.testTarget.name) +
                listOfNotNull(distCacheDir?.let { "-Xcache-directory=${it.absolutePath}" }) + provisionedXcodeCompilerArgs
        val result = callCompiler(compilerArgs.toTypedArray(), testRunSettings.get<KotlinNativeClassLoader>().classLoader)
        assertEquals(expectedExitCode, result.exitCode) { "kotlinc-native ${compilerArgs.joinToString(" ")}\n${result.toolOutput}" }
        return result.toolOutput
    }

    protected fun objCCacheBinDir(cacheDir: File, libraryName: String, frameworkName: String = "Kt"): File =
        cacheDir.resolve("$libraryName-$frameworkName.objc_cache/bin")

    protected fun objCCacheMetadata(cacheDir: File, libraryName: String, frameworkName: String = "Kt"): Properties =
        Properties().apply {
            objCCacheBinDir(cacheDir, libraryName, frameworkName).resolve("objc_cache_metadata.properties").reader().use { load(it) }
        }

    protected fun Properties.records(key: String): List<String> = getProperty(key).orEmpty().split(";").filter { it.isNotBlank() }

    protected fun Properties.adapterSymbols(libraryName: String): List<String> =
        records("classAdapters").map { "_Kotlin_ObjCExport_TypeAdapter_Class_$it" } +
            records("protocolAdapters").map { "_Kotlin_ObjCExport_TypeAdapter_Protocol_$it" } +
            records("categoryAdapters").map { "_Kotlin_ObjCExport_CategoryAdapter_${it}_${libraryName.replace('.', '_')}" }

    protected fun objCCacheArchive(cacheDir: File, libraryName: String): File =
        objCCacheBinDir(cacheDir, libraryName).listFiles { file: File -> file.extension == "a" }.orEmpty().single()

    protected fun definedSymbols(binary: File, includeLocal: Boolean = false): List<String> =
        runProcess("/usr/bin/nm", *listOfNotNull("-g".takeUnless { includeLocal }, binary.absolutePath).toTypedArray())
            .stdout.lines()
            .mapNotNull { line -> line.trim().split(Regex("\\s+")).takeIf { it.size >= 3 }?.last() }
}

@Tag("caches")
class ObjCCacheTest : ObjCCacheTestBase() {
    @Test
    fun testRejectsCInteropLibrary() {
        val posix = testRunSettings.get<KotlinNativeHome>().dir
            .resolve("klib/platform/${targets.testTarget.name}/org.jetbrains.kotlin.native.platform.posix")
        val cacheDir = buildDir.resolve("cache").apply { mkdirs() }
        val output = produceObjCCache(posix, cacheDir, expectedExitCode = ExitCode.COMPILATION_ERROR)
        assertTrue("-produce objc_cache does not support cinterop libraries" in output) { output }
        assertTrue(cacheDir.listFiles().isNullOrEmpty()) { "Expected no cache in $cacheDir" }
    }

    @Test
    fun testArtifactAndMetadataLayout() {
        val libraries = compileLibraries(
            Library(
                "layoutLib",
                """
                package test.layout
                interface LayoutProtocol {
                    fun doWork(): String
                }
                class LayoutWorker : LayoutProtocol {
                    override fun doWork(): String = "done"
                }
                """,
            )
        )
        val binDir = objCCacheBinDir(libraries.cacheDir, "layoutLib")
        assertTrue(binDir.isDirectory) { "Expected the cache in $binDir" }
        val units = GZIPInputStream(binDir.resolve("objc_cache_units.txt.gz").inputStream()).bufferedReader().use { it.readText() }
        assertTrue("LayoutWorker" in units) { "Expected the header units to declare LayoutWorker" }
        val metadata = objCCacheMetadata(libraries.cacheDir, "layoutLib")
        assertEquals(
            setOf("format", "classAdapters", "protocolAdapters", "categoryAdapters", "exportedLibraries"),
            metadata.stringPropertyNames(),
        )
        assertEquals("1", metadata.getProperty("format"))
        assertEquals(listOf("KtLayoutWorker"), metadata.records("classAdapters"))
        assertEquals(listOf("KtLayoutProtocol"), metadata.records("protocolAdapters"))
        val archiveSymbols = definedSymbols(objCCacheArchive(libraries.cacheDir, "layoutLib"))
        for (adapter in metadata.adapterSymbols("layoutLib")) {
            assertEquals(1, archiveSymbols.count { it.endsWith(adapter) }) { "Expected $adapter to be defined once in the archive" }
        }
    }

    @Test
    fun testCacheReusesReverseAdaptersOfExportedSuperclass() {
        val libraries = compileLibraries(Library("libA", parentSource), Library("libB", childSource, listOf("libA")))
        val [parentBridges, childBridges] = listOf("libA", "libB").map { parentAdapterBridges(objCCacheArchive(libraries.cacheDir, it)) }
        assertTrue(parentBridges > 0) { "Expected libA's cache to compile the reverse adapters of Parent" }
        assertEquals(0, childBridges) { "Expected libB's cache to reuse the reverse adapters of Parent from libA" }
    }

    private val parentSource = """
        package liba
        open class Parent {
            open fun greet(): String = "parent"
            open fun farewell(): String = "bye"
        }
        """

    private val childSource = """
        package libb
        import liba.Parent
        open class Child : Parent()
        fun callGreet(parent: Parent): String = parent.greet()
        """

    private fun parentAdapterBridges(binary: File): Int =
        definedSymbols(binary, includeLocal = true).count { "kotlin2objc_kfun:liba.Parent#" in it }
}
