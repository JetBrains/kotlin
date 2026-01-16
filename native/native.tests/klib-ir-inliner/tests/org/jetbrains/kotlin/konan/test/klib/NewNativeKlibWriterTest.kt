/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.library.AbstractNativeKlibWriterTest
import org.jetbrains.kotlin.konan.library.writer.includeBitcode
import org.jetbrains.kotlin.konan.library.writer.includeNativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.writer.legacyNativeDependenciesInManifest
import org.jetbrains.kotlin.konan.library.writer.legacyNativeShortNameInManifest
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.klib.NewNativeKlibWriterTest.NewNativeKlibWriterParameters
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.asComponentWriters
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

class NewNativeKlibWriterTest : AbstractNativeKlibWriterTest<NewNativeKlibWriterParameters>(::NewNativeKlibWriterParameters) {
    class NewNativeKlibWriterParameters : NativeParameters() {
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
    override fun customizeManifestForMockKlib(parameters: NewNativeKlibWriterParameters) {
        super.customizeManifestForMockKlib(parameters)
        properties[KLIB_PROPERTY_NATIVE_TARGETS] = parameters.targetsForManifest?.joinToString(" ") { it.visibleName }
            ?: parameters.target.visibleName
    }

    override fun writeKlib(parameters: NewNativeKlibWriterParameters): File {
        val klibDir = createNewKlibDir()
        val klibLocation = if (parameters.nopack) klibDir else klibDir.resolveSibling(klibDir.nameWithoutExtension + ".klib")

        KlibWriter {
            format(if (parameters.nopack) KlibFormat.Directory else KlibFormat.ZipArchive)
            manifest {
                moduleName(parameters.uniqueName)
                versions(
                    KotlinLibraryVersioning(
                        compilerVersion = parameters.compilerVersion,
                        metadataVersion = parameters.metadataVersion,
                        abiVersion = parameters.abiVersion,
                    )
                )
                platformAndTargets(
                    builtInsPlatform = BuiltInsPlatform.NATIVE,
                    targetNames = parameters.targetsForManifest?.map { it.name } ?: listOf(parameters.target.name)
                )
                legacyNativeDependenciesInManifest(parameters.dependencies.map { it.uniqueName })
                legacyNativeShortNameInManifest(parameters.shortName)
                customProperties {
                    parameters.customManifestProperties.forEach { (key, value) -> setProperty(key, value) }
                }
            }
            includeMetadata(parameters.metadata)
            include(parameters.ir?.asComponentWriters().orEmpty())
            includeBitcode(parameters.target, parameters.bitcodeFiles.map { it.file.path })
            includeNativeIncludedBinaries(parameters.target, parameters.nativeIncludedBinaryFiles.map { it.file.path })
        }.writeTo(klibLocation.path)

        return klibLocation
    }
}
