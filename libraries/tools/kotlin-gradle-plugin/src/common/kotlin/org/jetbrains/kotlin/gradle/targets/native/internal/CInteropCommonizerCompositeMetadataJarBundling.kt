/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerCompositeMetadataJarBundling.cinteropMetadataDirectoryPath
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun Project.includeCommonizedCInteropMetadata(
    metadataKlib: TaskProvider<out Zip>, compilation: KotlinSharedNativeCompilation
) {
    metadataKlib.configure { jar ->
        launch { jar.includeCommonizedCInteropMetadata(compilation) }
    }
}

internal suspend fun KotlinSharedNativeCompilation.commonizedCInteropOutput(): FileCollection? {
    val commonizerTask = project.commonizeCInteropTask() ?: return null
    val commonizerDependencyToken = CInteropCommonizerDependent.from(this) ?: return null
    val outputDirectory = commonizerTask.get().commonizedOutputDirectory(commonizerDependencyToken) ?: return null
    return project.filesProvider(commonizerTask) { outputDirectory }

}

internal suspend fun Zip.includeCommonizedCInteropMetadata(compilation: KotlinSharedNativeCompilation) {
    val outputDirectory = compilation.commonizedCInteropOutput() ?: return

    from(outputDirectory) { spec ->
        spec.into(compilation.cinteropMetadataDirectoryPath())
    }
}

internal fun KotlinSharedNativeCompilation.cinteropMetadataDirectoryPath(): String {
    return cinteropMetadataDirectoryPath(defaultSourceSet.name)
}

internal object CInteropCommonizerCompositeMetadataJarBundling {
    fun cinteropMetadataDirectoryPath(sourceSetName: String): String {
        return "$sourceSetName-cinterop"
    }
}
