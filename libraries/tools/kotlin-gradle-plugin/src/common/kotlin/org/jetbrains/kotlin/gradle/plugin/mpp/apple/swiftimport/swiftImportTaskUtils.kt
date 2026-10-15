/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.SerializationException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

internal abstract class SwiftImportFingerprintInput {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fingerprintFile: RegularFileProperty

    fun readFingerprint(): SwiftImportFingerprint =
        fingerprintFile.get().asFile.readSwiftImportFingerprint()

    fun readFingerprintOrNull(): SwiftImportFingerprint? =
        fingerprintFile.asFile.orNull
            ?.takeIf(File::isFile)
            ?.readSwiftImportFingerprint()

}

internal fun File.readSwiftImportFingerprint(): SwiftImportFingerprint {
    val content = readText()

    return try {
        return fingerprintJson.decodeFromString<SwiftImportFingerprint>(content)
    } catch (_: SerializationException) {
        readLegacySwiftImportFingerprint(content)
    }
}

private fun readLegacySwiftImportFingerprint(
    content: String,
): SwiftImportFingerprint {

    val lines = content
        .lineSequence()
        .filter(String::isNotBlank)
        .toList()

    require(lines.size >= 2) {
        "Invalid legacy Swift import fingerprint format"
    }

    return SwiftImportFingerprint(
        taskInvalidationFingerprint = lines[0],
        incrementalFingerprint = lines[1],
    )
}

internal abstract class LocalPackageTrackingInputs {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val filesToTrackFromLocalPackages: RegularFileProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val nonEmptyFilesFromLocalPackages: Provider<List<File>>
        get() = filesToTrackFromLocalPackages.map { inputFile ->
            inputFile.asFile
                .readLines()
                .filter { it.isNotBlank() }
                .map(::File)
        }
}
