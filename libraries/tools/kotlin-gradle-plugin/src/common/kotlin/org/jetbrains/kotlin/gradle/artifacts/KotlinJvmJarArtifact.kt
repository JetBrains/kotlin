/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal val KotlinJvmJarArtifact = KotlinTargetArtifact { target, apiElements, runtimeElements ->
    if (target !is KotlinJvmTarget) return@KotlinTargetArtifact
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val jvmJarTask = target.createArtifactsTask { jar ->
        jar.from(mainCompilation.output.allOutputs)
    }

    target.createPublishArtifact(jvmJarTask, JAR_TYPE, apiElements, runtimeElements)
}
