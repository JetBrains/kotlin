/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.base64Hash
import org.jetbrains.kotlin.commonizer.CommonizerOutputFileLayout.ensureMaxFileNameLength
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.utils.changing
import org.jetbrains.kotlin.gradle.utils.kotlinMetadataDir
import java.io.File

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
internal abstract class AbstractCInteropCommonizerTask : DefaultTask(), UsesBuildMetricsService {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}

internal const val CINTEROP_COMMONIZER_OUTPUT_PATH = "classes/kotlin/commonizer"

internal val Project.cInteropCommonizerOutputRoot: File
    get() = layout.buildDirectory.get().asFile.resolve(CINTEROP_COMMONIZER_OUTPUT_PATH)

internal val Project.copyCInteropCommonizerForIdeOutputRoot: File
    get() = kotlinMetadataDir()
        .resolve("commonizer")
        .resolve(path.removePrefix(":").replace(":", "/"))

internal fun AbstractCInteropCommonizerTask.outputDirectory(group: CInteropCommonizerGroup): File =
    outputDirectory.get().asFile.commonizerGroupDirectory(group)

private fun File.commonizerGroupDirectory(group: CInteropCommonizerGroup): File {
    val interopsDirectoryName = group.interops.map { it.interopName }.toSet().joinToString("_")
    val groupDisambiguation = group.targets.joinToString { it.identityString } +
            group.interops.joinToString { it.uniqueName }

    return resolve(ensureMaxFileNameLength(interopsDirectoryName))
        .resolve(base64Hash(groupDisambiguation))
}

internal suspend fun Project.commonizedOutputLibraries(dependent: CInteropCommonizerDependent): FileCollection? {
    val commonizerTask = commonizeCInteropTask() ?: return null
    return commonizedOutputLibraries(commonizerTask, cInteropCommonizerOutputRoot, dependent)
}

internal suspend fun Project.commonizedOutputLibrariesForIde(dependent: CInteropCommonizerDependent): FileCollection? {
    val commonizerTask = copyCommonizeCInteropForIdeTask() ?: return null
    return commonizedOutputLibraries(commonizerTask, copyCInteropCommonizerForIdeOutputRoot, dependent)
}

private suspend fun Project.commonizedOutputLibraries(
    commonizerTask: TaskProvider<out AbstractCInteropCommonizerTask>,
    outputRoot: File,
    dependent: CInteropCommonizerDependent,
): FileCollection {
    val commonizedOutputLibraries = objects.fileCollection().builtBy(commonizerTask)
    val outputDirectory = commonizedOutputDirectory(outputRoot, dependent) ?: return commonizedOutputLibraries
    return commonizedOutputLibraries.from(
        providers.changing { outputDirectory.listFiles().orEmpty().toSet() }
    )
}

internal suspend fun Project.commonizedOutputDirectory(dependent: CInteropCommonizerDependent): File? {
    if (commonizeCInteropTask() == null) return null
    return commonizedOutputDirectory(cInteropCommonizerOutputRoot, dependent)
}

private suspend fun Project.commonizedOutputDirectory(outputRoot: File, dependent: CInteropCommonizerDependent): File? {
    val group = findCInteropCommonizerGroup(dependent) ?: return null
    return CommonizerOutputFileLayout
        .resolveCommonizedDirectory(outputRoot.commonizerGroupDirectory(group), dependent.target)
}
