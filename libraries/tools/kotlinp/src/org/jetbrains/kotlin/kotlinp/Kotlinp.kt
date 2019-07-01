/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.InconsistentKotlinMetadataException
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.KotlinModuleMetadata
import java.io.File

class Kotlinp(val settings: KotlinpSettings) {
    internal fun renderClassFile(classFile: KotlinClassMetadata?): String =
        when (classFile) {
            is KotlinClassMetadata.Class -> ClassPrinter(settings).print(classFile)
            is KotlinClassMetadata.FileFacade -> FileFacadePrinter(settings).print(classFile)
            is KotlinClassMetadata.SyntheticClass -> {
                if (classFile.isLambda) LambdaPrinter(settings).print(classFile)
                else buildString { appendln("synthetic class") }
            }
            is KotlinClassMetadata.MultiFileClassFacade -> MultiFileClassFacadePrinter().print(classFile)
            is KotlinClassMetadata.MultiFileClassPart -> MultiFileClassPartPrinter(settings).print(classFile)
            is KotlinClassMetadata.Unknown -> buildString { appendln("unknown file (k=${classFile.header.kind})") }
            null -> buildString { appendln("unsupported file") }
        }

    internal fun readClassFile(file: File): KotlinClassMetadata? {
        val header = file.readKotlinClassHeader() ?: throw KotlinpException("file is not a Kotlin class file: $file")
        return try {
            KotlinClassMetadata.read(header)
        } catch (e: InconsistentKotlinMetadataException) {
            throw KotlinpException("inconsistent Kotlin metadata: ${e.message}")
        }
    }

    internal fun renderModuleFile(metadata: KotlinModuleMetadata?): String =
        if (metadata != null) ModuleFilePrinter().print(metadata)
        else buildString { appendln("unsupported file") }

    internal fun readModuleFile(file: File): KotlinModuleMetadata? =
        KotlinModuleMetadata.read(file.readBytes())
}

data class KotlinpSettings(
    val isVerbose: Boolean
)
