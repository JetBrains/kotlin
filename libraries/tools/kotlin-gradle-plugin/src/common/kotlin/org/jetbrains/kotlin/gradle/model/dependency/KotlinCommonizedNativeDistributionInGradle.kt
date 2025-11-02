/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.dependency

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.KotlinCommonizedNativeDistributionModel
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.gradle.model.KotlinMetadataLibrariesInGradle
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedNativeDistributionKlibsOrNull

internal class KotlinCommonizedNativeDistributionInGradle(
    val commonizerTarget: SharedCommonizerTarget,
    private val project: Project,
) : KotlinCommonizedNativeDistributionModel, KotlinMetadataLibrariesInGradle {
    override val files: FileCollection by lazy {
        val filesProvider = project.commonizedNativeDistributionKlibsOrNull(commonizerTarget)
        project.files(filesProvider)
    }
}