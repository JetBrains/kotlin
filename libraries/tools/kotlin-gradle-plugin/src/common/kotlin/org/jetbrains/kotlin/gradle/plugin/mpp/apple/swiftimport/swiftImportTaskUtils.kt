/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.decodeFromString
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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
        fingerprintFile.get().asFile.readText().let {
            fingerprintJson.decodeFromString<SwiftImportFingerprint>(it)
        }

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
