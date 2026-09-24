/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createTestProvider
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties
import java.util.zip.GZIPInputStream

abstract class ObjCCacheTestBase : AbstractNativeSimpleTest() {
    protected val frameworkTestDataDir = ForTestCompileRuntime.transformTestDataPath("native/native.tests/testData/framework")

    protected val alphaSource = "package liba\nclass Alpha(val name: String)"
    protected val betaSource = "package libb\nimport liba.Alpha\nclass Beta(val alpha: Alpha, val count: Int)"
    protected val frameworkMarkerSource = "package app\nfun frameworkMarker() = 1"

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

    protected fun alphaAndBeta(betaObjCCacheArgs: List<String> = emptyList()): Libraries =
        compileLibraries(Library("libA", alphaSource), Library("libB", betaSource, listOf("libA"), objCCacheArgs = betaObjCCacheArgs))

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

    protected fun linkFramework(
        libraries: Libraries,
        staticFramework: Boolean = true,
        withCaches: Boolean = true,
        included: List<String> = emptyList(),
        extraArgs: List<String> = emptyList(),
        source: String = frameworkMarkerSource,
        dir: File = buildDir,
    ): Framework {
        val sourceDir = dir.resolve("framework").apply { mkdirs() }
        val sourceFile = sourceDir.resolve("framework.kt").apply { writeText(source.trimIndent()) }
        val testCase = generateObjCFrameworkTestCase(
            TestKind.STANDALONE_NO_TR, TestCase.NoTestRunnerExtras("There's no entrypoint in Swift program"), "Kt", listOf(sourceFile),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout * 5),
        )
        val linked = libraries.all.filter { it.name !in included }
        val compilation = ObjCFrameworkCompilation(
            testRunSettings,
            TestCompilerArgs(
                listOfNotNull(
                    "-Xstatic-framework".takeIf { staticFramework },
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-Xbinary=bundleId=foo.bar",
                    "-module-name", "Kt",
                    "-Xcache-directory=${libraries.cacheDir.absolutePath}".takeIf { withCaches },
                    "-Xdisable-ir-checkers=IrVisibilityChecker",
                ) + included.map { "-Xinclude=${libraries.klib(it).absolutePath}" } + extraArgs
            ),
            sourceModules = testCase.modules,
            dependencies = linked.map { libraries.klibs.getValue(it.name).asLibraryDependency() },
            expectedArtifact = TestCompilationArtifact.ObjCFramework(dir, "Kt"),
            exportedLibraries = linked.filter { it.isExported }.map { libraries.klibs.getValue(it.name) },
        )
        return Framework(testCase, compilation.result, dir, libraries.cacheDir, sourceDir)
    }

    protected inner class Framework(
        private val testCase: TestCase,
        val result: TestCompilationResult<out TestCompilationArtifact.ObjCFramework>,
        private val dir: File,
        private val cacheDir: File,
        private val sourceDir: File,
    ) {
        val output: String
            get() = ((result as TestCompilationResult.ImmediateResult<*>).loggedData as LoggedData.CompilationToolCall).toolOutput
                .replace(sourceDir.absolutePath, "<sources>")

        val header: String get() = result.assertSuccess().resultingArtifact.mainHeader.readText()

        val binary: File
            get() {
                val frameworkDir = result.assertSuccess().resultingArtifact.frameworkDir
                return frameworkDir.resolve("Versions/A/Kt").takeIf { it.exists() } ?: frameworkDir.resolve("Kt")
            }

        val symbols: List<String> by lazy { definedSymbols(binary) }

        fun assertObjCCachesLinked(vararg libraryNames: String, linked: Boolean = true, cachedFrameworkName: String = "Kt") {
            for (libraryName in libraryNames) {
                val adapters = objCCacheMetadata(cacheDir, libraryName, cachedFrameworkName).adapterSymbols(libraryName)
                assertTrue(adapters.isNotEmpty()) { "Expected the $libraryName cache to record adapters" }
                for (adapter in adapters) {
                    assertEquals(if (linked) 1 else 0, symbols.count { it.endsWith(adapter) }) {
                        "Expected $adapter of the $libraryName cache to be ${if (linked) "defined once" else "absent"} in the framework"
                    }
                }
            }
        }

        fun runSwift(body: String, declarations: String = "") {
            val source = dir.resolve("objCCache.swift")
            source.writeText(
                """
                |import Foundation
                |import Kt
                |
                |${declarations.trimIndent()}
                |
                |func testObjCCache() throws {
                |${body.trimIndent().prependIndent("    ")}
                |}
                |
                |class ObjCCacheTests : SimpleTestProvider {
                |    override init() {
                |        super.init()
                |        test("testObjCCache", testObjCCache)
                |    }
                |}
                """.trimMargin()
            )
            val searchPaths = listOf(
                "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks",
                "-Xlinker", "-rpath", "-Xlinker", dir.absolutePath,
                "-F", dir.absolutePath,
            )
            val executable = SwiftCompilation(
                testRunSettings,
                listOf(source, createTestProvider(dir, listOf(source)), frameworkTestDataDir.resolve("main.swift")),
                TestCompilationArtifact.Executable(dir.resolve("swiftTestExecutable")),
                searchPaths,
                outputFile = { it.executableFile },
            ).result.assertSuccess()
            val testExecutable = TestExecutable(executable.resultingArtifact, executable.loggedData, listOf(TestName("testObjCCache")))
            runExecutableAndVerify(testCase, testExecutable)
        }
    }
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

    @Test
    fun testDynamicFrameworkLink() = frameworkLinkImpl(staticFramework = false)

    @Test
    fun testStaticFrameworkLink() = frameworkLinkImpl(staticFramework = true)

    private fun frameworkLinkImpl(staticFramework: Boolean) {
        val framework = linkFramework(
            compileLibraries(testLibrary()), staticFramework, source = frameworkTestDataDir.resolve("objcexport/library.kt").readText(),
        )
        framework.assertObjCCachesLinked("library")
        framework.runSwift(
            """
            let object = A(data: "Data from Cache")
            try assertEquals(actual: object.data, expected: "Data from Cache")
            try assertEquals(actual: E.b.data, expected: "Enum entry B")
            """
        )
    }

    private fun testLibrary() = Library("library", frameworkTestDataDir.resolve("objcexport/library/library.kt").readText())

    @Test
    fun testStaticFrameworkMultipleLibraries() {
        val framework = linkFramework(alphaAndBeta())
        framework.assertObjCCachesLinked("libA", "libB")
        framework.runSwift(
            """
            let beta = Beta(alpha: Alpha(name: "AlphaCached"), count: 42)
            try assertEquals(actual: beta.alpha.name, expected: "AlphaCached")
            try assertEquals(actual: beta.count, expected: 42)
            """
        )
    }

    @Test
    fun testStaticFrameworkTransitiveDependencyNotExported() = transitiveDependencyNotExportedImpl(staticFramework = true)

    @Test
    fun testDynamicFrameworkTransitiveDependencyNotExported() = transitiveDependencyNotExportedImpl(staticFramework = false)

    private fun transitiveDependencyNotExportedImpl(staticFramework: Boolean) {
        val libraries = compileLibraries(
            Library("libA", alphaSource, isExported = false, hasObjCCache = true),
            Library("libB", betaSource, listOf("libA")),
        )
        val framework = linkFramework(libraries, staticFramework)
        framework.assertObjCCachesLinked("libB")
        framework.assertObjCCachesLinked("libA", linked = false)
        framework.runSwift(
            """
            let beta = Beta(alpha: LibAAlpha(name: "AlphaCached"), count: 42)
            try assertEquals(actual: beta.alpha.name, expected: "AlphaCached")
            try assertEquals(actual: beta.count, expected: 42)
            """
        )
    }

    @Test
    fun testStaticFrameworkMixedCaching() {
        val libraries = compileLibraries(Library("libA", alphaSource), Library("libB", betaSource, listOf("libA"), isCached = false))
        val framework = linkFramework(libraries)
        framework.assertObjCCachesLinked("libA")
        assertTrue("@interface KtAlpha" in framework.header && "@interface KtBeta" in framework.header) { framework.header }
        assertTrue(framework.symbols.any { it.endsWith("_OBJC_CLASS_\$_KtBeta") }) { "Expected the class of the uncached library" }
    }

    @Test
    fun testStaticFrameworkCrossLibraryInheritance() {
        val libraries = compileLibraries(
            Library(
                "libA",
                """
                package liba
                open class Alpha(val name: String) {
                    open fun greet(): String = "Hello from Alpha: " + name
                }
                """,
            ),
            Library(
                "libB",
                """
                package libb
                import liba.Alpha
                class Beta(name: String, val count: Int) : Alpha(name) {
                    override fun greet(): String = "Hello from Beta: " + name + " with count " + count
                }
                """,
                listOf("libA"),
            ),
        )
        val framework = linkFramework(libraries)
        framework.assertObjCCachesLinked("libA", "libB")
        framework.runSwift(
            """
            let beta = Beta(name: "TestBeta", count: 42)
            let alpha: Alpha = beta
            try assertEquals(actual: beta.name, expected: "TestBeta")
            try assertEquals(actual: beta.count, expected: 42)
            try assertEquals(actual: beta.greet(), expected: "Hello from Beta: TestBeta with count 42")
            try assertEquals(actual: alpha.greet(), expected: "Hello from Beta: TestBeta with count 42")
            """
        )
    }

    @Test
    fun testStaticFrameworkCrossLibraryCategory() = crossLibraryCategoryImpl(staticFramework = true)

    @Test
    fun testDynamicFrameworkCrossLibraryCategory() = crossLibraryCategoryImpl(staticFramework = false)

    private fun crossLibraryCategoryImpl(staticFramework: Boolean) {
        val libraries = compileLibraries(
            Library("libA", alphaSource),
            Library("libB", "package libb\nimport liba.Alpha\nfun Alpha.greet(): String = \"Hello \" + name", listOf("libA")),
        )
        val framework = linkFramework(libraries, staticFramework)
        framework.assertObjCCachesLinked("libA", "libB")
        framework.runSwift("try assertEquals(actual: Alpha(name: \"AlphaCached\").greet(), expected: \"Hello AlphaCached\")")
    }

    @Test
    fun testSwiftOverridesInheritedMethodThroughCachedSubclass() = inheritedReverseAdaptersImpl(isChildCached = true)

    private fun inheritedReverseAdaptersImpl(isChildCached: Boolean) {
        val libraries = compileLibraries(
            Library("libA", parentSource), Library("libB", childSource, listOf("libA"), isCached = isChildCached),
        )
        val framework = linkFramework(libraries)
        framework.assertObjCCachesLinked("libA")
        if (isChildCached) framework.assertObjCCachesLinked("libB")
        assertEquals(parentAdapterBridges(objCCacheArchive(libraries.cacheDir, "libA")), parentAdapterBridges(framework.binary)) {
            "Expected the framework to reuse the reverse adapters of Parent from libA's cache"
        }
        framework.runSwift(
            declarations = """
                class SwiftChild : Child {
                    override func greet() -> String {
                        return "swift"
                    }
                }
                """,
            body = "try assertEquals(actual: LibBKt.callGreet(parent: SwiftChild()), expected: \"swift\")",
        )
    }

    @Test
    fun testKeptWhenDependencyChangeDoesNotAlterExportedApi() {
        val libraries = alphaAndBeta()
        val changed = compileLibraries(
            Library("libA", "package liba\nclass Alpha(val name: String, val extra: Int = 0)"),
            dir = buildDir.resolve("changed"),
            cacheDir = libraries.cacheDir,
        )
        linkFramework(Libraries(libraries.all, libraries.klibs + changed.klibs, libraries.cacheDir)).assertObjCCachesLinked("libA", "libB")
    }

    @Test
    fun testFrameworkEntryPointsUnion() {
        val entryPoints = buildDir.resolve("entryPoints.txt").apply {
            writeText("callable liba.Alpha.name\ncallable liba.Alpha.<init>\ncallable libb.Beta.value\ncallable libb.Beta.<init>")
        }
        val entryPointsArgs = listOf("-Xbinary=objcExportEntryPointsPath=${entryPoints.absolutePath}")
        val libraries = compileLibraries(
            Library(
                "libA",
                """
                package liba
                class Alpha(val name: String) {
                    fun secretA() = "secretA"
                }
                enum class Shade(val secretShade: String) { LIGHT("light") }
                """,
                objCCacheArgs = entryPointsArgs,
            ),
            Library(
                "libB",
                """
                package libb
                interface Valued {
                    val value: Int
                }
                class Beta(override val value: Int) : Valued {
                    fun secretB() = "secretB"
                }
                """,
                objCCacheArgs = entryPointsArgs,
            ),
        )
        val framework = linkFramework(libraries, staticFramework = false, extraArgs = entryPointsArgs)
        framework.assertObjCCachesLinked("libA", "libB")
        val header = framework.header
        val expected = listOf(
            "@interface KtAlpha", "initWithName:", "@property (readonly) NSString *name",
            "@interface KtBeta", "initWithValue:", "@property (readonly) int32_t value",
        )
        for (declaration in expected) {
            assertTrue(declaration in header) { "Expected $declaration in the header:\n$header" }
        }
        assertFalse("secretA" in header || "secretB" in header || "secretShade" in header) { header }
        assertEquals(1, header.lines().count { "@property (readonly) int32_t value" in it }) { "Expected only Beta.value:\n$header" }
    }

    @Test
    fun testIgnoredUnderSwiftExport() {
        val libraries = compileLibraries(testLibrary(), frameworkName = "Kotlin")
        val framework = linkFramework(libraries, staticFramework = false, extraArgs = listOf("-Xbinary=swiftExport=true"))
        framework.assertObjCCachesLinked("library", linked = false, cachedFrameworkName = "Kotlin")
    }

    @Test
    fun testIgnoredByDynamicLibrary() {
        val libraries = compileLibraries(testLibrary())
        val klib = libraries.klib("library").absolutePath
        val source = buildDir.resolve("dynamic.kt").apply { writeText("package test\nfun dynamicLibraryMarker() = 1") }
        val outputDir = buildDir.resolve("dynamic").apply { mkdirs() }
        val args = listOf(
            "-produce", "dynamic", "-module-name", "Kt", "-output", outputDir.resolve("kt").absolutePath, "-library", klib,
            "-Xexport-library=$klib", "-Xcache-directory=${libraries.cacheDir.absolutePath}", source.absolutePath,
        )
        runCompiler(args)
    }

    @Test
    fun testIgnoredWhenDumpingSelectors() {
        val dump = buildDir.resolve("selectors.txt")
        val framework = linkFramework(
            compileLibraries(testLibrary()), extraArgs = listOf("-Xbinary=dumpObjcSelectorToSignatureMapping=${dump.absolutePath}"),
        )
        framework.assertObjCCachesLinked("library", linked = false)
        assertTrue(dump.readLines().any { it.startsWith("KtA,") }) { "Expected the dump to map the cached class KtA:\n${dump.readText()}" }
    }

    @Test
    fun testNotUsedForIncludedLibrary() {
        val libraries = compileLibraries(
            Library(
                "includedLib",
                """
                package test.included
                class IncludedClass(val value: String) {
                    fun greet() = "Hello " + value
                }
                """,
                isExported = false,
                hasObjCCache = true,
            )
        )
        val framework = linkFramework(libraries, staticFramework = false, included = listOf("includedLib"))
        framework.assertObjCCachesLinked("includedLib", linked = false)
        assertTrue("@interface KtIncludedClass" in framework.header) { framework.header }
        framework.runSwift("try assertEquals(actual: IncludedClass(value: \"World\").greet(), expected: \"Hello World\")")
    }

    @Test
    fun testHybridCachedExportAndUncachedInclude() {
        val libraries = compileLibraries(
            Library(
                "cachedLib",
                """
                package test.cached
                open class CachedClass(val name: String) {
                    fun cachedMethod() = "cached: " + name
                }
                """,
            ),
            Library(
                "includedLib",
                """
                package test.included
                import test.cached.CachedClass
                class SubClass(name: String) : CachedClass(name) {
                    fun subMethod() = "sub: " + name
                }
                """,
                listOf("cachedLib"),
                isExported = false,
                isCached = false,
            ),
        )
        val framework = linkFramework(libraries, staticFramework = false, included = listOf("includedLib"))
        framework.assertObjCCachesLinked("cachedLib")
        assertTrue("@interface KtCachedClass" in framework.header && "@interface KtSubClass" in framework.header) { framework.header }
        framework.runSwift(
            """
            try assertEquals(actual: CachedClass(name: "Base").cachedMethod(), expected: "cached: Base")
            let sub = SubClass(name: "Derived")
            try assertEquals(actual: sub.cachedMethod(), expected: "cached: Derived")
            try assertEquals(actual: sub.subMethod(), expected: "sub: Derived")
            """
        )
    }

    @Test
    fun testRuntimeParityWithCleanFramework() {
        val libraries = compileLibraries(Library("parityLib", paritySource))
        val clean = linkFramework(libraries, staticFramework = false, withCaches = false, dir = buildDir.resolve("clean"))
        val cached = linkFramework(libraries, staticFramework = false, dir = buildDir.resolve("cached"))
        cached.assertObjCCachesLinked("parityLib")
        assertEquals(clean.header, cached.header) { "Expected the header of the framework without caches" }
        cached.runSwift(paritySwift)
    }

    private val paritySource = """
        package parity

        open class ParityPerson(val name: String, var age: Int) {
            open fun describe(): String = "Person(name=" + name + ", age=" + age + ")"
        }

        class ParityStudent(name: String, age: Int, val university: String) : ParityPerson(name, age) {
            override fun describe(): String = "Student(name=" + name + ", age=" + age + ", university=" + university + ")"
        }

        enum class ParityDirection(val degrees: Int) {
            NORTH(0),
            EAST(90),
            SOUTH(180),
            WEST(270);

            fun opposite(): ParityDirection = when (this) {
                NORTH -> SOUTH
                EAST -> WEST
                SOUTH -> NORTH
                WEST -> EAST
            }
        }

        class ParityMathOps {
            companion object {
                fun add(a: Int, b: Int): Int = a + b
            }
        }

        class ParityCalculator {
            @Throws(Throwable::class)
            fun divide(a: Int, b: Int): Int {
                if (b == 0) throw IllegalArgumentException("Division by zero")
                return a / b
            }
        }

        fun ParityPerson.celebrateBirthday(): String {
            age += 1
            return "Happy birthday " + name + ", now " + age
        }

        fun parityMultiply(a: Int, b: Int): Int = a * b
        """

    private val paritySwift = """
        let person = ParityPerson(name: "Ada", age: 25)
        try assertEquals(actual: person.name, expected: "Ada")
        try assertEquals(actual: person.age, expected: 25)
        try assertEquals(actual: person.describe(), expected: "Person(name=Ada, age=25)")

        let student = ParityStudent(name: "Grace", age: 19, university: "ComputingCollege")
        try assertEquals(actual: student.name, expected: "Grace")
        try assertEquals(actual: student.age, expected: 19)
        try assertEquals(actual: student.university, expected: "ComputingCollege")
        try assertEquals(actual: student.describe(), expected: "Student(name=Grace, age=19, university=ComputingCollege)")

        person.age = 26
        try assertEquals(actual: person.age, expected: 26)
        try assertEquals(actual: person.celebrateBirthday(), expected: "Happy birthday Ada, now 27")
        try assertEquals(actual: person.age, expected: 27)

        try assertEquals(actual: ParityLibKt.parityMultiply(a: 6, b: 7), expected: 42)
        try assertEquals(actual: ParityDirection.north.degrees, expected: 0)
        try assertEquals(actual: ParityDirection.north.opposite(), expected: ParityDirection.south)
        try assertEquals(actual: ParityMathOps.companion.add(a: 20, b: 22), expected: 42)

        var exceptionThrown = false
        do {
            _ = try ParityCalculator().divide(a: 10, b: 0)
        } catch {
            exceptionThrown = true
        }
        try assertTrue(exceptionThrown)
        """

    @Test
    fun testSwiftOverridesInheritedMethodThroughLiveSubclass() = inheritedReverseAdaptersImpl(isChildCached = false)

    @Test
    fun testStaticFrameworkRequiredClassExtendsCachedClass() {
        val libraries = compileLibraries(
            Library("libA", "package liba\nopen class Alpha(val name: String)"),
            Library(
                "libN", "package libn\nimport liba.Alpha\nclass Hidden(name: String) : Alpha(name)", listOf("libA"), isExported = false,
            ),
            Library(
                "libB",
                """
                package libb
                import libn.Hidden
                class Beta {
                    fun hidden(): Hidden = Hidden("fromBeta")
                }
                """,
                listOf("libA", "libN"),
            ),
        )
        val framework = linkFramework(libraries)
        framework.assertObjCCachesLinked("libA", "libB")
        framework.runSwift("let alpha: Alpha = Beta().hidden()\ntry assertEquals(actual: alpha.name, expected: \"fromBeta\")")
    }

    @Test
    fun testStaticFrameworkProtocolObjects() {
        val libraries = compileLibraries(
            Library("libA", "package liba\ninterface AlphaListener { fun onAlpha(): Int }\nopen class Alpha"),
            Library(
                "libB",
                """
                package libb
                import liba.Alpha
                import liba.AlphaListener
                import platform.Foundation.NSError
                interface BetaListener : AlphaListener { fun onBeta(): Int }
                class Beta : Alpha() {
                    fun domain(error: NSError): String? = error.domain
                }
                """,
                listOf("libA"),
            ),
        )
        val framework = linkFramework(libraries)
        framework.assertObjCCachesLinked("libA", "libB")
        framework.runSwift(
            """
            let _ = Beta()
            try assertTrue(NSProtocolFromString("KtAlphaListener") != nil)
            try assertTrue(NSProtocolFromString("KtBetaListener") != nil)
            """
        )
    }
}

@Tag("caches")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class ObjCCacheWarningsTest : ObjCCacheTestBase() {
    @Test
    fun testIgnoredWhenExportedLibrariesDiffer() {
        val libraries = compileLibraries(Library("libA", alphaSource), Library("libB", betaSource, listOf("libA"), hasObjCCache = false))
        produceObjCCache(libraries.klib("libB"), libraries.cacheDir, listOf("-l", libraries.klib("libA").absolutePath))
        assertLibBCacheIgnored(libraries, "was built exporting [] of the dependencies of libB")
    }

    @Test
    fun testIgnoredWithoutArchive() {
        val libraries = alphaAndBeta()
        objCCacheArchive(libraries.cacheDir, "libB").delete()
        assertLibBCacheIgnored(libraries, "libB-Kt.objc_cache is incomplete or unreadable (no archive)")
    }

    @Test
    fun testIgnoredWithoutStaticCache() {
        val libraries = alphaAndBeta()
        libraries.cacheDir.resolve("libB-cache").deleteRecursively()
        assertLibBCacheIgnored(libraries, "libB-Kt.objc_cache requires a static cache of libB, which this compilation does not use")
    }

    @Test
    fun testIgnoredWhenBuiltInAnotherFormat() {
        val libraries = alphaAndBeta()
        val metadata = objCCacheBinDir(libraries.cacheDir, "libB").resolve("objc_cache_metadata.properties")
        metadata.writeText(metadata.readText().replace("format=1", "format=0"))
        assertLibBCacheIgnored(libraries, "libB-Kt.objc_cache was built in format 0, but this compiler reads format 1")
    }

    @Test
    fun testIgnoredWhenIncomplete() {
        val libraries = alphaAndBeta()
        objCCacheBinDir(libraries.cacheDir, "libB").resolve("objc_cache_units.txt.gz").delete()
        assertLibBCacheIgnored(libraries, "libB-Kt.objc_cache is incomplete or unreadable")
    }

    private fun assertLibBCacheIgnored(libraries: Libraries, warning: String) {
        val framework = linkFramework(libraries)
        assertTrue(warning in framework.output && "Ignoring cache." in framework.output) {
            "Expected a warning about the libB cache:\n${framework.output}"
        }
        framework.assertObjCCachesLinked("libA")
        framework.assertObjCCachesLinked("libB", linked = false)
    }
}
