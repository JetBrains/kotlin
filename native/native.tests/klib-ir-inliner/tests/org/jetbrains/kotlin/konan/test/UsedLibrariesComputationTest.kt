/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.klib.compileToKlibsViaCli
import org.jetbrains.kotlin.konan.test.klib.newSourceModules
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.konan.file.File as KlibFile

class UsedLibrariesComputationTest : AbstractNativeSimpleTest() {
    @Test
    fun `Used libraries are computed correctly (CLI)`() {
        var additionalUnusedModulePath: String? = null
        newSourceModules { addRegularModule("additional") }.compileToKlibsViaCli { _, successKlib ->
            additionalUnusedModulePath = successKlib.resultingArtifact.klibFile.absolutePath
        }
        checkNotNull(additionalUnusedModulePath)

        fun TestCompilationResult.Success<out KLIB>.assertDependencyNames(vararg expectedDependencyNamesInManifest: String) {
            val expectedDependencyNames = expectedDependencyNamesInManifest.toSet()
            val actualDependencyNames = resultingArtifact.klibFile.resolve("default").resolve("manifest").bufferedReader().use { reader ->
                Properties().apply { load(reader) }
            }.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).toSet()

            assertEquals(expectedDependencyNames, actualDependencyNames) {
                "Dependency mismatch for module ${resultingArtifact.klibFile}: expected $expectedDependencyNames, got $actualDependencyNames"
            }
        }

        newSourceModules {
            addRegularModule("test")
        }.compileToKlibsViaCli { _, successKlib ->
            successKlib.assertDependencyNames(
                /* implicit unavoidable dependency */ "stdlib",
            )
        }

        // TODO (KT-60874): Need to clarify why used-specified libraries are forced to stay in the list of "used libraries"
        //  even when they are not used. Note that the default value for `konanPurgeUserLibs` is always `false`.
        newSourceModules {
            addRegularModule("test")
        }.compileToKlibsViaCli(extraCliArgs = listOf("-l", additionalUnusedModulePath)) { _, successKlib ->
            successKlib.assertDependencyNames(
                /* implicit unavoidable dependency */ "stdlib",
                /* because it was passed explicitly via CLI, even if "test" doesn't use any API from "additional" */ "additional",
            )
        }

        // TODO (KT-60874): Need to clarify why used-specified libraries are forced to stay in the list of "used libraries"
        //  even when they are not used. Note that the default value for `konanPurgeUserLibs` is always `false`.
        newSourceModules {
            addRegularModule("test")
        }.compileToKlibsViaCli(extraCliArgs = listOf("-l", additionalUnusedModulePath, "-Xpurge-user-libs")) { _, successKlib ->
            successKlib.assertDependencyNames(
                /* implicit unavoidable dependency */ "stdlib",
                /* because it was passed explicitly via CLI, even if "test" doesn't use any API from "additional" */ "additional",
            )
        }

        newSourceModules {
            addRegularModule("test") {
                sourceFileAddend("fun callAdditional() = additional.additional(-1)")
            }
        }.compileToKlibsViaCli(extraCliArgs = listOf("-l", additionalUnusedModulePath)) { _, successKlib ->
            successKlib.assertDependencyNames(
                /* implicit unavoidable dependency */ "stdlib",
                /* because it was passed explicitly via CLI and "test" uses some API from "additional" */ "additional",
            )
        }

        newSourceModules {
            addRegularModule("test") {
                sourceFileAddend("@kotlinx.cinterop.ExperimentalForeignApi fun callPosix() = platform.posix.fopen(\"test.txt\", \"r\")")
            }
        }.compileToKlibsViaCli { _, successKlib ->
            successKlib.assertDependencyNames(
                /* implicit unavoidable dependency */ "stdlib",
                /* because "test" uses some API from posix */ "org.jetbrains.kotlin.native.platform.posix",
            )
        }
    }

    @Test
    fun `Used libraries are computed correctly (low-level)`() {
        val usedPackages = setOf(
            FqName("com.example.used"),
            FqName("com.example.mixed")
        )

        val lib1 = createMockLibrary("lib1", listOf("com.example.used"))
        val lib2 = createMockLibrary("lib2", listOf("com.example.generated", "com.example.mixed"))
        val lib3 = createMockLibrary("lib3", listOf("com.example.unused"))
        val lib4 = createMockLibrary("lib4", emptyList())

        val allLibraries = listOf(lib1, lib2, lib3, lib4)

        // Compute used libraries using metadata proto header.
        val usedLibraries: Set<MockKotlinLibrary> = allLibraries.filter { library ->
            val header = parseModuleHeader(library.metadata.moduleHeaderData)
            val nonEmptyPackageNames = buildSet {
                addAll(header.packageFragmentNameList)
                removeAll(header.emptyPackageList)
            }
            usedPackages.any { it.asString() in nonEmptyPackageNames }
        }.toSet()

        assertEquals(setOf(lib1, lib2), usedLibraries)
    }

    private fun createMockLibrary(name: String, packages: List<String>): MockKotlinLibrary {
        val header = KlibMetadataProtoBuf.Header.newBuilder()
            .setModuleName(name)
            .addAllPackageFragmentName(packages)
            .build()
        return MockKotlinLibrary(name, header.toByteArray())
    }

    private class MockKotlinLibrary(
        val libName: String,
        val headerBytes: ByteArray
    ) : KotlinLibrary {
        override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>): KC? {
            if (kind == KlibMetadataComponent.Kind) {
                @Suppress("UNCHECKED_CAST")
                return object : KlibMetadataComponent {
                    override val moduleHeaderData: ByteArray get() = headerBytes
                    override fun getPackageFragmentNames(packageFqName: String): Set<String> = emptySet()
                    override fun getPackageFragment(packageFqName: String, fragmentName: String): ByteArray = ByteArray(0)
                } as KC
            }
            return null
        }

        override val location: KlibFile get() = KlibFile(".")
        override val attributes: KlibAttributes get() = error("Not supported")

        override val versions: KotlinLibraryVersioning get() = error("Not supported")
        override val libraryFile: KlibFile get() = KlibFile(".")

        override val manifestProperties: Properties = Properties().apply {
            setProperty(KLIB_PROPERTY_UNIQUE_NAME, libName)
        }
    }
}