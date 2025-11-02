/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.KotlinSourcesContainerModel
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class KotlinSourcesContainerInGradle(
    val sourceSet: KotlinSourceSet
) : KotlinSourcesContainerModel {
    val files: FileCollection = sourceSet.allKotlinSources
}