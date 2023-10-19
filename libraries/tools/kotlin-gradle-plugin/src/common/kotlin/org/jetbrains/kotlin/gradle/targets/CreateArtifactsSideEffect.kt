/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.artifacts.KotlinTargetArtifact
import org.jetbrains.kotlin.gradle.plugin.launch


/**
 * Creates target level artifacts (and exposes them in the 'apiElements' and 'runtimeElements' configurations)
 * The corresponding task for jvm would be called 'jvmJar'
 */
internal val CreateArtifactsSideEffect = KotlinTargetSideEffect { target ->
    val apiElements = target.project.configurations.getByName(target.apiElementsConfigurationName)
    val runtimeElements = target.project.configurations.findByName(target.runtimeElementsConfigurationName)
    KotlinTargetArtifact.extensionPoint[target.project].forEach { artifact ->
        target.project.launch {
            artifact.createArtifact(target, apiElements, runtimeElements)
        }
    }
}
