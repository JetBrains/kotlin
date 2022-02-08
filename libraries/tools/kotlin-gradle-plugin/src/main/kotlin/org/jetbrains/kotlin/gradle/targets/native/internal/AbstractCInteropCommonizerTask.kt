/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.base64Hash
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.ensureMaxFileNameLength
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.utils.outputFilesProvider
import java.io.File

internal abstract class AbstractCInteropCommonizerTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: File

    internal abstract fun findInteropsGroup(dependent: CInteropCommonizerDependent): CInteropCommonizerGroup?
}

internal fun AbstractCInteropCommonizerTask.outputDirectory(group: CInteropCommonizerGroup): File {
    val interopsDirectoryName = group.interops.map { it.interopName }.toSet().joinToString(";")
    val groupDisambiguation = group.targets.joinToString { it.identityString } +
            group.interops.joinToString { it.uniqueName }

    return outputDirectory
        .resolve(ensureMaxFileNameLength(interopsDirectoryName))
        .resolve(base64Hash(groupDisambiguation))
}

internal fun AbstractCInteropCommonizerTask.commonizedOutputLibraries(dependent: CInteropCommonizerDependent): FileCollection {
    return outputFilesProvider {
        (commonizedOutputDirectory(dependent) ?: return@outputFilesProvider emptySet<File>())
            .listFiles().orEmpty().toSet()
    }
}

internal fun AbstractCInteropCommonizerTask.commonizedOutputDirectory(dependent: CInteropCommonizerDependent): File? {
    val group = findInteropsGroup(dependent) ?: return null
    return CommonizerOutputFileLayout
        .resolveCommonizedDirectory(outputDirectory(group), dependent.target)
}
