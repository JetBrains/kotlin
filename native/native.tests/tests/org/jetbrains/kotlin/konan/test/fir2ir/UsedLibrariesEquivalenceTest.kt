/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.fir2ir

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderOptimized
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UsedLibrariesEquivalenceTest {

    @Test
    fun `test usedLibraries calculation equivalence`() {
        val usedPackages = setOf(
            FqName("com.example.used"),
            FqName("com.example.mixed")
        )

        val lib1 = createMockLibrary("lib1", listOf("com.example.used"))
        val lib2 = createMockLibrary("lib2", listOf("com.example.generated", "com.example.mixed"))
        val lib3 = createMockLibrary("lib3", listOf("com.example.unused"))
        val lib4 = createMockLibrary("lib4", emptyList())

        val resolvedLibraries = listOf(lib1, lib2, lib3, lib4).map { createMockResolvedLibrary(it) }

        val librariesDescriptors = resolvedLibraries.map { it.moduleDescriptor as ModuleDescriptorImpl }

        val usedLibraries1 = librariesDescriptors.zip(resolvedLibraries).filter { (module, _) ->
            // Use extension function isEmpty from org.jetbrains.kotlin.descriptors
            usedPackages.any { !module.packageFragmentProviderForModuleContentWithoutDependencies.isEmpty(it) }
        }.map { it.second }.toSet()

        val usedLibraries2 = resolvedLibraries.filter { resolvedLibrary ->
            val header = parseModuleHeader(
                resolvedLibrary.library.getComponent(KlibMetadataComponent.Kind)?.moduleHeaderData!!
            )
            val nonEmptyPackageNames = buildSet {
                addAll(header.packageFragmentNameList)
                removeAll(header.emptyPackageList)
            }
            usedPackages.any { it.asString() in nonEmptyPackageNames }
        }.toSet()

        assertEquals(usedLibraries1, usedLibraries2)
        assertEquals(setOf(resolvedLibraries[0], resolvedLibraries[1]), usedLibraries1)
    }

    private fun createMockLibrary(name: String, packages: List<String>): MockKotlinLibrary {
        val header = KlibMetadataProtoBuf.Header.newBuilder()
            .setModuleName(name)
            .addAllPackageFragmentName(packages)
            .build()
        return MockKotlinLibrary(name, header.toByteArray())
    }

    private fun createMockResolvedLibrary(library: MockKotlinLibrary): MockKotlinResolvedLibrary {
        return MockKotlinResolvedLibrary(library)
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
        override val attributes: org.jetbrains.kotlin.library.KlibAttributes get() = error("Not supported")
        
        override val versions: KotlinLibraryVersioning get() = error("Not supported")
        override val libraryFile: File get() = File(".")
        
        override val manifestProperties: Properties = Properties().apply {
            setProperty(KLIB_PROPERTY_UNIQUE_NAME, libName)
        }
    }

    private class MockKotlinResolvedLibrary(
        override val library: MockKotlinLibrary
    ) : KotlinResolvedLibrary {
        override val resolvedDependencies: List<KotlinResolvedLibrary> get() = emptyList()
        override val isNeededForLink: Boolean get() = true
        override val isDefault: Boolean get() = false
        override fun markNeededForLink(packageFqName: String) {}

        val moduleDescriptor: ModuleDescriptor = ModuleDescriptorImpl(
            Name.special("<${library.libName}>"),
            LockBasedStorageManager("test"),
            DefaultBuiltIns.Instance,
            null
        ).apply {
            initialize(object : PackageFragmentProviderOptimized {
                @Deprecated("Mock implementation")
                override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> = emptyList()
                override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptyList()
                override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {}
                
                override fun isEmpty(fqName: FqName): Boolean {
                    val header = parseModuleHeader(
                        library.getComponent(KlibMetadataComponent.Kind)?.moduleHeaderData!!
                    )
                    return fqName.asString() !in header.packageFragmentNameList
                }
            })
        }
    }
}
