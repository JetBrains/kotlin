/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.utils.getFile

internal interface EmptySwiftPMDefFileWorkParameters : WorkParameters {
    val architectures: SetProperty<AppleArchitecture>
    val defFilesOutputDir: DirectoryProperty
    val ldDumpOutputDir: DirectoryProperty
    val cinteropNamespace: Property<String>
}

internal abstract class EmptySwiftPMDefFileWorkAction : WorkAction<EmptySwiftPMDefFileWorkParameters> {
    override fun execute() {
        val architectures = parameters.architectures.get()
        val cinteropNamespace = parameters.cinteropNamespace.get()
        val defFilesDir = parameters.defFilesOutputDir.getFile()
        val ldDumpDir = parameters.ldDumpOutputDir.getFile()

        architectures.forEach { architecture ->
            defFilesDir.resolve(XcodebuildDefFileUtils.defFileName(architecture)).writeText(
                """
                    language = Objective-C
                    package = $cinteropNamespace
                """.trimIndent()
            )
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkLdFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.ldFingerprintFileName(architecture)).writeText("0")
            ldDumpDir.resolve(XcodebuildDefFileUtils.frameworkSearchpathFileName(architecture)).writeText("\n")
            ldDumpDir.resolve(XcodebuildDefFileUtils.librarySearchpathFileName(architecture)).writeText("\n")
        }
    }
}
