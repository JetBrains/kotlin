/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.library.AbstractNativeKlibWriterTest
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.native.interop.gen.CInteropKlibWritingTest.CInteropParameters
import org.jetbrains.kotlin.native.interop.gen.jvm.createInteropLibrary
import org.jetbrains.kotlin.util.toCInteropKlibMetadataVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.abort
import java.io.File
import java.util.Properties
import kotlin.collections.forEach

class CInteropKlibWritingTest : AbstractNativeKlibWriterTest<CInteropParameters>(::CInteropParameters) {
    class CInteropParameters : NativeParameters() {
        var abiLevel: KlibAbiCompatibilityLevel = KlibAbiCompatibilityLevel.LATEST_STABLE

        override var compilerVersion: String? = KotlinCompilerVersion.VERSION
            set(_) = abort<Nothing>("createInteropLibrary() only supports the single compiler version")

        override var metadataVersion: MetadataVersion?
            get() = abiLevel.toCInteropKlibMetadataVersion()
            set(_) = abort<Nothing>("The metadata version can only be deduced from the ABI compatibility level")

        override var abiVersion: KotlinAbiVersion?
            get() = abiLevel.toAbiVersionForManifest()
            set(_) = abort<Nothing>("The ABI version can only be deduced from the ABI compatibility level")

        override var ir: Collection<SerializedIrFile>?
            get() = null
            set(_) = abort<Nothing>("createInteropLibrary() does not support serialization of IR")

        override var irOfInlinableFunctions: SerializedIrFile?
            get() = null
            set(_) = abort<Nothing>("createInteropLibrary() does not support serialization of IR")
    }

    @Test
    fun `Writing C-interop klib with different ABI levels`() {
        KlibAbiCompatibilityLevel.entries.forEach { abiLevel ->
            runTestWithParameters {
                this.abiLevel = abiLevel
            }
        }
    }

    override fun writeKlib(parameters: CInteropParameters): File {
        val klibDir = createNewKlibDir()
        val klibLocation = if (parameters.nopack) klibDir else klibDir.resolveSibling(klibDir.nameWithoutExtension + ".klib")

        createInteropLibrary(
                serializedMetadata = parameters.metadata,
                outputPath = klibLocation.path,
                moduleName = parameters.uniqueName,
                nativeBitcodeFiles = parameters.bitCodeFiles.map { it.file.path },
                target = parameters.target,
                manifest = Properties().apply {
                    parameters.customManifestProperties.forEach { (key, value) -> setProperty(key, value) }
                },
                dependencies = KlibLoader { libraryPaths(parameters.dependencies.map { it.path }) }.load().librariesStdlibFirst,
                nopack = parameters.nopack,
                shortName = parameters.shortName,
                staticLibraries = parameters.nativeIncludedBinaryFiles.map { it.file.path },
                klibAbiCompatibilityLevel = parameters.abiLevel,
        )

        return klibLocation
    }
}
