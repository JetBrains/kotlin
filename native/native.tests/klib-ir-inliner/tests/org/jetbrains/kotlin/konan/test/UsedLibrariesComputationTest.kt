/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UsedLibrariesComputationTest {

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

        override val location: File get() = File(".")
        override val attributes: KlibAttributes get() = error("Not supported")

        override val versions: KotlinLibraryVersioning get() = error("Not supported")
        override val libraryFile: File get() = File(".")

        override val manifestProperties: Properties = Properties().apply {
            setProperty(KLIB_PROPERTY_UNIQUE_NAME, libName)
        }
    }
}