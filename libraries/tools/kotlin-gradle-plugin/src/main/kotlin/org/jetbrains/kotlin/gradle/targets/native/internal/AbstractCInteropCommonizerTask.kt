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
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal abstract class AbstractCInteropCommonizerTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: File

    internal fun outputDirectory(group: CInteropCommonizerGroup): File {
        val interopsDirectoryName = group.interops.map { it.interopName }.toSet().joinToString(";")
        val groupDisambiguation = group.targets.joinToString { it.identityString } +
                group.interops.joinToString { it.uniqueName }

        return outputDirectory
            .resolve(ensureMaxFileNameLength(interopsDirectoryName))
            .resolve(base64Hash(groupDisambiguation))
    }

    internal abstract fun findInteropsGroup(dependent: CInteropCommonizerDependent): CInteropCommonizerGroup?

    internal fun commonizedOutputLibraries(dependent: CInteropCommonizerDependent): FileCollection {
        val fileProvider = project.filesProvider {
            val group = findInteropsGroup(dependent) ?: return@filesProvider emptySet<File>()
            CommonizerOutputFileLayout
                .resolveCommonizedDirectory(outputDirectory(group), dependent.target)
                .listFiles().orEmpty().toSet()
        }

        return fileProvider.builtBy(this)
    }
}
