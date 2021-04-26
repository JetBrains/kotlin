/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.commonizer.HierarchicalCommonizerOutputLayout
import org.jetbrains.kotlin.commonizer.prettyName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal abstract class AbstractCInteropCommonizerTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: File

    internal fun outputDirectory(parameters: CInteropCommonizationParameters): File {
        return outputDirectory
            .resolve(parameters.commonizerTarget.prettyName)
            .resolve(parameters.interops.map { it.interopName }.distinct().joinToString("-"))
    }

    internal abstract fun getCommonizationParameters(compilation: KotlinSharedNativeCompilation): CInteropCommonizationParameters?

    internal fun getLibraries(compilation: KotlinSharedNativeCompilation): FileCollection {
        val compilationCommonizerTarget = project.getCommonizerTarget(compilation) ?: return project.files()
        val fileProvider = project.filesProvider {
            val parameters = getCommonizationParameters(compilation) ?: return@filesProvider emptySet<File>()
            HierarchicalCommonizerOutputLayout
                .getTargetDirectory(outputDirectory(parameters), compilationCommonizerTarget)
                .listFiles().orEmpty().toSet()
        }

        return fileProvider.builtBy(this)
    }
}

internal fun TaskProvider<out AbstractCInteropCommonizerTask>.getLibraries(compilation: KotlinSharedNativeCompilation): FileCollection {
    return compilation.target.project.files(map { it.getLibraries(compilation) })
}
