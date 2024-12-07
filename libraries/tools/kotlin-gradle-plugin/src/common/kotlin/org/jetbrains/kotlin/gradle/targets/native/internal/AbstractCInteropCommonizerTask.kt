/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectory
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.base64Hash
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.ensureMaxFileNameLength
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.utils.changing
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.outputFilesProvider
import java.io.File

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
internal abstract class AbstractCInteropCommonizerTask : DefaultTask(), UsesBuildMetricsService {
    @get:OutputDirectory
    abstract val outputDirectory: File
}

internal fun AbstractCInteropCommonizerTask.outputDirectory(group: CInteropCommonizerGroup): File {
    val interopsDirectoryName = group.interops.map { it.interopName }.toSet().joinToString("_")
    val groupDisambiguation = group.targets.joinToString { it.identityString } +
            group.interops.joinToString { it.uniqueName }

    return outputDirectory
        .resolve(ensureMaxFileNameLength(interopsDirectoryName))
        .resolve(base64Hash(groupDisambiguation))
}

internal fun AbstractCInteropCommonizerTask.commonizedOutputLibraries(dependent: CInteropCommonizerDependent): FileCollection {
    return outputFilesProvider {
        val outputDirectory = project.future { commonizedOutputDirectory(dependent) }.getOrThrow()
            ?: return@outputFilesProvider emptySet<File>()
        project.providers.changing {
            outputDirectory.listFiles().orEmpty().toSet()
        }
    }
}

internal suspend fun AbstractCInteropCommonizerTask.commonizedOutputDirectory(dependent: CInteropCommonizerDependent): File? {
    val group = project.findCInteropCommonizerGroup(dependent) ?: return null
    return CommonizerOutputFileLayout
        .resolveCommonizedDirectory(outputDirectory(group), dependent.target)
}
