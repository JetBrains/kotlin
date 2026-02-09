/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolverLegacy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.jetbrains.kotlin.util.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.collections.set
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * This is a special test needed to make sure that the external users of the KLIB resolver that cannot migrate to [KlibLoader]
 * can still use the KLIB resolver and get the correct results.
 *
 * Related tickets: KT-82882, KT-83328.
 */
@Tag("klib")
class LegacyKlibResolverUserTest : AbstractNativeSimpleTest() {
    @Test
    fun `Minimal required set of dependencies written to manifest of Native libraries`() {
        createModules(
            TestKlibModule("usesOnlyStdlib")
        ).compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false,
        ) { module, successKlib ->
            assertEquals("usesOnlyStdlib", module.name)

            patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                assertEquals("stdlib", properties[KLIB_PROPERTY_DEPENDS])
            }
        }

        createModules(
            TestKlibModule("usesStdlibAndPosix")
        ).also { modules ->
            modules[0].sourceFile.appendText(
                """
                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        fun makePosixCall() {
                            platform.posix.fopen("test.txt", "r")
                        }
                    """.trimIndent()
            )
        }.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false,
        ) { module, successKlib ->
            assertEquals("usesStdlibAndPosix", module.name)

            patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                assertEquals(
                    setOf("stdlib", "org.jetbrains.kotlin.native.platform.posix"),
                    properties[KLIB_PROPERTY_DEPENDS]?.split(" ")?.toSet()
                )
            }
        }
    }

    @Test
    fun `Resolve non-Native libraries in kotlinx-benchmarks Gradle plugins`() = doTest(isForKotlinNative = false)

    @Test
    fun `Resolve Native libraries in kotlinx-benchmarks Gradle plugins`() = doTest(isForKotlinNative = true)

    private fun doTest(isForKotlinNative: Boolean) {
        val moduleToKlibMapping: MutableMap<TestKlibModule, File> = hashMapOf()

        /*
         * "e" -> "d" -> "c" -> "b" -> "a"
         *  |      |             ^      ^
         *  |      +-------------+      |
         *  +---------------------------+
         */
        createModules(
            TestKlibModule("a"),
            TestKlibModule("b", "a"),
            TestKlibModule("c", "b"),
            TestKlibModule("d", "c", "b"),
            TestKlibModule("e", "d", "a"),
        ).compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false
        ) { module, successKlib ->
            // Remember the location of a KLIB dir.
            val libraryLocation = File(successKlib.resultingArtifact.path)
            moduleToKlibMapping[module] = libraryLocation

            if (!isForKotlinNative) {
                // Simulate the absence of `depends=` property, which is in effect for Kotlin/JS and Kotlin/Wasm.
                patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                    properties.remove(KLIB_PROPERTY_DEPENDS)
                }
            }
        }

        // There should be 5 generated KLIBs in total.
        assertEquals(5, moduleToKlibMapping.size)

        val targetModule: TestKlibModule = moduleToKlibMapping.keys.single { it.name == "e" }
        val targetModuleLocation: File = moduleToKlibMapping.getValue(targetModule)

        val dependencyModules: Set<TestKlibModule> = moduleToKlibMapping.keys - targetModule
        val dependencyModuleLocations: Set<File> = dependencyModules.map { moduleToKlibMapping.getValue(it) }.toSet()

        val allDependencyLocations: Set<File> = dependencyModuleLocations + stdlibLocation // also add stdlib

        val resolvedDependencies = resolveLibrariesInKotlinxBenchmarksGradlePlugin(
            libraryFile = targetModuleLocation,
            dependencyFiles = allDependencyLocations,
            isForKotlinNative = isForKotlinNative,
        )

        assertEquals(allDependencyLocations.size, resolvedDependencies.size)

        assertEquals(
            dependencyModules.map { it.name }.toSet() + "stdlib",
            resolvedDependencies.map { it.uniqueName }.toSet()
        )
    }

    private val stdlibLocation: File
        get() = testRunSettings.get<KotlinNativeHome>().librariesDir.resolve("common/stdlib")

    /**
     * This is an emulation of `KlibResolver.createModuleDescriptor(File, Set<File>, StorageManager)` function
     * from the kotlinx-benchmarks Gradle plugin.
     */
    @Suppress("DEPRECATION", "UNRESOLVED_REFERENCE", "OVERRIDE_DEPRECATION", "DEPRECATION_ERROR")
    private fun resolveLibrariesInKotlinxBenchmarksGradlePlugin(
        libraryFile: File,
        dependencyFiles: Set<File>,
        isForKotlinNative: Boolean,
    ): Collection<KotlinLibrary> {
        val logger = object : Logger {
            override fun log(message: String) = Unit
            override fun error(message: String) = kotlin.error("e: $message")
            override fun warning(message: String) = Unit
            override fun fatal(message: String) = kotlin.error("e: $message")
        }

        val knownIrProviders = if (isForKotlinNative) listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER) else emptyList()

        class KotlinxBenchmarksLibraryResolverSimulation(
            klibs: List<String>
        ) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
            repositories = emptyList(),
            directLibs = klibs,
            distributionKlib = null,
            localKotlinDir = null,
            skipCurrentDir = false,
            logger = logger,
            knownIrProviders = knownIrProviders
        ) {
            override fun libraryComponentBuilder(file: KlibFile, isDefault: Boolean): List<KotlinLibrary> =
                createKotlinLibraryComponents(file, isDefault, null as ZipFileSystemAccessor?)
        }

        val library = resolveSingleFileKlib(KlibFile(libraryFile.path).canonicalFile)

        return KotlinxBenchmarksLibraryResolverSimulation(
            klibs = dependencyFiles.map { it.path }
        ).libraryResolverLegacy().resolveWithDependencies(
            unresolvedLibraries = library.unresolvedDependencies,
            noStdLib = !isForKotlinNative,
            noDefaultLibs = !isForKotlinNative,
            noEndorsedLibs = !isForKotlinNative,
        ).getFullList()
    }
}
