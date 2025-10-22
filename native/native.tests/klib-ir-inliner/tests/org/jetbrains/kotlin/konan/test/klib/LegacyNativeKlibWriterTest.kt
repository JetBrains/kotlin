/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.library.AbstractNativeKlibWriterTest
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.klib.LegacyNativeKlibWriterTest.LegacyNativeKlibWriterParameters
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

class LegacyNativeKlibWriterTest : AbstractNativeKlibWriterTest<LegacyNativeKlibWriterParameters>(::LegacyNativeKlibWriterParameters) {
    class LegacyNativeKlibWriterParameters : NativeParameters() {
        var targetsForManifest: List<KonanTarget>? = null
    }

    @Test
    fun `Writing a klib with different 'targets for manifest'`() {
        listOf(
            listOf(KonanTarget.IOS_ARM64, KonanTarget.MACOS_ARM64, KonanTarget.WATCHOS_ARM64),
            listOf(KonanTarget.LINUX_ARM64, KonanTarget.MACOS_X64),
            listOf(KonanTarget.ANDROID_ARM64),
        ).forEach { targets ->
            runTestWithParameters {
                this.targetsForManifest = targets
            }
        }
    }

    context(properties: Properties)
    override fun customizeManifestForMockKlib(parameters: LegacyNativeKlibWriterParameters) {
        super.customizeManifestForMockKlib(parameters)
        properties[KLIB_PROPERTY_NATIVE_TARGETS] = parameters.targetsForManifest?.joinToString(" ") { it.visibleName }
            ?: parameters.target.visibleName
    }

    override fun writeKlib(parameters: LegacyNativeKlibWriterParameters): File {
        val klibDir = createNewKlibDir()
        val klibLocation = if (parameters.nopack) klibDir else klibDir.resolveSibling(klibDir.nameWithoutExtension + ".klib")

        buildLibrary(
            natives = parameters.bitCodeFiles.map { it.file.path },
            included = parameters.includedFiles.map { it.file.path },
            linkDependencies = KlibLoader { libraryPaths(parameters.dependencies.map { it.path }) }.load().librariesStdlibFirst,
            metadata = parameters.metadata,
            ir = if (parameters.ir != null || parameters.irOfInlinableFunctions != null) {
                SerializedIrModule(parameters.ir.orEmpty(), parameters.irOfInlinableFunctions)
            } else null,
            versions = KotlinLibraryVersioning(
                compilerVersion = parameters.compilerVersion,
                metadataVersion = parameters.metadataVersion,
                abiVersion = parameters.abiVersion,
            ),
            target = parameters.target,
            output = klibLocation.path,
            moduleName = parameters.uniqueName,
            nopack = parameters.nopack,
            shortName = parameters.shortName,
            manifestProperties = Properties().apply {
                parameters.customManifestProperties.forEach { (key, value) -> setProperty(key, value) }
            },
            nativeTargetsForManifest = parameters.targetsForManifest?.map { it.visibleName }
                ?: listOf(parameters.target.visibleName)
        )

        return klibLocation
    }
}
