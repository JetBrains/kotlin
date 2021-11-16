/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.external.KotlinExternalTargetHandle
import org.jetbrains.kotlin.gradle.targets.external.addIdeImplementationDependency

internal fun KotlinExternalTargetHandle.createCommonAndroidSourceSet(name: String): KotlinSourceSet {
    return project.kotlinExtension.sourceSets.maybeCreate(name).apply {
        setupAndroidArtifactTypeForIde(project)
        addIdeImplementationDependency(project, project.getAndroidRuntimeJars())
    }
}
