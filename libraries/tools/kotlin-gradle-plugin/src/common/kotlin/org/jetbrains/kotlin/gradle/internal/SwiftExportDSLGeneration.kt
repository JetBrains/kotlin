/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.jetbrains.kotlin.backend.common.serialization.IrKlibBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.deserializeFqName
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.cli.klib.KlibToolOutput
import org.jetbrains.kotlin.cli.klib.KlibToolLogger
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME

fun dumpKlibs(
    klibs: List<String>
): List<SwiftExportDSLModule> {
    val swiftExportDSLModules = klibs.mapNotNull {
        try {
            return@mapNotNull dumpPackageFQNsFromLibrary(it)
        } catch (e: Exception) {
            return@mapNotNull null
        }
    }

    return swiftExportDSLModules
}

class SwiftExportDSLModule(
    val libraryName: String,
    val packages: Set<String>,
)

fun dumpPackageFQNsFromLibrary(path: String): SwiftExportDSLModule {
    val output = KlibToolOutput(stdout = System.out, stderr = System.err)
    val library = resolverByName(emptyList(), logger = KlibToolLogger(output)).resolve(path)
    val packageFqns = mutableSetOf<String>()

    for (fileIndex in 0 until library.fileCount()) {
        val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))
        val fileProto = IrFile.parseFrom(
            library.file(fileIndex).codedInputStream,
            IrLibraryFileFromBytes.extensionRegistryLite,
        )
        val packageFQN = fileReader.deserializeFqName(fileProto.fqNameList)
        if (packageFQN.isEmpty()) {
            continue
        }
        println("$fileIndex: $packageFQN")
        packageFqns.add(packageFQN)
    }

    val moduleName = library.manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

    return SwiftExportDSLModule(
        moduleName,
        packageFqns,
    )
}