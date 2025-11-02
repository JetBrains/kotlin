/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.dependency

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.CommonizedCinteropMetadataLibrariesModel
import org.jetbrains.kotlin.gradle.model.KotlinMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropMetadataDependencyClasspath
import org.jetbrains.kotlin.gradle.utils.future

internal class CommonizedCinteropMetadataLibrariesInGradle(
    private val sourceSet: DefaultKotlinSourceSet
) : CommonizedCinteropMetadataLibrariesModel, KotlinMetadataLibrariesInGradle {
    override val files: FileCollection by lazy {
        val project = sourceSet.project
        project.future { project.createCInteropMetadataDependencyClasspath(sourceSet, false) }.getOrThrow()
    }
}