/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerCompositeMetadataJarBundling.cinteropMetadataDirectoryPath

internal fun Project.includeCommonizedCInteropMetadata(
    metadataKlib: TaskProvider<out Zip>, compilation: KotlinSharedNativeCompilation
) {
    metadataKlib.configure { jar ->
        launch { includeCommonizedCInteropMetadata(jar, compilation) }
    }
}

internal suspend fun Project.includeCommonizedCInteropMetadata(metadataKlib: Zip, compilation: KotlinSharedNativeCompilation) {
    val commonizerTask = commonizeCInteropTask()?.get() ?: return
    val commonizerDependencyToken = CInteropCommonizerDependent.from(compilation) ?: return
    val outputDirectory = commonizerTask.commonizedOutputDirectory(commonizerDependencyToken) ?: return

    metadataKlib.from(outputDirectory) { spec ->
        spec.into(cinteropMetadataDirectoryPath(compilation.defaultSourceSet.name))
    }
}

internal object CInteropCommonizerCompositeMetadataJarBundling {
    fun cinteropMetadataDirectoryPath(sourceSetName: String): String {
        return "$sourceSetName-cinterop"
    }
}
