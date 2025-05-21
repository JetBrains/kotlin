/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.compilation

import org.jetbrains.kotlin.container.topologicalSort
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allRegularDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allDependsOnDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule.Companion.allFriendDependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

class TestCompilationFactory {
    private val cachedKlibCompilations = ThreadSafeCache<KlibCacheKey, KlibCompilations>()
    private val cachedExecutableCompilations = ThreadSafeCache<ExecutableCacheKey, TestCompilation<Executable>>()
    private val cachedObjCFrameworkCompilations = ThreadSafeCache<ObjCFrameworkCacheKey, ObjCFrameworkCompilation>()
    private val cachedBinaryLibraryCompilations = ThreadSafeCache<BinaryLibraryCacheKey, BinaryLibraryCompilation>()
    private val cachedTestBundleCompilations = ThreadSafeCache<TestBundleCacheKey, TestBundleCompilation>()

    private data class KlibCacheKey(val sourceModules: Set<TestModule>, val freeCompilerArgs: TestCompilerArgs, val useHeaders: Boolean)
    private data class ExecutableCacheKey(val sourceModules: Set<TestModule>)
    private data class ObjCFrameworkCacheKey(val sourceModules: Set<TestModule>)
    private data class BinaryLibraryCacheKey(val sourceModules: Set<TestModule>, val kind: BinaryLibraryKind)
    private data class TestBundleCacheKey(val sourceModules: Set<TestModule>)

    // A pair of compilations for a KLIB itself and for its static cache that are created together.
    data class KlibCompilations(val klib: TestCompilation<KLIB>, val staticCache: TestCompilation<KLIBStaticCache>?, val headerCache: TestCompilation<KLIBStaticCache>?)

    data class CompilationDependencies(
        private val klibDependencies: List<CompiledDependency<KLIB>>,
        private val staticCacheDependencies: List<CompiledDependency<KLIBStaticCache>>,
        private val staticCacheHeaderDependencies: List<CompiledDependency<KLIBStaticCache>>
    ) {
        /** Dependencies needed to compile KLIB. */
        fun forKlib(): Iterable<CompiledDependency<KLIB>> = klibDependencies

        /** Dependencies needed to compile KLIB static cache. */
        fun forStaticCache(klib: CompiledDependency<KLIB>, useHeaders: Boolean): Iterable<CompiledDependency<*>> =
            (klibDependencies.asSequence().filter { it.type == FriendLibrary } + klib + if (useHeaders) staticCacheHeaderDependencies else staticCacheDependencies).asIterable()

        /** Dependencies needed to compile one-stage executable. */
        fun forOneStageExecutable(): Iterable<CompiledDependency<*>> =
            (klibDependencies.asSequence() + staticCacheDependencies).asIterable()

        /** Dependencies needed to compile two-stage executable. */
        fun forTwoStageExecutable(
            includedKlib: CompiledDependency<KLIB>,
            includedKlibStaticCache: CompiledDependency<KLIBStaticCache>?
        ): Iterable<CompiledDependency<*>> =
            (klibDependencies.asSequence() + staticCacheDependencies + listOfNotNull(includedKlib, includedKlibStaticCache)).asIterable()
    }

    sealed interface ProduceStaticCache {
        object No : ProduceStaticCache

        sealed class Yes(val options: StaticCacheCompilation.Options) : ProduceStaticCache {
            object Regular : Yes(StaticCacheCompilation.Options.Regular)
            class ForIncludedKlibWithTests(options: StaticCacheCompilation.Options.ForIncludedLibraryWithTests) : Yes(options)
        }

        companion object {
            fun decideForRegularKlib(settings: Settings): ProduceStaticCache =
                if (settings.get<CacheMode>().useStaticCacheForUserLibraries) Yes.Regular else No

            fun decideForIncludedKlib(settings: Settings, expectedExecutableArtifact: Executable, extras: Extras): ProduceStaticCache =
                if (!settings.get<CacheMode>().useStaticCacheForUserLibraries)
                    No
                else
                    when (extras) {
                        is NoTestRunnerExtras -> Yes.Regular
                        is WithTestRunnerExtras -> Yes.ForIncludedKlibWithTests(
                            StaticCacheCompilation.Options.ForIncludedLibraryWithTests(expectedExecutableArtifact, extras)
                        )
                    }
        }
    }

    fun testCaseToKLib(testCase: TestCase, settings: Settings): TestCompilation<KLIB> {
        return modulesToKlib(
            sourceModules = testCase.modules,
            freeCompilerArgs = testCase.freeCompilerArgs,
            settings = settings,
            produceStaticCache = ProduceStaticCache.No,
        ).klib
    }

    fun testCaseToBinaryLibrary(testCase: TestCase, settings: Settings, kind: BinaryLibraryKind): BinaryLibraryCompilation {
        val rootModules = testCase.rootModules
        val cacheKey = BinaryLibraryCacheKey(testCase.rootModules, kind)
        cachedBinaryLibraryCompilations[cacheKey]?.let { return it }

        val (
            dependencies: Iterable<CompiledDependency<*>>,
            sourceModules: Set<TestModule.Exclusive>
        ) = getDependenciesAndSourceModules(settings, testCase.rootModules, testCase.freeCompilerArgs) {
            ProduceStaticCache.No
        }
        val expectedArtifact = BinaryLibrary(settings.artifactFileForBinaryLibrary(rootModules, kind))
        return cachedBinaryLibraryCompilations.computeIfAbsent(cacheKey) {
            BinaryLibraryCompilation(
                settings = settings,
                freeCompilerArgs = testCase.freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = dependencies,
                expectedArtifact = expectedArtifact,
                kind = kind,
            )
        }
    }

    fun testCaseToObjCFrameworkCompilation(
        testCase: TestCase,
        settings: Settings,
        exportedLibraries: Iterable<KLIB> = emptyList(),
    ): ObjCFrameworkCompilation {
        val cacheKey = ObjCFrameworkCacheKey(testCase.rootModules)
        cachedObjCFrameworkCompilations[cacheKey]?.let { return it }

        val (
            dependencies: Iterable<CompiledDependency<*>>,
            sourceModules: Set<TestModule.Exclusive>
        ) = getDependenciesAndSourceModules(settings, testCase.rootModules, testCase.freeCompilerArgs) {
            ProduceStaticCache.No
        }

        return cachedObjCFrameworkCompilations.computeIfAbsent(cacheKey) {
            ObjCFrameworkCompilation(
                settings = settings,
                freeCompilerArgs = testCase.freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = dependencies,
                exportedLibraries = exportedLibraries,
                expectedArtifact = ObjCFramework(
                    settings.get<Binaries>().testBinariesDir,
                    testCase.nominalPackageName.compressedPackageName
                )
            )
        }
    }

    fun testCasesToExecutable(testCases: Collection<TestCase>, settings: Settings): TestCompilation<Executable> {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = ExecutableCacheKey(rootModules)

        // Fast pass.
        cachedExecutableCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs // Should be identical inside the same test case group.
        val extras = testCases.first().extras // Should be identical inside the same test case group.
        val fileCheckStage = testCases.map { it.fileCheckStage }.singleOrNull()
        if (fileCheckStage != null)
            require(testCases.size == 1) { "FILECHECK-enabled test must be standalone" }
        val executableArtifact = Executable(settings.artifactFileForExecutable(rootModules), fileCheckStage)

        val (
            dependenciesToCompileExecutable: Iterable<CompiledDependency<*>>,
            sourceModulesToCompileExecutable: Set<TestModule.Exclusive>
        ) = getDependenciesAndSourceModules(settings, rootModules, freeCompilerArgs) {
            ProduceStaticCache.decideForIncludedKlib(settings, executableArtifact, extras)
        }

        return cachedExecutableCompilations.computeIfAbsent(cacheKey) {
            ExecutableCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModulesToCompileExecutable,
                extras = extras,
                dependencies = dependenciesToCompileExecutable,
                expectedArtifact = executableArtifact
            )
        }
    }

    fun testCasesToTestBundle(testCases: Collection<TestCase>, settings: Settings): TestCompilation<XCTestBundle> {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = TestBundleCacheKey(rootModules)

        // Fast pass.
        cachedTestBundleCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs // Should be identical inside the same test case group.
        val extras = testCases.first().extras // Should be identical inside the same test case group.
        val fileCheckStage = testCases.map { it.fileCheckStage }.singleOrNull()
        if (fileCheckStage != null) {
            require(testCases.size == 1) { "FILECHECK-enabled test must be standalone" }
        }
        val executableArtifact = XCTestBundle(settings.artifactFileForXCTestBundle(rootModules), fileCheckStage)

        val (
            dependenciesToCompileExecutable: Iterable<CompiledDependency<*>>,
            sourceModulesToCompileExecutable: Set<TestModule.Exclusive>
        ) = getDependenciesAndSourceModules(settings, rootModules, freeCompilerArgs) {
            // An adapter to the cache production that accepts only Executable
            val executable = Executable(executableArtifact.bundleDir, executableArtifact.fileCheckStage)
            ProduceStaticCache.decideForIncludedKlib(settings, executable, extras)
        }

        return cachedTestBundleCompilations.computeIfAbsent(cacheKey) {
            TestBundleCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModulesToCompileExecutable,
                extras = extras,
                dependencies = dependenciesToCompileExecutable,
                expectedArtifact = executableArtifact
            )
        }
    }

    private fun getDependenciesAndSourceModules(
        settings: Settings,
        rootModules: Set<TestModule.Exclusive>,
        freeCompilerArgs: TestCompilerArgs,
        produceStaticCache: () -> ProduceStaticCache,
    ): Pair<Iterable<CompiledDependency<*>>, Set<TestModule.Exclusive>> =
        when (settings.get<TestMode>()) {
            TestMode.ONE_STAGE_MULTI_MODULE -> {
                Pair(
                    // Collect dependencies of root modules. Compile root modules directly to executable.
                    collectDependencies(rootModules, freeCompilerArgs, settings).forOneStageExecutable(),
                    rootModules
                )
            }
            TestMode.TWO_STAGE_MULTI_MODULE -> {
                // Compile root modules to KLIB. Pass this KLIB as included dependency to executable compilation.
                val klibCompilations =
                    modulesToKlib(rootModules, freeCompilerArgs, produceStaticCache(), settings)

                Pair(
                    // Include just compiled KLIB as -Xinclude dependency.
                    collectDependencies(rootModules, freeCompilerArgs, settings).forTwoStageExecutable(
                        includedKlib = klibCompilations.klib.asKlibDependency(IncludedLibrary),
                        includedKlibStaticCache = klibCompilations.staticCache?.asStaticCacheDependency()
                    ),
                    emptySet() // No sources.
                )
            }
        }

    fun modulesToKlib(
        sourceModules: Set<TestModule>,
        freeCompilerArgs: TestCompilerArgs,
        produceStaticCache: ProduceStaticCache,
        settings: Settings
    ): KlibCompilations {
        val useHeaders: Boolean = settings.get<CacheMode>().useHeaders
        val cacheKey = KlibCacheKey(sourceModules, freeCompilerArgs, useHeaders)

        // Fast pass.
        cachedKlibCompilations[cacheKey]?.let { return it }

        // Long pass.
        val dependencies = collectDependencies(sourceModules, freeCompilerArgs, settings)
        val klibArtifact = KLIB(settings.artifactFileForKlib(sourceModules, freeCompilerArgs))

        val isGivenKlibArtifact = sourceModules.singleOrNull() is TestModule.Given

        val staticCacheArtifactAndOptions: Pair<KLIBStaticCache, StaticCacheCompilation.Options>? = when (produceStaticCache) {
            is ProduceStaticCache.No -> null // No artifact means no static cache should be compiled.
            is ProduceStaticCache.Yes -> KLIBStaticCacheImpl(
                cacheDir = settings.cacheDirForStaticCache(klibArtifact, isGivenKlibArtifact),
                klib = klibArtifact
            ) to produceStaticCache.options
        }

        val headerCacheArtifactAndOptions = staticCacheArtifactAndOptions?.let {
            if (!useHeaders) return@let null
            KLIBStaticCacheHeader(
                cacheDir = settings.cacheDirForStaticCache(klibArtifact, isGivenKlibArtifact, header = true),
                klib = klibArtifact
            ) to it.second
        }

        return cachedKlibCompilations.computeIfAbsent(cacheKey) {
            val (klibCompilation, makePerFileCacheOverride) = if (isGivenKlibArtifact)
                GivenLibraryCompilation(klibArtifact) to false // Don't make per-file-cache from given dependencies(usually, cinterop)
            else {
                val filesByExtension = sourceModules.first().files
                    .map { it.location }
                    .groupBy { it.name.split(".").last() }
                when {
                    filesByExtension.contains("kt") -> LibraryCompilation(
                        settings = settings,
                        freeCompilerArgs = freeCompilerArgs,
                        sourceModules = sourceModules.flatMapToSet { sortDependsOnTopologically(it) },
                        dependencies = dependencies.forKlib(),
                        expectedArtifact = klibArtifact
                    ) to null
                    filesByExtension.contains("def") -> {
                        val defFile = filesByExtension["def"]!!.single()
                        val testTarget = settings.get<KotlinNativeTargets>().testTarget
                        check(defFile.defFileIsSupportedOn(testTarget)) {
                            "Unsupported $defFile for target $testTarget"
                        }
                        val cSourceFiles = buildList {
                            for (ext in CINTEROP_SOURCE_EXTENSIONS) {
                                filesByExtension[ext]?.let { addAll(it) }
                            }
                        }
                        CInteropCompilation(
                            classLoader = settings.get(),
                            targets = settings.get(),
                            freeCompilerArgs = freeCompilerArgs,
                            defFile = defFile,
                            sources = cSourceFiles,
                            dependencies = dependencies.forKlib(),
                            expectedArtifact = klibArtifact
                        ) to false // CInterop klib cannot be compiled into per-file cache
                    }
                    else -> error("Test module must contain either KT or DEF file")
                }
            }

            val staticCacheCompilation: StaticCacheCompilation? =
                staticCacheArtifactAndOptions?.let { (staticCacheArtifact, staticCacheOptions) ->
                    StaticCacheCompilation(
                        settings = settings,
                        freeCompilerArgs = freeCompilerArgs,
                        options = staticCacheOptions,
                        dependencies = dependencies.forStaticCache(
                            klibCompilation.asKlibDependency(type = /* does not matter in fact*/ Library),
                            settings.get<CacheMode>().useHeaders
                        ),
                        expectedArtifact = staticCacheArtifact,
                        makePerFileCacheOverride = makePerFileCacheOverride,
                    )
                }

            val headerCacheCompilation: StaticCacheCompilation? =
                headerCacheArtifactAndOptions?.let { (staticCacheArtifact, staticCacheOptions) ->
                    StaticCacheCompilation(
                        settings = settings,
                        freeCompilerArgs = freeCompilerArgs,
                        options = staticCacheOptions,
                        createHeaderCache = true,
                        dependencies = dependencies.forStaticCache(
                            klibCompilation.asKlibDependency(type = /* does not matter in fact*/ Library),
                            settings.get<CacheMode>().useHeaders
                        ),
                        expectedArtifact = staticCacheArtifact
                    )
                }

            KlibCompilations(klibCompilation, staticCacheCompilation, headerCacheCompilation)
        }
    }

    fun collectDependencies(
        sourceModules: Set<TestModule>,
        freeCompilerArgs: TestCompilerArgs,
        settings: Settings
    ): CompilationDependencies {
        val klibDependencies = mutableListOf<CompiledDependency<KLIB>>()
        val staticCacheDependencies = mutableListOf<CompiledDependency<KLIBStaticCache>>()
        val staticCacheHeaderDependencies = mutableListOf<CompiledDependency<KLIBStaticCache>>()

        val produceStaticCache = ProduceStaticCache.decideForRegularKlib(settings)

        fun <T : TestCompilationDependencyType<KLIB>> Set<TestModule>.collectDependencies(type: T) =
            forEach { dependencyModule: TestModule ->
                val klibCompilations = modulesToKlib(setOf(dependencyModule), freeCompilerArgs, produceStaticCache, settings)
                klibDependencies += klibCompilations.klib.asKlibDependency(type)

                staticCacheDependencies.addIfNotNull(klibCompilations.staticCache?.asStaticCacheDependency())
                staticCacheHeaderDependencies.addIfNotNull((klibCompilations.headerCache ?: klibCompilations.staticCache)?.asStaticCacheDependency())
            }

        sourceModules.allRegularDependencies().collectDependencies(Library)
        sourceModules.allFriendDependencies().collectDependencies(FriendLibrary)

        return CompilationDependencies(klibDependencies, staticCacheDependencies, staticCacheHeaderDependencies)
    }

    private fun sortDependsOnTopologically(module: TestModule): List<TestModule> {
        return topologicalSort(listOf(module), reverseOrder = true) { it.allDependsOnDependencies }
    }

    companion object {
        private fun Set<TestModule>.allRegularDependencies(): Set<TestModule> =
            if (size == 1) first().allRegularDependencies else flatMapToSet { it.allRegularDependencies }

        private fun Set<TestModule>.allFriendDependencies(): Set<TestModule> =
            if (size == 1) first().allFriendDependencies else flatMapToSet { it.allFriendDependencies }

        private fun <T : TestCompilationDependencyType<KLIB>> TestCompilation<KLIB>.asKlibDependency(type: T): CompiledDependency<KLIB> =
            CompiledDependency(this, type)

        private fun TestCompilation<KLIBStaticCache>.asStaticCacheDependency(): CompiledDependency<KLIBStaticCache> =
            CompiledDependency(this, LibraryStaticCache)

        private fun Settings.artifactFileForExecutable(modules: Set<TestModule.Exclusive>) = when (modules.size) {
            1 -> artifactFileForExecutable(modules.first())
            else -> multiModuleArtifactFile(modules, get<KotlinNativeTargets>().testTarget.family.exeSuffix)
        }

        private fun Settings.artifactFileForExecutable(module: TestModule.Exclusive) =
            singleModuleArtifactFile(module, get<KotlinNativeTargets>().testTarget.family.exeSuffix)

        private fun Settings.pickBinaryLibrarySuffix(kind: BinaryLibraryKind) = when (kind) {
            BinaryLibraryKind.STATIC -> get<KotlinNativeTargets>().testTarget.family.staticSuffix
            BinaryLibraryKind.DYNAMIC -> get<KotlinNativeTargets>().testTarget.family.dynamicSuffix
        }

        private fun Settings.artifactFileForBinaryLibrary(modules: Set<TestModule.Exclusive>, kind: BinaryLibraryKind) = when (modules.size) {
            1 -> artifactFileForBinaryLibrary(modules.first(), kind)
            else -> multiModuleArtifactFile(modules, pickBinaryLibrarySuffix(kind))
        }

        private fun Settings.artifactFileForBinaryLibrary(module: TestModule.Exclusive, kind: BinaryLibraryKind) =
            singleModuleArtifactFile(module, pickBinaryLibrarySuffix(kind))

        private fun Settings.artifactFileForXCTestBundle(modules: Set<TestModule.Exclusive>): File {
            /*
            FirebaseCloudXCTestExecutor supports only `test-ios-launchTests.xctest` as the bundle name.
            Other XCTest executors don't have this limitation, but let's keep things uniform for simplicity.

            So, here we take the path similarly to other artifact kinds but make it a directory and place
            the xctest bundle inside.

            This way we keep the artifact path unique and meet the requirements of FirebaseCloudXCTestExecutor.
            */

            val parentDirectory = when (modules.size) {
                1 -> singleModuleArtifactFile(modules.first(), "out")
                else -> multiModuleArtifactFile(modules, "out")
            }

            return parentDirectory.resolve("test-ios-launchTests.${xctestExtension()}")
        }

        private fun Settings.xctestExtension(): String = CompilerOutputKind.TEST_BUNDLE
            .suffix(get<KotlinNativeTargets>().testTarget)
            .substringAfterLast(".")

        private fun Settings.artifactFileForKlib(modules: Set<TestModule>, freeCompilerArgs: TestCompilerArgs): File =
            when (modules.size) {
                1 -> when (val module = modules.first()) {
                    is TestModule.Exclusive -> singleModuleArtifactFile(module, "klib")
                    is TestModule.Shared -> get<Binaries>().sharedBinariesDir.resolve("${module.name}-${prettyHash(freeCompilerArgs.hashCode())}.klib")
                    is TestModule.Given -> module.klibFile
                }
                else -> {
                    assertTrue(modules.none { module -> module is TestModule.Shared }) {
                        "Can't compile shared module together with any other module"
                    }
                    assertTrue(modules.none { module -> module is TestModule.Given }) {
                        "Can't compile given module together with any other module"
                    }
                    multiModuleArtifactFile(modules, "klib")
                }
            }

        private fun Settings.cacheDirForStaticCache(klibArtifact: KLIB, isGivenKlibArtifact: Boolean, header: Boolean = false): File {
            val artifactBaseDir = if (isGivenKlibArtifact) {
                // Special case for the given (external) KLIB artifacts.
                get<Binaries>().givenBinariesDir
            } else {
                // The KLIB artifact is located inside the build dir. This means it was built just a moment ago.
                klibArtifact.klibFile.parentFile
            }

            return artifactBaseDir.resolve(if (header) HEADER_CACHE_DIR_NAME else STATIC_CACHE_DIR_NAME).apply { mkdirs() }
        }

        private fun Settings.singleModuleArtifactFile(module: TestModule.Exclusive, extension: String): File {
            val artifactFileName = buildString {
                append(module.testCase.nominalPackageName.compressedPackageName).append('.')
                if (extension == "klib") append(module.name).append('.')
                append(extension)
            }
            return artifactDirForPackageName(module.testCase.nominalPackageName).resolve(artifactFileName)
        }

        private fun Settings.multiModuleArtifactFile(modules: Collection<TestModule>, extension: String): File {
            var filesCount = 0
            var hash = 0
            val uniquePackageNames = hashSetOf<PackageName>()

            modules.forEach { module ->
                module.files.forEach { file ->
                    filesCount++
                    hash = hash * 31 + file.hashCode()
                }

                if (module is TestModule.Exclusive)
                    uniquePackageNames += module.testCase.nominalPackageName
            }

            val commonPackageName = uniquePackageNames.findCommonPackageName()

            val artifactFileName = buildString {
                val prefix = filesCount.toString()
                repeat(4 - prefix.length) { append('0') }
                append(prefix).append('-')

                if (!commonPackageName.isEmpty())
                    append(commonPackageName.compressedPackageName).append('-')

                append(prettyHash(hash))

                append('.').append(extension)
            }

            return artifactDirForPackageName(commonPackageName).resolve(artifactFileName)
        }

        private fun Settings.artifactDirForPackageName(packageName: PackageName): File {
            val baseDir = get<Binaries>().testBinariesDir
            val outputDir = if (!packageName.isEmpty()) baseDir.resolve(packageName.compressedPackageName) else baseDir

            outputDir.mkdirs()

            return outputDir
        }
    }
}
